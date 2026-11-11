package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NRGMojoValidateTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        NRG.exitOnFailure = false;
    }

    private Path writeSource(String body) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testValidateModeOnCleanTemplateDoesNotThrowAndDoesNotGenerate() throws Exception {
        Path src = writeSource("<!--@nrg.languages=en-->\nBody\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.setValidate(true);

        assertDoesNotThrow(mojo::execute);
        assertFalse(Files.exists(tempDir.resolve("README.md")), "must not generate files in validate mode");
    }

    @Test
    public void testValidateModeOnBadTemplateRaisesMojoExecutionException() throws Exception {
        Path src = writeSource("<!--@nrg.languages=en-->\n${widget:doesNotExist}\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.setValidate(true);

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("validation"), e.getMessage());
    }

    @Test
    public void testMissingSourceIsReportedAsConfigurationError() {
        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{tempDir.resolve("nope.src.md").toString()});

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("Invalid plugin configuration"), e.getMessage());
    }

    @Test
    public void testFormatIsPassedToTheCli() throws Exception {
        Path src = writeSource("<!--@nrg.languages=en-->\nBody\n");
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, "UTF-8"));
        try {
            NRGMojo mojo = new NRGMojo();
            mojo.setFile(new String[]{src.toString()});
            mojo.setValidate(true);
            mojo.setFormat("json");
            mojo.execute();
        } finally {
            System.setOut(originalOut);
        }
        String out = new String(captured.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(out.trim().startsWith("{") && out.contains("\"mode\": \"validate\""), out);
    }
}
