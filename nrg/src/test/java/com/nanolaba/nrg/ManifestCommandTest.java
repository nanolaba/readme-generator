package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How {@link ManifestCommand} reads a file's previous version from git for
 * {@code --stale-translations}: the process handling behind {@link ManifestCommand#gitShow}
 * is driven with stub processes, so the exit status, an interrupt and a missing directory are
 * covered without depending on what git does.
 */
public class ManifestCommandTest {

    @TempDir
    Path tempDir;

    /** A finished process with fixed output; {@code waitFor} throws when {@code exitCode} is negative. */
    private static final class StubProcess extends Process {
        private final String stdout;
        private final int exitCode;
        boolean stdinClosed;

        StubProcess(String stdout, int exitCode) {
            this.stdout = stdout;
            this.exitCode = exitCode;
        }

        @Override
        public OutputStream getOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) {
                    throw new AssertionError("nothing is written to the process");
                }

                @Override
                public void close() {
                    stdinClosed = true;
                }
            };
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream("warning: something\n".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int waitFor() throws InterruptedException {
            if (exitCode < 0) {
                throw new InterruptedException();
            }
            return exitCode;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
        }
    }

    @Test
    public void testOutputOfASuccessfulProcess() throws IOException {
        StubProcess process = new StubProcess("Привет\n", 0);

        assertEquals("Привет\n", ManifestCommand.output(process));
        assertTrue(process.stdinClosed, "the process gets no input");
    }

    @Test
    public void testFailedProcessHasNoOutput() throws IOException {
        assertNull(ManifestCommand.output(new StubProcess("fatal: invalid object name\n", 128)));
    }

    @Test
    public void testInterruptGivesNoOutputAndKeepsTheInterruptStatus() throws IOException {
        try {
            assertNull(ManifestCommand.output(new StubProcess("text", -1)));
            assertTrue(Thread.currentThread().isInterrupted(), "the interrupt is not swallowed");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testGitShowInAMissingDirectoryGivesNothing() {
        assertNull(ManifestCommand.gitShow(new File(tempDir.toFile(), "missing/README.src.md"), "HEAD"));
    }
}
