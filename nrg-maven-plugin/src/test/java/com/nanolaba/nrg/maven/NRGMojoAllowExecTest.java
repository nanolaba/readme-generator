package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NRGMojoAllowExecTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
    private PrintStream originalErr;

    @BeforeEach
    void captureStderr() {
        NRG.exitOnFailure = false;
        originalErr = System.err;
        System.setErr(new PrintStream(new TeeOutputStream(originalErr, errBuf)));
    }

    @AfterEach
    void restoreStderr() {
        System.setErr(originalErr);
        errBuf.reset();
    }

    private String getErrAndClear() {
        String s = errBuf.toString();
        errBuf.reset();
        return s;
    }

    private Path writeSource(String body) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testMojoExecDisabledByDefault() throws Exception {
        Path src = writeSource("${widget:exec(cmd='echo hi')}\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.execute();

        String err = getErrAndClear();
        assertTrue(err.contains("exec widget: execution is disabled"), err);
    }

    @Test
    public void testMojoAllowExecTruePropagates() throws Exception {
        Path src = writeSource("${widget:exec(cmd='nrg_nonexistent_binary_xxyyzz')}\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.setAllowExec(true);
        mojo.execute();

        String err = getErrAndClear();
        assertFalse(err.contains("exec widget: execution is disabled"), err);
        assertTrue(err.contains("exec widget: failed to run command"), err);
    }
}
