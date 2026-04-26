package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileNamePatternEndToEndTest extends DefaultNRGTest {

    @Test
    void writesIntoLanguageSubdirectory(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("README.src.md");
        Files.write(src, ("<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "<!--@nrg.fileNamePattern=docs/<lang>/<base>.md-->\n" +
                "Hello\n").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("-f", src.toAbsolutePath().toString());
        assertEquals(0, code);

        File enOut = tmp.resolve("docs/en/README.md").toFile();
        File ruOut = tmp.resolve("docs/ru/README.md").toFile();
        assertTrue(enOut.isFile(), "expected " + enOut);
        assertTrue(ruOut.isFile(), "expected " + ruOut);
    }

    @Test
    void cliFileNamePatternBeatsTemplateProperty(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("README.src.md");
        Files.write(src, ("<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "<!--@nrg.fileNamePattern=template/<lang>/<base>.md-->\n" +
                "Hello\n").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("-f", src.toAbsolutePath().toString(),
                "--file-name-pattern", "cli/<lang>/<base>.md");
        assertEquals(0, code);

        assertTrue(tmp.resolve("cli/en/README.md").toFile().isFile());
        assertTrue(tmp.resolve("cli/ru/README.md").toFile().isFile());
        assertFalse(tmp.resolve("template/en/README.md").toFile().exists());
    }

    @Test
    void collisionAborts(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("README.src.md");
        Files.write(src, ("<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "<!--@nrg.fileNamePattern=README.md-->\n" +
                "Hello\n").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("-f", src.toAbsolutePath().toString());
        assertEquals(1, code, "expected non-zero exit on collision");
        assertFalse(tmp.resolve("README.md").toFile().exists(),
                "no file should be written when validation fails");
    }
}
