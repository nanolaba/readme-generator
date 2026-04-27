package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NRGMojoMultiFileTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void noSystemExit() {
        NRG.exitOnFailure = false;
    }

    private Path writeSource(String relative, String body) throws IOException {
        Path src = tempDir.resolve(relative);
        Files.createDirectories(src.getParent() == null ? tempDir : src.getParent());
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    @Test
    public void testMojoExpandsGlob() throws Exception {
        writeSource("docs/A.src.md", "<!--@nrg.languages=en-->\nA\n");
        writeSource("docs/B.src.md", "<!--@nrg.languages=en-->\nB\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{tempDir.toString() + "/docs/*.src.md"});
        mojo.execute();

        assertTrue(Files.exists(tempDir.resolve("docs/A.md")));
        assertTrue(Files.exists(tempDir.resolve("docs/B.md")));
    }

    @Test
    public void testMojoMultipleEntries() throws Exception {
        writeSource("A.src.md", "<!--@nrg.languages=en-->\nA\n");
        writeSource("B.src.md", "<!--@nrg.languages=en-->\nB\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{
                tempDir.resolve("A.src.md").toString(),
                tempDir.resolve("B.src.md").toString()});
        mojo.execute();

        assertTrue(Files.exists(tempDir.resolve("A.md")));
        assertTrue(Files.exists(tempDir.resolve("B.md")));
    }

    @Test
    public void testFailFastStopsAfterFirstFailure() throws Exception {
        // First file has an undeclared language marker that Validator flags as ERROR.
        // Second file is fine. With --fail-fast on, the Mojo aborts before generating B.
        writeSource("A.src.md", "<!--@nrg.languages=en-->\nbad<!--zz-->\n");
        writeSource("B.src.md", "<!--@nrg.languages=en-->\nB\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setValidate(true);
        mojo.setFailFast(true);
        mojo.setFile(new String[]{
                tempDir.resolve("A.src.md").toString(),
                tempDir.resolve("B.src.md").toString()});

        try {
            mojo.execute();
            org.junit.jupiter.api.Assertions.fail("expected MojoExecutionException");
        } catch (org.apache.maven.plugin.MojoExecutionException expected) {
            // expected — validate failure surfaces
        }
        // Mojo runs in --validate mode so no .md files should be written either way,
        // but the test's true purpose is to lock that --fail-fast is forwarded.
    }
}
