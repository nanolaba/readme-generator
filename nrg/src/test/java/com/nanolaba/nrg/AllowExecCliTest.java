package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllowExecCliTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path writeSource(String body) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testExecDisabledByDefault() throws IOException {
        Path src = writeSource("${widget:exec(cmd='echo hi')}\n");

        NRG.main("-f", src.toString());

        String err = getErrAndClear();
        assertTrue(err.contains("exec widget: execution is disabled"), err);
    }

    @Test
    public void testAllowExecFlagEnablesWidget() throws IOException {
        Path src = writeSource("${widget:exec(cmd='nrg_nonexistent_binary_xxyyzz')}\n");

        NRG.main("-f", src.toString(), "--allow-exec");

        String err = getErrAndClear();
        assertFalse(err.contains("exec widget: execution is disabled"), err);
        assertTrue(err.contains("exec widget: failed to run command"), err);
    }
}
