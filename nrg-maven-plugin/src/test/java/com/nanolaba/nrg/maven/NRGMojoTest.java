package com.nanolaba.nrg.maven;

import com.nanolaba.nrg.NRG;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that every {@link NRGMojo} parameter reaches the CLI: each test sets one parameter and
 * looks for its effect on the generated files or on what {@link NRG} reports.
 */
class NRGMojoTest {

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

    private NRGMojo mojoFor(Path src) {
        NRGMojo mojo = new NRGMojo();
        mojo.setFile(new String[]{src.toString()});
        return mojo;
    }

    private String read(String name) throws IOException {
        return new String(Files.readAllBytes(tempDir.resolve(name)), StandardCharsets.UTF_8);
    }

    @Test
    void testNoTemplateFilesOnlyWarns() throws Exception {
        for (String[] files : new String[][]{null, new String[0]}) {
            RecordingLog log = new RecordingLog();
            NRGMojo mojo = new NRGMojo();
            mojo.setLog(log);
            mojo.setFile(files);

            mojo.execute();

            assertEquals(1, log.warnings.size(), String.valueOf(log.warnings));
            assertEquals("No template files specified", log.warnings.get(0));
        }
    }

    @Test
    void testLogLevelIsForwarded() throws Exception {
        NRGMojo mojo = mojoFor(writeSource("Hello\n"));
        mojo.setLogLevel("chatty");

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().contains("Invalid plugin configuration"), e.getMessage());
    }

    @Test
    void testFileNamePatternIsForwarded() throws Exception {
        NRGMojo mojo = mojoFor(writeSource("Hello\n"));
        mojo.setFileNamePattern("<base>-<lang>.md");

        mojo.execute();

        assertTrue(Files.exists(tempDir.resolve("README-en.md")));
        assertFalse(Files.exists(tempDir.resolve("README.md")));
    }

    @Test
    void testDefaultLanguageFileNamePatternIsForwarded() throws Exception {
        NRGMojo mojo = mojoFor(writeSource("Hello\n"));
        mojo.setDefaultLanguageFileNamePattern("<base>.<lang>.md");

        mojo.execute();

        assertTrue(Files.exists(tempDir.resolve("README.en.md")));
        assertFalse(Files.exists(tempDir.resolve("README.md")));
    }

    @Test
    void testLineEndingIsForwarded() throws Exception {
        // One of the two differs from the platform default whatever the platform is.
        Path src = writeSource("one\ntwo\n");
        NRGMojo crlf = mojoFor(src);
        crlf.setLineEnding("crlf");
        crlf.execute();
        assertTrue(read("README.md").contains("one\r\ntwo"), read("README.md"));

        Files.delete(tempDir.resolve("README.md"));
        NRGMojo lf = mojoFor(src);
        lf.setLineEnding("lf");
        lf.execute();
        assertFalse(read("README.md").contains("\r"), read("README.md"));
    }

    @Test
    void testHeaderIsWrittenUnlessSuppressed() throws Exception {
        Path src = writeSource("Hello\n");
        mojoFor(src).execute();
        assertTrue(read("README.md").contains("automatically generated"), read("README.md"));

        NRGMojo noHeader = mojoFor(src);
        noHeader.setNoHeader(true);
        noHeader.execute();
        assertFalse(read("README.md").contains("automatically generated"), read("README.md"));
    }

    @Test
    void testHeaderTextIsForwarded() throws Exception {
        NRGMojo mojo = mojoFor(writeSource("Hello\n"));
        mojo.setHeaderText("Custom head line");

        mojo.execute();

        assertTrue(read("README.md").startsWith("Custom head line"), read("README.md"));
    }

    @Test
    void testEnvironmentIsReadableByDefault() throws Exception {
        mojoFor(writeSource("[${env.PATH:none}]\n")).execute();

        assertFalse(read("README.md").contains("[none]"), read("README.md"));
    }

    @Test
    void testNoEnvIsForwarded() throws Exception {
        NRGMojo mojo = mojoFor(writeSource("[${env.PATH:none}]\n"));
        mojo.setNoEnv(true);

        mojo.execute();

        assertTrue(read("README.md").contains("[none]"), read("README.md"));
    }

    @Test
    void testEnvAllowIsForwarded() throws Exception {
        NRGMojo mojo = mojoFor(writeSource("[${env.PATH:none}]\n"));
        mojo.setEnvAllow("NRG_MOJO_TEST_UNRELATED_*");

        mojo.execute();

        assertTrue(read("README.md").contains("[none]"), read("README.md"));
    }

    @Test
    void testOfflineIsForwarded() throws Exception {
        // Without --offline the import would be refused for another reason (remote imports are
        // not enabled), so no connection is attempted either way.
        NRGMojo mojo = mojoFor(writeSource("${widget:import(url='https://127.0.0.1:9/x.md')}\n"));
        mojo.setOffline(true);

        String err = captureStderr(() -> assertThrows(MojoExecutionException.class, mojo::execute));

        assertTrue(err.contains("disabled by --offline"), err);
    }

    private static String captureStderr(Runnable action) throws IOException {
        PrintStream original = System.err;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buf, true, StandardCharsets.UTF_8.name()));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void testDefaultsGenerateTheFile() throws Exception {
        assertDoesNotThrow(mojoFor(writeSource("Hello\n"))::execute);
        assertTrue(read("README.md").contains("Hello"));
    }

    /** Keeps the warnings instead of printing them. */
    private static final class RecordingLog extends SystemStreamLog {
        final List<String> warnings = new ArrayList<>();

        @Override
        public void warn(CharSequence content) {
            warnings.add(content.toString());
        }
    }
}
