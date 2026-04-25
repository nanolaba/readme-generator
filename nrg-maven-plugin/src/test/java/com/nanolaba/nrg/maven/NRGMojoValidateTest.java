package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
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
}
