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
        assertEquals(NRG.EXIT_TEMPLATE_ERROR, code, "expected non-zero exit on collision");
        assertFalse(tmp.resolve("README.md").toFile().exists(),
                "no file should be written when validation fails");
    }

    @Test
    void templatePatternCannotWriteOutsideTheProjectRoot(@TempDir Path tmp) throws Exception {
        // A file name pattern is template data. "../" in it used to write anywhere the process
        // could — with nrg.noHeader the content was fully template-controlled too.
        Path src = Files.createDirectories(tmp.resolve("repo/docs")).resolve("a.src.md");
        Files.write(src, ("<!--@nrg.noHeader=true-->\n" +
                "<!--@nrg.fileNamePattern=../../outside/hook.sh-->\n" +
                "echo pwned\n").getBytes(StandardCharsets.UTF_8));

        int code = NRG.run("-f", src.toAbsolutePath().toString());

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, code);
        assertFalse(tmp.resolve("outside").toFile().exists(), "nothing may be created outside the root");
        String out = getOutAndClear() + getErrAndClear();
        assertTrue(out.contains("outside the project root"), out);
        assertTrue(out.contains("--root"), out);
    }

    @Test
    void checkAndStdoutModesRejectEscapingPatternsToo(@TempDir Path tmp) throws Exception {
        Path src = Files.createDirectories(tmp.resolve("repo")).resolve("a.src.md");
        Files.write(src, ("<!--@nrg.fileNamePattern=../<base>.md-->\nHello\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("-f", src.toAbsolutePath().toString(), "--check"));
        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("-f", src.toAbsolutePath().toString(), "--stdout"));
    }

    @Test
    void patternMayLeaveTheSourceDirectoryInsideAGitRepository(@TempDir Path tmp) throws Exception {
        Files.createDirectories(tmp.resolve("repo/.git"));
        Path src = Files.createDirectories(tmp.resolve("repo/docs")).resolve("README.src.md");
        Files.write(src, ("<!--@nrg.fileNamePattern=../<base>.md-->\nHello\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(0, NRG.run("-f", src.toAbsolutePath().toString()));
        assertTrue(tmp.resolve("repo/README.md").toFile().isFile());
    }

    @Test
    void rootOptionWidensTheRoot(@TempDir Path tmp) throws Exception {
        Path src = Files.createDirectories(tmp.resolve("plain/docs")).resolve("README.src.md");
        Files.write(src, ("<!--@nrg.fileNamePattern=../<base>.md-->\nHello\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("-f", src.toAbsolutePath().toString()));
        assertEquals(0, NRG.run("-f", src.toAbsolutePath().toString(),
                "--root", tmp.resolve("plain").toString()));
        assertTrue(tmp.resolve("plain/README.md").toFile().isFile());
    }

    @Test
    void rootOptionMustBeADirectory(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("README.src.md");
        Files.write(src, "Hello\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(NRG.EXIT_USAGE, NRG.run("-f", src.toAbsolutePath().toString(),
                "--root", tmp.resolve("missing").toString()));
        assertTrue(getErrAndClear().contains("--root is not a directory"));
    }
}
