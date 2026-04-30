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
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NRGMojoCheckPathsTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void noSystemExit() {
        NRG.exitOnFailure = false;
    }

    private Path writeSource(String body) throws IOException {
        Path src = tempDir.resolve("README.src.md");
        Files.write(src, body.getBytes(StandardCharsets.UTF_8));
        return src;
    }

    private String renderEn(Path src) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(buf));
        try {
            NRG.run("--stdout", "--language", "en", "-f", src.toString());
        } finally {
            System.setOut(originalOut);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    @Test
    void testCheckPathsForwardedAndChecksOnlyMatchedOutputs() throws Exception {
        Path src = writeSource("<!--@nrg.languages=en,ru-->\n## Title\n");
        // Only EN tracked on disk; RU is "bot-managed" and absent.
        Files.write(tempDir.resolve("README.md"), renderEn(src).getBytes(StandardCharsets.UTF_8));

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.setCheck(true);
        mojo.setCheckPaths(Collections.singletonList(tempDir.resolve("README.md").toString()));

        assertDoesNotThrow(mojo::execute,
                "build should pass: RU absent on disk is excluded by the filter");
    }

    @Test
    void testCheckPathsWithoutCheckFails() throws Exception {
        Path src = writeSource("<!--@nrg.languages=en-->\n## Title\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.setCheck(false);
        mojo.setCheckPaths(Arrays.asList("README.md"));

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("checkPaths") && ex.getMessage().contains("check"),
                "message should mention <checkPaths> requires <check>: " + ex.getMessage());
    }

    @Test
    void testEmptyCheckPathsBehavesLikeAbsent() throws Exception {
        Path src = writeSource("<!--@nrg.languages=en-->\n## Title\n");

        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        mojo.setCheck(false);
        mojo.setCheckPaths(Collections.<String>emptyList());

        // Empty list, check=false → no error (would only error in generate-mode if file absent;
        // since this is just a generate run, it should produce README.md and succeed).
        assertDoesNotThrow(mojo::execute);
        assertTrue(Files.exists(tempDir.resolve("README.md")));
    }
}
