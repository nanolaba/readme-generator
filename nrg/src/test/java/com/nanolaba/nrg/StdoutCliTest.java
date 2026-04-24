package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class StdoutCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSource(String name, String body) throws IOException {
        Path src = tempDir.resolve(name);
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testStdoutSingleLanguagePrintsContent() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "# Hello\n" +
                        "content-line\n"
        );

        NRG.main("--stdout", "-f", src.toString());

        String out = getOutAndClear();
        assertTrue(out.contains("# Hello"));
        assertTrue(out.contains("content-line"));
        assertFalse(Files.exists(tempDir.resolve("README.md")), "README.md must not be written");
    }

    @Test
    public void testStdoutMultiLanguagePrintsAllWithSeparators() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "Hello<!--en-->\n" +
                        "Привет<!--ru-->\n"
        );

        NRG.main("--stdout", "-f", src.toString());

        String out = getOutAndClear();
        assertTrue(out.contains("=== README.md ==="), "English separator missing");
        assertTrue(out.contains("=== README.ru.md ==="), "Russian separator missing");
        assertTrue(out.contains("Hello"));
        assertTrue(out.contains("Привет"));
        assertFalse(Files.exists(tempDir.resolve("README.md")));
        assertFalse(Files.exists(tempDir.resolve("README.ru.md")));
    }

    @Test
    public void testStdoutWithLanguageFilterPrintsOnlyThatVariant() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "Hello<!--en-->\n" +
                        "Привет<!--ru-->\n"
        );

        NRG.main("--stdout", "--language", "en", "-f", src.toString());

        String out = getOutAndClear();
        assertTrue(out.contains("Hello"));
        assertFalse(out.contains("Привет"), "ru variant must not leak");
        assertFalse(out.contains("=== README"), "no separator expected for single language");
        assertFalse(Files.exists(tempDir.resolve("README.md")));
        assertFalse(Files.exists(tempDir.resolve("README.ru.md")));
    }

    @Test
    public void testStdoutWithUnknownLanguageLogsError() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "Hello\n"
        );

        NRG.main("--stdout", "--language", "xx", "-f", src.toString());

        String out = getOutAndClear();
        String err = getErrAndClear();

        assertFalse(out.contains("Hello"), "Nothing should be printed for unknown language");
        assertTrue(err.contains("Unknown language 'xx'"), "Expected error about unknown language, got: " + err);
        assertFalse(Files.exists(tempDir.resolve("README.md")));
    }

    @Test
    public void testLanguageWithoutStdoutIsIgnoredWithWarning() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +
                        "Hello<!--en-->\n" +
                        "Привет<!--ru-->\n"
        );

        NRG.main("--language", "en", "-f", src.toString());

        String combined = getOutAndClear() + getErrAndClear();
        assertTrue(combined.contains("--language has no effect without --stdout"));
        // Regular file-writing path: both variants must land on disk
        assertTrue(Files.exists(tempDir.resolve("README.md")));
        assertTrue(Files.exists(tempDir.resolve("README.ru.md")));
    }

    @Test
    public void testStdoutDoesNotWriteAnyFilesForMultipleLanguages() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en,ru,fr-->\n" +
                        "## Title\n"
        );

        long before = Files.list(tempDir).count();
        NRG.main("--stdout", "-f", src.toString());
        long after = Files.list(tempDir).count();

        assertEquals(before, after, "tempDir must not gain any files");
    }
}
