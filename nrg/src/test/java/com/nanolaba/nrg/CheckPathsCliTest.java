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

public class CheckPathsCliTest extends DefaultNRGTest {

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
    public void testCheckPathsLimitsToMatchingOutput() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en,ru-->\n## Title\n");
        String en = expectedFor(src, "en");
        Files.write(tempDir.resolve("README.md"), en.getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check",
                "--check-paths", tempDir.resolve("README.md").toString(),
                "-f", src.toString());

        String err = getErrAndClear();
        assertEquals(0, code, "RU absent must be ignored when filter excludes it; stderr=" + err);
        assertEquals("", err);
    }

    @Test
    public void testCheckPathsStillFlagsMatchedFileMissing() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--check",
                "--check-paths", tempDir.resolve("README.md").toString(),
                "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("Missing file: README.md"), err);
    }

    @Test
    public void testCheckPathsStillFlagsMatchedFileDrift() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\nline A\nline B\n");
        Files.write(tempDir.resolve("README.md"),
                "## Title\nline A\nSTALE\n".getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check",
                "--check-paths", tempDir.resolve("README.md").toString(),
                "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("--- README.md (on disk)"), err);
        assertTrue(err.contains("-STALE"), err);
    }

    @Test
    public void testCheckPathsRepeatableFlag() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en,ru-->\n## Title\n");
        Files.write(tempDir.resolve("README.md"), expectedFor(src, "en").getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("README.ru.md"), expectedFor(src, "ru").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check",
                "--check-paths", tempDir.resolve("README.md").toString(),
                "--check-paths", tempDir.resolve("README.ru.md").toString(),
                "-f", src.toString());

        assertEquals(0, code, getErrAndClear());
    }

    @Test
    public void testCheckPathsGlobWithStar() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en,ru,de-->\n## Title\n");
        Files.write(tempDir.resolve("README.md"), expectedFor(src, "en").getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("README.ru.md"), expectedFor(src, "ru").getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("README.de.md"), expectedFor(src, "de").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check",
                "--check-paths", tempDir.toString() + "/README*.md",
                "-f", src.toString());

        assertEquals(0, code, getErrAndClear());
    }

    @Test
    public void testCheckPathsGlobstarMatchesZeroAndMore() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");
        Files.write(tempDir.resolve("README.md"), expectedFor(src, "en").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check",
                "--check-paths", tempDir.toString() + "/**/README.md",
                "-f", src.toString());

        assertEquals(0, code, getErrAndClear());
    }

    @Test
    public void testCheckPathsNoMatchWarns() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");
        Files.write(tempDir.resolve("README.md"), expectedFor(src, "en").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check",
                "--check-paths", tempDir.toString() + "/NOSUCH.md",
                "-f", src.toString());

        assertEquals(0, code);
        String err = getErrAndClear();
        assertTrue(err.contains("WARN: --check-paths matched no generated outputs"),
                "expected zero-match warning, got: " + err);
    }

    @Test
    public void testCheckPathsRequiresCheckFlag() throws IOException {
        Path src = writeSrc("<!--@nrg.languages=en-->\n## Title\n");

        int code = NRG.run("--check-paths", "README.md", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("--check-paths requires --check"), err);
    }

    @Test
    public void testCheckPathsEmptyValueTreatedAsAbsent() throws IOException {
        // Passing --check-paths with an empty string should behave like no filter at all,
        // i.e. *all* outputs are checked. RU absence => exit 1 + Missing file: README.ru.md.
        Path src = writeSrc("<!--@nrg.languages=en,ru-->\n## Title\n");
        Files.write(tempDir.resolve("README.md"), expectedFor(src, "en").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("--check", "--check-paths", "", "-f", src.toString());

        String err = getErrAndClear();
        assertEquals(1, code, err);
        assertTrue(err.contains("Missing file: README.ru.md"), err);
        assertFalse(err.contains("WARN: --check-paths matched no generated outputs"),
                "empty value must not trigger zero-match warning");
    }
}
