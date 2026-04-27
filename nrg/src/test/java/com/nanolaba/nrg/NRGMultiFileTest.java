package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NRGMultiFileTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSource(String relative, String body) throws IOException {
        Path src = tempDir.resolve(relative);
        Files.createDirectories(src.getParent() == null ? tempDir : src.getParent());
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testMultiplePositionalFilesProcessAll() throws IOException {
        writeSource("A.src.md", "<!--@nrg.languages=en-->\nA-content\n");
        writeSource("B.src.md", "<!--@nrg.languages=en-->\nB-content\n");

        int code = NRG.run(
                tempDir.resolve("A.src.md").toString(),
                tempDir.resolve("B.src.md").toString());

        assertEquals(0, code);
        assertTrue(Files.exists(tempDir.resolve("A.md")));
        assertTrue(Files.exists(tempDir.resolve("B.md")));
    }

    @Test
    public void testGlobPatternProcessesAllMatches() throws IOException {
        writeSource("docs/A.src.md", "<!--@nrg.languages=en-->\nA\n");
        writeSource("docs/B.src.md", "<!--@nrg.languages=en-->\nB\n");

        int code = NRG.run(tempDir.toString() + "/docs/*.src.md");

        assertEquals(0, code);
        assertTrue(Files.exists(tempDir.resolve("docs/A.md")));
        assertTrue(Files.exists(tempDir.resolve("docs/B.md")));
    }

    @Test
    public void testRecursiveGlob() throws IOException {
        writeSource("docs/A.src.md", "<!--@nrg.languages=en-->\nA\n");
        writeSource("docs/sub/B.src.md", "<!--@nrg.languages=en-->\nB\n");

        int code = NRG.run(tempDir.toString() + "/docs/**/*.src.md");

        assertEquals(0, code);
        assertTrue(Files.exists(tempDir.resolve("docs/A.md")));
        assertTrue(Files.exists(tempDir.resolve("docs/sub/B.md")));
    }

    @Test
    public void testFFlagAndPositionalAreMutuallyExclusive() throws IOException {
        Path a = writeSource("A.src.md", "<!--@nrg.languages=en-->\nA\n");
        writeSource("B.src.md", "<!--@nrg.languages=en-->\nB\n");

        int code = NRG.run("-f", a.toString(), tempDir.resolve("B.src.md").toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("-f") && err.contains("positional"),
                "expected mutual-exclusion message, got: " + err);
        assertFalse(Files.exists(tempDir.resolve("A.md")));
        assertFalse(Files.exists(tempDir.resolve("B.md")));
    }

    @Test
    public void testTotalZeroMatchExitsOne() {
        int code = NRG.run(tempDir.toString() + "/nope/**/*.src.md");
        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("No source files matched"),
                "expected total-zero error, got: " + err);
    }

    @Test
    public void testOneEmptyPatternStillProcessesOthers() throws IOException {
        writeSource("A.src.md", "<!--@nrg.languages=en-->\nA\n");

        int code = NRG.run(
                tempDir.toString() + "/nope/*.src.md",
                tempDir.resolve("A.src.md").toString());

        assertEquals(0, code);
        assertTrue(Files.exists(tempDir.resolve("A.md")));
        // warn about the empty pattern should appear on stderr
        String err = getErrAndClear();
        assertTrue(err.contains("nope"),
                "expected warning mentioning the empty pattern, got: " + err);
    }

    @Test
    public void testFailFastStopsOnFirstFailure() throws IOException {
        // Use --validate with an undeclared language marker — Validator returns 1 for "zz" not in
        // nrg.languages. Both files are bad; with --fail-fast we should see only A's diagnostic.
        writeSource("A.src.md",
                "<!--@nrg.languages=en-->\nbad-line<!--zz-->\n");
        writeSource("B.src.md",
                "<!--@nrg.languages=en-->\nbad-line<!--zz-->\n");

        int code = NRG.run("--validate", "--fail-fast",
                tempDir.resolve("A.src.md").toString(),
                tempDir.resolve("B.src.md").toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("A.src.md"), "expected A diagnostic, got: " + err);
        assertFalse(err.contains("B.src.md"), "fail-fast must skip B, got: " + err);
    }

    @Test
    public void testDefaultModeAggregatesAndReturnsOne() throws IOException {
        writeSource("A.src.md",
                "<!--@nrg.languages=en-->\nbad-line<!--zz-->\n");
        writeSource("B.src.md",
                "<!--@nrg.languages=en-->\nbad-line<!--zz-->\n");

        int code = NRG.run("--validate",
                tempDir.resolve("A.src.md").toString(),
                tempDir.resolve("B.src.md").toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("A.src.md"), "expected A diagnostic, got: " + err);
        assertTrue(err.contains("B.src.md"), "expected B diagnostic too (no fail-fast), got: " + err);
    }

    @Test
    public void testStdoutWithMultipleInputsIncludesPerFileSeparators() throws IOException {
        writeSource("A.src.md", "<!--@nrg.languages=en-->\nA-CONTENT\n");
        writeSource("B.src.md", "<!--@nrg.languages=en-->\nB-CONTENT\n");

        int code = NRG.run("--stdout",
                tempDir.resolve("A.src.md").toString(),
                tempDir.resolve("B.src.md").toString());

        assertEquals(0, code);
        String out = getOutAndClear();
        assertTrue(out.contains("=== A.md ==="), "got: " + out);
        assertTrue(out.contains("=== B.md ==="), "got: " + out);
        assertTrue(out.contains("A-CONTENT"));
        assertTrue(out.contains("B-CONTENT"));
    }
}
