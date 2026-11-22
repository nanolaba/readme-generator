package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSource(String name, String body) throws IOException {
        Path src = tempDir.resolve(name);
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    private List<Validator.Diagnostic> validate(Path src) {
        return new Validator(src.toFile()).validate();
    }

    @Test
    public void testCleanTemplatePassesWithoutDiagnostics() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "Hello<!--en-->\n" +
                        "Привет<!--ru-->\n");

        assertTrue(validate(src).isEmpty(), "expected no diagnostics");
    }

    @Test
    public void testUnregisteredWidgetFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "Body line\n" +
                        "${widget:doesNotExist(x='1')}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        Validator.Diagnostic d = diags.get(0);
        assertTrue(d.isError());
        assertEquals(3, d.getLine());
        assertTrue(d.getMessage().contains("doesNotExist"), d.getMessage());
    }

    @Test
    public void testKnownWidgetsAreNotFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "${widget:date}\n" +
                        "${widget:languages}\n");

        assertTrue(validate(src).isEmpty(), "built-in widgets must pass");
    }

    @Test
    public void testLanguageMarkerNotInLanguagesPropertyFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "English<!--en-->\n" +
                        "Русский<!--ru-->\n" +
                        "Spanish<!--es-->\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertTrue(diags.get(0).isError());
        assertEquals(4, diags.get(0).getLine());
        assertTrue(diags.get(0).getMessage().contains("'es'"));
        assertTrue(diags.get(0).getMessage().contains("nrg.languages"));
    }

    @Test
    public void testMissingImportFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "${widget:import(path='does-not-exist.src.md')}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertTrue(diags.get(0).isError());
        assertTrue(diags.get(0).getMessage().contains("does-not-exist.src.md"));
    }

    @Test
    public void testExistingImportNotFlagged() throws IOException {
        Path included = writeSource("inc.src.md", "Inner content\n");
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "${widget:import(path='inc.src.md')}\n");

        assertTrue(validate(src).isEmpty(), "existing import must not be flagged");
    }

    @Test
    public void testUnbalancedIgnoreBeginFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "before\n" +
                        "<!--nrg.ignore.begin-->\n" +
                        "inside\n" +
                        "[no end marker]\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertTrue(diags.get(0).isError());
        assertTrue(diags.get(0).getMessage().toLowerCase().contains("unclosed"));
        assertEquals(3, diags.get(0).getLine());
    }

    @Test
    public void testStrayIgnoreEndFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "before\n" +
                        "<!--nrg.ignore.end-->\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertTrue(diags.get(0).isError());
        assertEquals(3, diags.get(0).getLine());
    }

    @Test
    public void testBalancedIgnoreNotFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "<!--nrg.ignore.begin-->\n" +
                        "drop\n" +
                        "<!--nrg.ignore.end-->\n");

        assertTrue(validate(src).isEmpty(), "balanced markers must pass");
    }

    @Test
    public void testValidatorRecursesIntoImports() throws IOException {
        Path inc = writeSource("inc.src.md", "${widget:bogus(x='1')}\n");
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "${widget:import(path='inc.src.md')}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertTrue(diags.get(0).isError());
        assertTrue(diags.get(0).getFile().getName().equals("inc.src.md"),
                "diagnostic must reference the imported file: " + diags.get(0).getFile());
        assertTrue(diags.get(0).getMessage().contains("bogus"));
    }

    @Test
    public void testDiagnosticHasFilePathAndLineNumber() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "first\n" +
                        "${widget:nope}\n" +
                        "${widget:doh}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(2, diags.size(), diags.toString());
        assertEquals(3, diags.get(0).getLine());
        assertEquals(4, diags.get(1).getLine());
        assertTrue(diags.get(0).getFile().getName().equals("README.src.md"));
    }

    @Test
    public void testCustomWidgetsRegisteredViaPropertyAreRecognised() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "<!--@nrg.widgets=com.nanolaba.nrg.examples.ExampleWidget-->\n" +
                        "${widget:exampleWidget(name='X')}\n");

        // ExampleWidget is on the test classpath; validator should accept it.
        assertTrue(validate(src).isEmpty(), "custom widget on classpath must validate");
    }

    @Test
    public void testImportedFileInheritsLanguagesFromRoot() throws IOException {
        // The imported file does NOT declare nrg.languages itself but uses <!--ru--> markers;
        // it must inherit the languages declared in the root template.
        Path inc = writeSource("inc.src.md", "Hello<!--en-->\nПривет<!--ru-->\n");
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "${widget:import(path='inc.src.md')}\n");

        assertTrue(validate(src).isEmpty(), "imported file must inherit nrg.languages from root");
    }

    @Test
    public void testEscapedWidgetMarkerNotFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "Use \\${widget:bogus} to write a literal example.\n");

        // Escaped markers aren't widget calls; validator must not flag them.
        assertTrue(validate(src).isEmpty(), "escaped widget marker must not be flagged");
    }

    @Test
    public void testMissingSourceFileIsTheOnlyDiagnostic() {
        File missing = tempDir.resolve("missing.src.md").toFile();

        List<Validator.Diagnostic> diags = new Validator(missing).validate();

        assertEquals(1, diags.size(), diags.toString());
        assertEquals("source-not-found", diags.get(0).getCode());
        assertEquals(Validator.Severity.ERROR, diags.get(0).getSeverity());
        assertEquals("source file does not exist: " + missing.getAbsolutePath(), diags.get(0).getMessage());

        List<Validator.Diagnostic> forNull = new Validator(null).validate();
        assertEquals(1, forNull.size(), forNull.toString());
        assertEquals("source file does not exist: <null>", forNull.get(0).getMessage());
    }

    @Test
    public void testUnreadableConfigurationIsReported() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "<!--@nrg.defaultLanguage=de-->\n" +
                        "${widget:doesNotExist}\n");

        List<Validator.Diagnostic> diags = validate(src);

        assertEquals(1, diags.size(), diags.toString());
        assertEquals("config-error", diags.get(0).getCode());
        assertTrue(diags.get(0).getMessage().startsWith("cannot read the template configuration: The default language"),
                diags.get(0).getMessage());
    }

    @Test
    public void testUnreadableSourceFileIsAConfigurationError() throws IOException {
        Path src = writeSource("README.src.md", "<!--@nrg.languages=en-->\n");

        try (ReadDenial ignored = ReadDenial.of(src)) {
            List<Validator.Diagnostic> diags = validate(src);

            assertEquals(1, diags.size(), diags.toString());
            assertEquals("config-error", diags.get(0).getCode());
        }
    }

    @Test
    public void testNestedIgnoreBeginFlagged() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "<!--nrg.ignore.begin-->\n" +
                        "<!--nrg.ignore.begin-->\n" +
                        "<!--nrg.ignore.end-->\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertEquals("unbalanced-ignore", diags.get(0).getCode());
        assertEquals(3, diags.get(0).getLine());
        assertTrue(diags.get(0).getMessage().startsWith("nested <!--nrg.ignore.begin-->"), diags.get(0).getMessage());
    }

    @Test
    public void testUndeclaredLanguageHints() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "Russian<!--rus-->\n" +
                        "French<!--fr-->\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(2, diags.size(), diags.toString());
        assertEquals("did you mean 'ru'?", diags.get(0).getHint());
        assertEquals("add it to nrg.languages or remove the marker", diags.get(1).getHint());
    }

    @Test
    public void testImportWithoutParametersIsNotAMissingFile() throws IOException {
        Path src = writeSource("README.src.md", "<!--@nrg.languages=en-->\n${widget:import}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertTrue(diags.isEmpty(), diags.toString());
    }

    @Test
    public void testImportedFileInheritsDisabledParity() throws IOException {
        writeSource("part.src.md", "Only English<!--en-->\n");
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "<!--@nrg.validate.parity=false-->\n" +
                        "${widget:import(path='part.src.md')}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertTrue(diags.isEmpty(), diags.toString());
    }

    @Test
    public void testLanguageScopedPropertyNeedsAName() throws IOException {
        // "<!--@.ru=...-->" is not a Russian value of some property: it declares no "" property
        // that "${x}" could be a near miss of.
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "<!--@.ru=1-->\n" +
                        "${x}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertEquals("undefined-variable", diags.get(0).getCode());
        assertTrue(diags.get(0).isWarning(), diags.toString());
    }

    @Test
    public void testUnresolvedNpmAndGradleReferencesAreWarnings() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "Name: ${npm.name}\n" +
                        "Group: ${gradle.group}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(2, diags.size(), diags.toString());
        assertEquals("unresolved-reference", diags.get(0).getCode());
        assertEquals(2, diags.get(0).getLine());
        assertTrue(diags.get(0).getMessage().startsWith("${npm.name}"), diags.get(0).getMessage());
        assertEquals("unresolved-reference", diags.get(1).getCode());
        assertEquals(3, diags.get(1).getLine());
        assertTrue(diags.get(1).getMessage().startsWith("${gradle.group}"), diags.get(1).getMessage());
    }

    @Test
    public void testDescriptorOutsideTheProjectRootIsAnError() throws IOException {
        Path docs = Files.createDirectories(tempDir.resolve("repo/docs/.git")).getParent();
        Files.write(tempDir.resolve("repo/pom.xml"), "<project><version>2.0</version></project>".getBytes(StandardCharsets.UTF_8));
        Path src = docs.resolve("README.src.md");
        Files.write(src, ("<!--@nrg.pom.path=../pom.xml-->\n" +
                "Version ${pom.version}\n").getBytes(StandardCharsets.UTF_8));

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertEquals("descriptor-error", diags.get(0).getCode());
        assertEquals(2, diags.get(0).getLine());
        assertTrue(diags.get(0).getMessage().contains("outside the project root"), diags.get(0).getMessage());

        // The root given by the caller is the one descriptors are confined to.
        List<Validator.Diagnostic> widened = new Validator(src.toFile(), tempDir.resolve("repo").toFile()).validate();
        assertTrue(widened.isEmpty(), widened.toString());
    }

    @Test
    public void testDescriptorReferencesOfAnImportedFileAreResolved() throws IOException {
        writeSource("pom.xml", "<project><version>2.0</version></project>");
        writeSource("part.src.md", "Version ${pom.version}\n");
        Path src = writeSource("README.src.md", "${widget:import(path='part.src.md')}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertTrue(diags.isEmpty(), diags.toString());
    }

    @Test
    public void testImportedFileWithBrokenConfigurationUsesTheParentOne() throws IOException {
        writeSource("pom.xml", "<project><version>2.0</version></project>");
        writeSource("part.src.md", "<!--@nrg.defaultLanguage=de-->\nVersion ${pom.version}\n");
        Path src = writeSource("README.src.md", "${widget:import(path='part.src.md')}\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertTrue(diags.isEmpty(), diags.toString());
    }

    @Test
    public void testDiagnosticAccessorsAndTextForm() {
        File file = new File("README.src.md");
        Validator.Diagnostic error = new Validator.Diagnostic(file, 3, Validator.Severity.ERROR, "unknown-widget",
                "unknown widget 'x'", "fix the name");
        Validator.Diagnostic warning = new Validator.Diagnostic(null, 0, "plain", Validator.Severity.WARNING);

        assertEquals(Validator.Severity.ERROR, error.getSeverity());
        assertTrue(error.isError());
        assertFalse(error.isWarning());
        assertEquals(Validator.Severity.WARNING, warning.getSeverity());
        assertTrue(warning.isWarning());
        assertFalse(warning.isError());
        assertEquals(Validator.Diagnostic.DEFAULT_CODE, warning.getCode());
        assertEquals("ERROR: README.src.md:3: unknown widget 'x'; fix the name", error.toString());
        assertEquals("WARNING: <unknown>:0: plain", warning.toString());

        assertEquals("", Validator.format(Collections.emptyList()));
        assertEquals(error + System.lineSeparator() + warning, Validator.format(Arrays.asList(error, warning)));
    }
}
