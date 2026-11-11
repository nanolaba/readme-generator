package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the {@code --validate} rules aimed at near-miss syntax — the kind of mistake a template
 * author (or a coding agent) makes when writing NRG from memory: unknown widget parameters,
 * undefined variables, unbalanced block widgets, cosmetic {@code <!--\@nrg.…-->} escapes and
 * descriptor references that render empty, plus the "did you mean" hints attached to them.
 */
class ValidatorAuthoringRulesTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path write(String name, String body) throws IOException {
        Path src = tempDir.resolve(name);
        Files.createDirectories(src.getParent());
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    private List<Validator.Diagnostic> validate(Path src) {
        return new Validator(src.toFile()).validate();
    }

    private static List<Validator.Diagnostic> errors(List<Validator.Diagnostic> all) {
        return all.stream().filter(Validator.Diagnostic::isError).collect(Collectors.toList());
    }

    private static List<Validator.Diagnostic> warnings(List<Validator.Diagnostic> all) {
        return all.stream().filter(Validator.Diagnostic::isWarning).collect(Collectors.toList());
    }

    @Test
    public void testUnknownWidgetSuggestsClosestName() throws IOException {
        List<Validator.Diagnostic> d = validate(write("README.src.md", "${widget:tocc}\n"));

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).toString().contains("did you mean 'toc'?"), d.get(0).getMessage());
    }

    @Test
    public void testUnknownWidgetParameterIsAnError() throws IOException {
        List<Validator.Diagnostic> d = validate(write("README.src.md", "${widget:toc(maxDepth='2')}\n"));

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).isError());
        assertEquals(1, d.get(0).getLine());
        assertTrue(d.get(0).toString().contains("widget 'toc' has no parameter 'maxDepth'"), d.get(0).getMessage());
        assertTrue(d.get(0).toString().contains("did you mean 'max-depth'?"), d.get(0).getMessage());
    }

    @Test
    public void testUnknownParameterWithoutCloseMatchListsKnownOnes() throws IOException {
        List<Validator.Diagnostic> d = validate(write("README.src.md",
                "${widget:date(format='yyyy')}\n${widget:languages(x='1')}\n"));

        assertEquals(2, d.size(), d.toString());
        assertTrue(d.get(0).toString().contains("known parameters: pattern"), d.get(0).getMessage());
        assertTrue(d.get(1).toString().contains("takes no parameters"), d.get(1).getMessage());
    }

    @Test
    public void testDocumentedParametersOfEveryBuiltInPass() throws IOException {
        Path src = write("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "${widget:toc(title='T', ordered='true', min-depth='1', max-depth='3', min-items='2', " +
                        "anchor-style='github', numbering-style='dotted', start='x')}\n" +
                        "${widget:alert(type='note', text='x')}\n" +
                        "${widget:badge(type='github-workflow', repo='a/b', workflow='ci.yml', name='CI', branch='main', alt='x', url='u')}\n" +
                        "${widget:badge(type='custom', label='a', message='b', color='red')}\n" +
                        "${widget:badge(type='maven-central', coordinates='a:b')} ${widget:badge(type='license', value='MIT')}\n" +
                        "${widget:date(pattern='yyyy')} ${widget:todo(text='x')}\n" +
                        "${widget:math(expr='x', display='block', renderer='svg', service='s', alt='a')}\n" +
                        "${widget:exec(cmd='echo', trim='false', timeout='5s', cwd='.', codeblock='text')}\n" +
                        "${widget:fileTree(path='.', depth='1', dirsOnly='true', codeblock='false', exclude='x')}\n" +
                        "${widget:if(cond='true', text='x')} ${widget:details(summary='s', content='c', open='true')}\n" +
                        "${widget:languages}\n");

        assertTrue(validate(src).isEmpty(), validate(src).toString());
    }

    @Test
    public void testCustomWidgetWithoutDeclaredParametersIsNotChecked() throws IOException {
        Path src = write("README.src.md",
                "<!--@nrg.widgets=com.nanolaba.nrg.examples.ExampleWidget-->\n" +
                        "${widget:exampleWidget(anything='X')}\n");

        assertTrue(validate(src).isEmpty(), validate(src).toString());
    }

    @Test
    public void testUndefinedVariableNearDeclaredOneIsAnError() throws IOException {
        List<Validator.Diagnostic> d = validate(write("README.src.md",
                "<!--@stableVersion=1.0-->\nVersion ${stableVerison}\n"));

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).isError());
        assertEquals(2, d.get(0).getLine());
        assertTrue(d.get(0).toString().contains("did you mean 'stableVersion'?"), d.get(0).getMessage());
    }

    @Test
    public void testUnrelatedUndefinedVariableIsOnlyAWarning() throws IOException {
        // A shell example such as ${HOME} legitimately stays in the output as written.
        List<Validator.Diagnostic> d = validate(write("README.src.md", "echo ${HOME}\n"));

        assertTrue(errors(d).isEmpty(), d.toString());
        assertEquals(1, warnings(d).size(), d.toString());
        assertTrue(d.get(0).toString().contains("\\${HOME}"), d.get(0).getMessage());
    }

    @Test
    public void testVariablesThatResolveAreNotReported() throws IOException {
        write("docs/part.src.md", "${name} ${title} ${local}\n<!--@local=1-->\n");
        Path src = write("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "<!--@name=x-->\n" +
                        "<!--@title.ru=y-->\n" +
                        "Escaped \\${undefined} and ${env.HOME} ${pom.version:1} ${npm.name:x} ${gradle.version:2}\n" +
                        "${widget:import(path='docs/part.src.md')}\n");

        assertTrue(validate(src).isEmpty(), validate(src).toString());
    }

    @Test
    public void testIgnoredLinesAreNotCheckedForVariables() throws IOException {
        Path src = write("README.src.md",
                "${undefinedOne}<!--nrg.ignore-->\n" +
                        "<!--nrg.ignore.begin-->\n${undefinedTwo}\n<!--nrg.ignore.end-->\n");

        assertTrue(validate(src).isEmpty(), validate(src).toString());
    }

    @Test
    public void testPlainTextImportIsNotScanned() throws IOException {
        write("snippet.sh", "echo ${HOME} ${widget:bogus}\n");
        Path src = write("README.src.md", "${widget:import(path='snippet.sh', run-generator='false', wrap='true')}\n");

        assertTrue(validate(src).isEmpty(), validate(src).toString());
    }

    @Test
    public void testUnclosedAndStrayBlockWidgets() throws IOException {
        List<Validator.Diagnostic> d = validate(write("README.src.md",
                "${widget:if(cond='true')}\n" +
                        "body\n" +
                        "${widget:endDetails}\n"));

        assertEquals(2, d.size(), d.toString());
        assertEquals(1, d.get(0).getLine());
        assertTrue(d.get(0).toString().contains("unclosed ${widget:if}"), d.get(0).getMessage());
        assertTrue(d.get(0).toString().contains("${widget:endIf}"), d.get(0).getMessage());
        assertEquals(3, d.get(1).getLine());
        assertTrue(d.get(1).toString().contains("${widget:endDetails} without a matching ${widget:details}"),
                d.get(1).getMessage());
    }

    @Test
    public void testBalancedNestedAndInlineBlocksPass() throws IOException {
        Path src = write("README.src.md",
                "${widget:details(summary='s')}\n" +
                        "${widget:if(cond='true')}\n" +
                        "${widget:if(cond='true', text='inline needs no closer')}\n" +
                        "${widget:endIf}\n" +
                        "${widget:endDetails}\n");

        assertTrue(validate(src).isEmpty(), validate(src).toString());
    }

    @Test
    public void testCosmeticNrgPropertyEscapeIsAWarning() throws IOException {
        List<Validator.Diagnostic> d = validate(write("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "```\n<!--\\@nrg.languages=agents,claude-->\n<\\!--@nrg.languages=shown,literally-->\n```\n"));

        assertTrue(errors(d).isEmpty(), d.toString());
        assertEquals(1, warnings(d).size(), d.toString());
        assertEquals(3, d.get(0).getLine());
        assertTrue(d.get(0).toString().contains("<\\!--@nrg.languages"), d.get(0).getMessage());
    }

    @Test
    public void testDescriptorReferenceThatRendersEmptyIsAWarning() throws IOException {
        write("pom.xml", "<project><version>2.0</version></project>");
        List<Validator.Diagnostic> d = validate(write("README.src.md",
                "${pom.version} ${pom.artifactId} ${pom.artifactId:fallback}\n"));

        assertEquals(1, d.size(), d.toString());
        assertTrue(d.get(0).isWarning());
        assertTrue(d.get(0).toString().contains("${pom.artifactId}"), d.get(0).getMessage());
    }

    @Test
    public void testDiagnosticsAreOrderedByFileThenLine() throws IOException {
        write("part.src.md", "${widget:nopeA}\n");
        List<Validator.Diagnostic> d = validate(write("README.src.md",
                "${undefinedRootVar}\n" +
                        "${widget:import(path='part.src.md')}\n" +
                        "${widget:nopeB}\n"));

        assertEquals("README.src.md", d.get(0).getFile().getName());
        assertEquals(1, d.get(0).getLine());
        assertEquals("README.src.md", d.get(1).getFile().getName());
        assertEquals(3, d.get(1).getLine());
        assertEquals("part.src.md", d.get(2).getFile().getName());
    }
}
