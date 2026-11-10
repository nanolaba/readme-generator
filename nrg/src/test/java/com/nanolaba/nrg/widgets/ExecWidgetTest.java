package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecWidgetTest extends DefaultNRGTest {

    private static Generator makeGenerator(String body, ExecWidget widget, boolean allow) {
        Generator g = new Generator(new File("README.src.md"), body,
                Collections.singletonList(widget));
        g.getConfig().setExecAllowed(allow);
        return g;
    }

    @Test
    public void testMissingCmdLogsErrorAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        Generator g = makeGenerator("x\n${widget:exec()}\ny", new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("x"));
        assertTrue(body.contains("y"));
        assertTrue(getErrAndClear().contains("exec widget: missing required 'cmd' parameter"));
        assertEquals(0, runner.invocations.size(), "runner must not run");
    }

    @Test
    public void testExecDisabledLogsErrorAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        Generator g = makeGenerator("${widget:exec(cmd='echo hi')}", new ExecWidget(runner), false);

        g.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("exec widget: execution is disabled"));
        assertEquals(0, runner.invocations.size(), "runner must not run when exec disabled");
    }

    @Test
    public void testHappyPathRunsCommandAndInlinesStdout() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("hello world\n", "");
        Generator g = makeGenerator("before ${widget:exec(cmd='echo hi')} after",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("before hello world after"), body);
        assertEquals(1, runner.invocations.size());
        assertEquals(Arrays.asList("echo", "hi"), runner.invocations.get(0).argv);
    }

    @Test
    public void testCmdSplitsOnWhitespace() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("", "");
        Generator g = makeGenerator("${widget:exec(cmd='java   -version   -XshowSettings')}",
                new ExecWidget(runner), true);

        g.getResult("en").getContent().toString();
        assertEquals(Arrays.asList("java", "-version", "-XshowSettings"),
                runner.invocations.get(0).argv);
    }

    @Test
    public void testTrimTrueStripsTrailingWhitespace() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("hello\n\n  \n", "");
        Generator g = makeGenerator("${widget:exec(cmd='x')}", new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("hello"), body);
        assertFalse(body.contains("hello\n\n"), body);
    }

    @Test
    public void testTrimFalsePreservesTrailingWhitespace() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("hello\n\n", "");
        Generator g = makeGenerator("${widget:exec(cmd='x', trim='false')}",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertTrue(body.contains("hello\n\n"), body);
    }

    @Test
    public void testInvalidTrimLogsErrorAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("x", "");
        Generator g = makeGenerator("${widget:exec(cmd='x', trim='nope')}",
                new ExecWidget(runner), true);

        g.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("exec widget: invalid 'trim'"));
        assertEquals(0, runner.invocations.size());
    }

    @Test
    public void testCodeblockWithLanguageWraps() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("Usage: nrg ...", "");
        Generator g = makeGenerator("${widget:exec(cmd='x', codeblock='bash')}",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        String ls = System.lineSeparator();
        assertTrue(body.contains("```bash" + ls + "Usage: nrg ..." + ls + "```"), body);
    }

    @Test
    public void testCodeblockEmptyLanguageWrapsWithoutLang() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("plain", "");
        Generator g = makeGenerator("${widget:exec(cmd='x', codeblock='')}",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        String ls = System.lineSeparator();
        assertTrue(body.contains("```" + ls + "plain" + ls + "```"), body);
    }

    @Test
    public void testNonZeroExitLogsErrorAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.failed(2, "partial", "bad thing");
        Generator g = makeGenerator("${widget:exec(cmd='x')}",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertFalse(body.contains("partial"), body);
        String err = getErrAndClear();
        assertTrue(err.contains("exec widget: command failed with exit code 2"), err);
        assertTrue(err.contains("bad thing"), err);
    }

    @Test
    public void testTimeoutLogsWarningAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.timeout();
        Generator g = makeGenerator("${widget:exec(cmd='sleep 99', timeout='2')}",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertFalse(body.contains("sleep"), body);
        assertEquals(2L, runner.invocations.get(0).timeoutSeconds);
    }

    @Test
    public void testInvalidTimeoutLogsErrorAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        Generator g = makeGenerator("${widget:exec(cmd='x', timeout='abc')}",
                new ExecWidget(runner), true);

        g.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("exec widget: invalid 'timeout'"));
        assertEquals(0, runner.invocations.size());
    }

    @Test
    public void testZeroTimeoutRejected() {
        RecordingRunner runner = new RecordingRunner();
        Generator g = makeGenerator("${widget:exec(cmd='x', timeout='0')}",
                new ExecWidget(runner), true);

        g.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("exec widget: invalid 'timeout'"));
        assertEquals(0, runner.invocations.size());
    }

    @Test
    public void testDefaultTimeoutIs30Seconds() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("", "");
        Generator g = makeGenerator("${widget:exec(cmd='x')}", new ExecWidget(runner), true);

        g.getResult("en").getContent().toString();
        assertEquals(30L, runner.invocations.get(0).timeoutSeconds);
    }

    @Test
    public void testDefaultCwdIsSourceFileDirectory(@TempDir Path tempDir) {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("", "");
        File src = tempDir.resolve("README.src.md").toFile();
        Generator g = new Generator(src, "${widget:exec(cmd='x')}",
                Collections.singletonList(new ExecWidget(runner)));
        g.getConfig().setExecAllowed(true);

        g.getResult("en").getContent().toString();
        assertEquals(tempDir.toFile().getAbsoluteFile(), runner.invocations.get(0).cwd);
    }

    @Test
    public void testExplicitCwdRelativeToSourceFile(@TempDir Path tempDir) throws IOException {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("", "");
        File src = tempDir.resolve("README.src.md").toFile();
        Path sub = tempDir.resolve("sub");
        Files.createDirectory(sub);
        Generator g = new Generator(src, "${widget:exec(cmd='x', cwd='sub')}",
                Collections.singletonList(new ExecWidget(runner)));
        g.getConfig().setExecAllowed(true);

        g.getResult("en").getContent().toString();
        assertEquals(sub.toFile().getAbsoluteFile(), runner.invocations.get(0).cwd);
    }

    @Test
    public void testAbsoluteCwd(@TempDir Path tempDir) {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("", "");
        File src = new File("README.src.md");
        Generator g = new Generator(src,
                "${widget:exec(cmd='x', cwd='" + tempDir.toFile().getAbsolutePath().replace("\\", "\\\\") + "')}",
                Collections.singletonList(new ExecWidget(runner)));
        g.getConfig().setExecAllowed(true);

        g.getResult("en").getContent().toString();
        assertEquals(tempDir.toFile().getAbsoluteFile(), runner.invocations.get(0).cwd);
    }

    @Test
    public void testMissingCwdLogsErrorAndEmitsNothing() {
        RecordingRunner runner = new RecordingRunner();
        runner.next = ExecWidget.ExecResult.ok("", "");
        Generator g = makeGenerator("${widget:exec(cmd='x', cwd='no-such-directory-xyz')}",
                new ExecWidget(runner), true);

        g.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("exec widget: cwd not found"));
        assertEquals(0, runner.invocations.size());
    }

    @Test
    public void testIoExceptionFromRunnerLogsError() {
        RecordingRunner runner = new RecordingRunner();
        runner.nextIo = new IOException("boom: command not found");
        Generator g = makeGenerator("${widget:exec(cmd='nonesuch')}",
                new ExecWidget(runner), true);

        String body = g.getResult("en").getContent().toString();
        assertFalse(body.contains("nonesuch"), body);
        assertTrue(getErrAndClear().contains("exec widget: failed to run command"));
    }

    /** Runs {@link ExecProbeMain} as a real child process through the production runner. */
    private static String runProbe(String probeArgs, String timeout) {
        // 'java' is taken from PATH and the class path is passed as cwd + "-cp .", so no path
        // (which may contain spaces) has to survive the widget's whitespace splitting.
        String classes = new File(ExecProbeMain.class.getProtectionDomain().getCodeSource().getLocation().getPath())
                .getAbsolutePath().replace('\\', '/').replace("%20", " ");
        Generator g = makeGenerator("[${widget:exec(cmd='java -cp . " + ExecProbeMain.class.getName() + " "
                + probeArgs + "', cwd='" + classes + "', timeout='" + timeout + "')}]", new ExecWidget(), true);
        return g.getResult("en").getContent().toString();
    }

    @Test
    public void testOutputLargerThanThePipeBufferIsCaptured() {
        // The runner used to wait for the process before reading its output. A child that
        // writes more than the pipe buffer blocks until someone reads, so it never exited and
        // every such command "timed out" with nothing captured.
        String body = assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> runProbe("flood 3000000 0", "30"));

        int from = body.indexOf('[') + 1;
        int to = body.lastIndexOf(']');
        assertEquals(3_000_000, to - from, "captured stdout length");
        assertEquals("", getErrAndClear());
    }

    @Test
    public void testLargeStderrDoesNotBlockTheCommandEither() {
        String body = assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> runProbe("flood 10 3000000", "30"));

        assertTrue(body.contains("[oooooooooo]"), body);
    }

    @Test
    public void testCommandDoesNotInheritAnOpenStdin() {
        // Nothing will ever be typed into the child: its stdin must be at EOF, otherwise a
        // command that reads input waits until the timeout.
        String body = assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> runProbe("stdin", "30"));

        assertTrue(body.contains("[stdin-closed]"), body);
    }

    /**
     * Package-private test double that records invocations and returns a fixed result.
     */
    static final class RecordingRunner implements ExecWidget.CommandRunner {
        final List<Invocation> invocations = new ArrayList<>();
        ExecWidget.ExecResult next = ExecWidget.ExecResult.ok("", "");
        IOException nextIo;

        @Override
        public ExecWidget.ExecResult run(List<String> argv, File cwd, long timeoutSeconds) throws IOException {
            invocations.add(new Invocation(new ArrayList<>(argv), cwd, timeoutSeconds));
            if (nextIo != null) {
                throw nextIo;
            }
            return next;
        }

        static final class Invocation {
            final List<String> argv;
            final File cwd;
            final long timeoutSeconds;

            Invocation(List<String> argv, File cwd, long timeoutSeconds) {
                this.argv = argv;
                this.cwd = cwd;
                this.timeoutSeconds = timeoutSeconds;
            }
        }
    }
}
