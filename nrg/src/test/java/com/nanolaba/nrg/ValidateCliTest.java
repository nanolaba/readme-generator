package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ValidateCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSource(String name, String body) throws IOException {
        Path src = tempDir.resolve(name);
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testValidateCleanTemplateExitsZeroSilently() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "Hello\n");

        int code = NRG.run("--validate", "-f", src.toString());

        assertEquals(0, code);
        assertTrue(getErrAndClear().isEmpty(), "expected silent stderr on clean template");
        assertFalse(Files.exists(tempDir.resolve("README.md")), "must not generate files");
    }

    @Test
    public void testValidateBadTemplateExitsOneWithDiagnostics() throws IOException {
        Path src = writeSource("README.src.md",
                "<!--@nrg.languages=en-->\n" +
                        "${widget:doesNotExist}\n");

        int code = NRG.run("--validate", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("doesNotExist"), err);
        assertTrue(err.contains("README.src.md"), err);
        assertFalse(Files.exists(tempDir.resolve("README.md")), "must not generate files");
    }

    @Test
    public void testValidateMutuallyExclusiveWithCheck() throws IOException {
        Path src = writeSource("README.src.md", "X");
        int code = NRG.run("--validate", "--check", "-f", src.toString());

        assertEquals(1, code);
        String err = getErrAndClear();
        assertTrue(err.contains("mutually exclusive"), err);
    }
}
