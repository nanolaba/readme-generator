package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CheckCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSrc(String body) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    private String expectedFor(Path src, String language) throws IOException {
        NRG.main("--stdout", "--language", language, "-f", src.toString());
        return getOutAndClear();
    }

    @Test
    public void testCheckPassesWhenFilesMatch() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n- item\n");
        String en = expectedFor(src, "en");
        Files.write(tempDir.resolve("README.md"), en.getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check", "-f", src.toString());

        assertEquals(0, code);
        assertEquals("", getErrAndClear(), "no diff expected");
    }

    @Test
    public void testCheckFailsWithDiffWhenContentDiffers() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\nline A\nline B\nline C\n");
        Files.write(tempDir.resolve("README.md"),
                ("## Title\nline A\nSTALE\nline C\n").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("--- README.md (on disk)"), "missing --- header, got: " + err);
        assertTrue(err.contains("+++ README.md (generated)"));
        assertTrue(err.contains("-STALE"), "missing removed line marker, got: " + err);
    }

    @Test
    public void testCheckFailsWhenTargetFileMissing() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--check", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("Missing file: README.md"), "missing-file error not found, got: " + err);
    }

    @Test
    public void testCheckValidatesAllLanguages() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en,ru-->\n## Title\n");
        String en = expectedFor(src, "en");
        String ru = expectedFor(src, "ru");
        Files.write(tempDir.resolve("README.md"), en.getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("README.ru.md"),
                (ru + "EXTRA LINE\n").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("README.ru.md"), "should report RU mismatch, got: " + err);
        assertFalse(err.contains("+++ README.md (generated)"), "EN should be OK, got: " + err);
    }

    @Test
    public void testCheckDoesNotWriteFiles() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en,ru-->\n## Title\n");

        NRG.run("--check", "-f", src.toString());

        assertFalse(Files.exists(tempDir.resolve("README.md")));
        assertFalse(Files.exists(tempDir.resolve("README.ru.md")));
    }

    @Test
    public void testCheckAndStdoutAreMutuallyExclusive() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--check", "--stdout", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("--stdout and --check are mutually exclusive"), err);
    }

    @Test
    public void testUnifiedDiffHeaderAndContext() {
        String diff = NRG.unifiedDiff(
                "line1\nline2 old\nline3\n",
                "line1\nline2 NEW\nline3\n",
                "README.md");

        assertTrue(diff.contains("--- README.md (on disk)"));
        assertTrue(diff.contains("+++ README.md (generated)"));
        assertTrue(diff.contains("@@ line 2 @@"));
        assertTrue(diff.contains("-line2 old"));
        assertTrue(diff.contains("+line2 NEW"));
        assertTrue(diff.contains(" line1"));
        assertTrue(diff.contains(" line3"));
    }

    @Test
    public void testUnifiedDiffReturnsEmptyWhenIdentical() {
        assertEquals("", NRG.unifiedDiff("abc", "abc", "x.md"));
    }
}
