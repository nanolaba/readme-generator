package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        "Spanish<!--es-->\n");

        List<Validator.Diagnostic> diags = validate(src);
        assertEquals(1, diags.size(), diags.toString());
        assertTrue(diags.get(0).isError());
        assertEquals(3, diags.get(0).getLine());
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
}
