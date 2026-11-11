package com.nanolaba.nrg;

import com.nanolaba.nrg.core.json.MinimalJsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --format json} / {@code --format github} and the exit codes: one parseable JSON document
 * on stdout (logs on stderr) for every mode, GitHub workflow annotations, and the distinct exit
 * statuses for drift, template errors and usage errors.
 */
public class FormatCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path write(String name, String body) throws IOException {
        Path p = tempDir.resolve(name);
        Files.write(p, body.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        return (Map<String, Object>) MinimalJsonParser.parse(json);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> report, String key) {
        return (List<Map<String, Object>>) report.get(key);
    }

    @Test
    public void testGenerateReportsWrittenThenUnchangedFiles() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nHello<!--en-->\nПривет<!--ru-->\n");

        assertEquals(NRG.EXIT_OK, NRG.run("--format", "json", "-f", src.toString()));
        Map<String, Object> first = parse(getOutAndClear());
        assertEquals("generate", first.get("mode"));
        assertEquals(Boolean.TRUE, first.get("ok"));
        List<Map<String, Object>> files = list(first, "files");
        assertEquals(2, files.size());
        assertEquals("written", files.get(0).get("status"));
        assertTrue(((String) files.get(0).get("output")).endsWith("README.md"), files.toString());
        assertEquals("en", files.get(0).get("language"));
        assertTrue(getErrAndClear().contains("created"), "logs go to stderr in json mode");

        assertEquals(NRG.EXIT_OK, NRG.run("--format", "json", "-f", src.toString()));
        assertEquals("unchanged", list(parse(getOutAndClear()), "files").get(1).get("status"));
    }

    @Test
    public void testCheckReportsStaleAndMissingWithDriftExitCode() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nHello<!--en-->\nПривет<!--ru-->\n");
        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        Files.write(tempDir.resolve("README.md"), "hand edit".getBytes(StandardCharsets.UTF_8));
        Files.delete(tempDir.resolve("README.ru.md"));
        getOutAndClear();
        getErrAndClear();

        assertEquals(NRG.EXIT_DRIFT, NRG.run("--check", "--format", "json", "-f", src.toString()));
        Map<String, Object> report = parse(getOutAndClear());
        assertEquals("check", report.get("mode"));
        assertEquals(Boolean.FALSE, report.get("ok"));
        assertEquals(1L, ((Number) report.get("exitCode")).longValue());
        List<Map<String, Object>> files = list(report, "files");
        assertEquals("stale", files.get(0).get("status"));
        assertEquals("missing", files.get(1).get("status"));
        assertTrue(getErrAndClear().contains("(on disk)"), "the diff still goes to stderr");
    }

    @Test
    public void testTextCheckEndsWithTheFixingCommand() throws IOException {
        Path src = write("README.src.md", "Hello\n");
        NRG.run("-f", src.toString());
        Files.write(tempDir.resolve("README.md"), "hand edit".getBytes(StandardCharsets.UTF_8));
        getErrAndClear();

        assertEquals(NRG.EXIT_DRIFT, NRG.run("--check", "-f", src.toString()));
        assertTrue(getErrAndClear().trim().endsWith("Regenerate with: nrg -f " + src));
    }

    @Test
    public void testValidateReportsDiagnosticsWithCodesAndHints() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\n${widget:tocc}\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--validate", "--format", "json", "-f", src.toString()));
        Map<String, Object> report = parse(getOutAndClear());
        assertEquals("validate", report.get("mode"));
        Map<String, Object> d = list(report, "diagnostics").get(0);
        assertEquals("error", d.get("severity"));
        assertEquals("unknown-widget", d.get("code"));
        assertTrue(((String) d.get("file")).endsWith("/README.src.md"), d.toString());
        assertEquals(2L, ((Number) d.get("line")).longValue());
        assertEquals("unknown widget 'tocc'", d.get("message"));
        assertEquals("did you mean 'toc'?", d.get("hint"));
        assertEquals("invalid", list(report, "files").get(0).get("status"));
        assertFalse(getErrAndClear().contains("ERROR: README.src.md"), "no text diagnostics in json mode");
    }

    @Test
    public void testGenerationFailureIsATemplateErrorWithADiagnostic() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.fileNamePattern=../../outside/<base>.md-->\nx\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json", "-f", src.toString()));
        Map<String, Object> d = list(parse(getOutAndClear()), "diagnostics").get(0);
        assertEquals("file-name-pattern", d.get("code"));
    }

    @Test
    public void testWidgetExceptionNoLongerEscapesRun() throws IOException {
        Path src = write("README.src.md", "${widget:import(path='../../../../outside.md')}\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json", "-f", src.toString()));
        Map<String, Object> d = list(parse(getOutAndClear()), "diagnostics").get(0);
        assertEquals("generation-failed", d.get("code"));
        assertTrue(((String) d.get("message")).contains("outside the project root"), d.toString());
    }

    @Test
    public void testMissingSourceIsAUsageError() {
        assertEquals(NRG.EXIT_USAGE, NRG.run("--format", "json", "-f", tempDir.resolve("nope.src.md").toString()));
        assertEquals("source-not-found", list(parse(getOutAndClear()), "diagnostics").get(0).get("code"));
    }

    @Test
    public void testUsageErrors() throws IOException {
        Path src = write("README.src.md", "x\n");
        assertEquals(NRG.EXIT_USAGE, NRG.run("--format", "yaml", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("Invalid format"));
        assertEquals(NRG.EXIT_USAGE, NRG.run("--format", "json", "--stdout", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("cannot be combined with --stdout"));
    }

    @Test
    public void testBatchReturnsTheWorstCode() throws IOException {
        Path good = write("a.src.md", "x\n");
        Path bad = write("b.src.md", "${widget:nope}\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR,
                NRG.run("--validate", "--format", "json", good.toString(), bad.toString()));
        Map<String, Object> report = parse(getOutAndClear());
        assertEquals(2, list(report, "files").size());
        assertEquals(1, list(report, "diagnostics").size());
    }

    @Test
    public void testGithubFormatPrintsAnnotations() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\n${widget:tocc}\necho ${HOME}\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--validate", "--format", "github", "-f", src.toString()));
        String out = getOutAndClear();
        assertTrue(out.contains("README.src.md,line=2,title=NRG unknown-widget::unknown widget 'tocc'; did you mean 'toc'?"), out);
        assertTrue(out.contains("::error file="), out);
        assertTrue(out.contains("README.src.md,line=3,title=NRG undefined-variable::"), out);
        assertTrue(out.contains("::warning file="), out);
    }

    @Test
    public void testGithubFormatAnnotatesStaleOutputs() throws IOException {
        Path src = write("README.src.md", "Hello\n");
        NRG.run("-f", src.toString());
        Files.write(tempDir.resolve("README.md"), "hand edit".getBytes(StandardCharsets.UTF_8));
        getOutAndClear();

        assertEquals(NRG.EXIT_DRIFT, NRG.run("--check", "--format", "github", "-f", src.toString()));
        String out = getOutAndClear();
        assertTrue(out.contains("::error file="), out);
        assertTrue(out.contains("title=NRG stale output::"), out);
        assertTrue(out.contains("regenerate with%3A") || out.contains("regenerate with: nrg -f"), out);
    }

    @Test
    public void testJsonStringEscaping() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", RunReport.str("a\"b\\c\nd\u0001"));
    }
}
