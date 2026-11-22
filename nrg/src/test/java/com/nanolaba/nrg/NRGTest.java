package com.nanolaba.nrg;

import com.nanolaba.logging.LOG;
import com.nanolaba.logging.MemoryLogger;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.widgets.DefaultWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.DEBUG;
import static com.nanolaba.logging.LogEntry.LogEntryLevel.ERROR;
import static com.nanolaba.logging.LogEntry.LogEntryLevel.WARN;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The command line itself: what {@code main} does around {@link NRG#run(String...)}, the option
 * table, the class loader of {@code --classpath}, the modes the other CLI tests do not reach and
 * the messages, log entries and report entries that tell the user what happened.
 */
public class NRGTest extends DefaultNRGTest {

    private static final String MARKER = "nrg-test-classpath-marker.txt";

    @TempDir
    Path tempDir;

    private InputStream originalIn;

    @BeforeEach
    public void emptyStdin() {
        // run() hands System.in to --claude-hook; a test JVM's stdin must never be read.
        originalIn = System.in;
        System.setIn(new ByteArrayInputStream(new byte[0]));
        // The probe's fields are static: a value left by an earlier test (or, under pitest, by an
        // earlier mutant in the same JVM) must not pass for what this run saw.
        ContextLoaderProbe.seen = null;
        ContextLoaderProbe.markerVisible = false;
    }

    @AfterEach
    public void restoreStdin() {
        System.setIn(originalIn);
        LOG.init();
    }

    private Path write(String name, String body) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, body.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    private String read(String name) throws IOException {
        return new String(Files.readAllBytes(tempDir.resolve(name)), StandardCharsets.UTF_8);
    }

    /**
     * Runs {@code args} while {@code System.out} fails on the first bytes printed — the report of
     * {@code --format json} — so that the run ends with an exception rather than a status. NRG
     * prints through a UTF-8 stream of its own that hands {@code System.out} bytes, not lines.
     */
    private static void runWithFailingStdout(String... args) {
        PrintStream out = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()) {
            @Override
            public void write(byte[] buf, int off, int len) {
                throw new IllegalStateException("stdout is gone");
            }
        });
        try {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> NRG.run(args));
            assertEquals("stdout is gone", e.getMessage());
        } finally {
            System.setOut(out);
        }
    }

    /** A directory for {@code --classpath} holding a resource only a loader over it can find. */
    private Path classpathDirectory() throws IOException {
        Path dir = Files.createDirectories(tempDir.resolve("extra-classes"));
        Files.write(dir.resolve(MARKER), "x".getBytes(StandardCharsets.UTF_8));
        return dir;
    }

    // ------------------------------------------------------------------ main

    @Test
    public void testMainExitsWithTheStatusOfAFailedRunOnly() {
        List<Integer> exits = new ArrayList<>();
        NRG.exitOnFailure = true;
        try {
            NRG.launch(new String[]{"--version"}, name -> null, exits::add);
            assertEquals(Collections.emptyList(), exits, "a successful run does not exit");

            NRG.launch(new String[]{"--no-such-option"}, name -> null, exits::add);
            assertEquals(Collections.singletonList(NRG.EXIT_USAGE), exits);
        } finally {
            NRG.exitOnFailure = false;
        }
        NRG.launch(new String[]{"--no-such-option"}, name -> null, exits::add);
        assertEquals(Collections.singletonList(NRG.EXIT_USAGE), exits, "exitOnFailure=false keeps the JVM alive");
    }

    @Test
    public void testMainQuietsNanologAgentModeForTheHook() {
        String saved = System.getProperty("nanolog.agent");
        try {
            System.clearProperty("nanolog.agent");
            NRG.launch(new String[]{"--claude-hook", "pre-edit", "--no-such-option"}, name -> null, code -> {
            });
            assertEquals("off", System.getProperty("nanolog.agent"));
            assertTrue(getErrAndClear().contains("Incorrect command line arguments"));
        } finally {
            if (saved == null) {
                System.clearProperty("nanolog.agent");
            } else {
                System.setProperty("nanolog.agent", saved);
            }
        }
    }

    // ------------------------------------------------------------------ options

    @Test
    public void testHelpNamesTheArgumentOfEveryOption() {
        assertEquals(NRG.EXIT_OK, NRG.run("--help"));
        String help = getOutAndClear();

        String[] usages = {"-f,--file <filename>", "--charset <charset>", "--log-level <level>",
                "--language <code>", "--variant <name>", "--widgets <FQCN,FQCN,...>", "--classpath <path>",
                "--check-paths <pattern>", "--file-name-pattern <pattern>",
                "--default-language-file-name-pattern <pattern>", "--line-ending <auto|lf|crlf>", "--root <dir>",
                "--header-text <text>", "--format <text|json|github>", "--wrapper-version <version>",
                "--wrapper-sha256 <hex>", "--is-generated <file>", "--affected <file>", "--locate <file:line>",
                "--stale-translations <git-ref>", "--write <file>", "--env-allow <NAME,PREFIX_*>",
                "--claude-hook <pre-edit|post-edit>"};
        for (String usage : usages) {
            assertTrue(help.contains(usage), "help lists '" + usage + "':\n" + help);
        }
    }

    @Test
    public void testCheckPathsTakesSeveralValuesAfterOneFlag() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nHello\n");
        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        getErrAndClear();

        assertEquals(NRG.EXIT_OK, NRG.run("--check", "-f", src.toString(), "--check-paths", "**/README.md", "**/OTHER.md"));
        assertFalse(getErrAndClear().contains("mutually exclusive"), "OTHER.md is a pattern, not a source file");
    }

    @Test
    public void testAffectedTakesSeveralValuesAfterOneFlag() {
        File missing = tempDir.resolve("missing.src.md").toFile();

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--affected", "a.md", "b.md", "-f", missing.getPath()),
                "a template that cannot be loaded fails the query");
        String err = getErrAndClear();
        assertTrue(err.contains("Source file does not exist: " + missing.getPath()), err);
        assertFalse(err.contains("mutually exclusive"), "b.md is a changed file, not a source file: " + err);
    }

    @Test
    public void testSourceCharsetIsLoggedAtDebug() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_OK, NRG.run("--charset", "UTF-16"));
            log.assertLogged(DEBUG, "Source charset: UTF-16");
        }
    }

    @Test
    public void testVariantWithoutStdoutIsIgnoredWithAWarning() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.variants=a,b-->\nx\n");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_OK, NRG.run("--variant", "a", "-f", src.toString()));
            log.assertLogged(WARN, "--variant has no effect without --stdout; ignoring");
        }
        assertTrue(Files.exists(tempDir.resolve("README.a.md")) && Files.exists(tempDir.resolve("README.b.md")),
                "every variant is still written");
    }

    @Test
    public void testTraceLevelEnablesTrace() {
        NRG.applyLogLevel(NRG.LogLevel.TRACE);
        LOG.trace("trace marker 4711");

        assertTrue((getOutAndClear() + getErrAndClear()).contains("trace marker 4711"));
    }

    @Test
    public void testJsonFormatSendsTheLogToStderr() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nHello\n");

        assertEquals(NRG.EXIT_OK, NRG.run("--format", "json", "-f", src.toString()));
        String out = getOutAndClear();
        String err = getErrAndClear();
        assertTrue(err.contains("File \"README.md\" created"), err);
        assertTrue(out.trim().startsWith("{") && !out.contains("created"), "stdout holds the report alone: " + out);
        assertTrue(out.contains("\"mode\": \"generate\"") && out.contains("\"status\": \"written\""), out);

        assertEquals(NRG.EXIT_OK, NRG.run("--format", "json", "-f", src.toString()));
        out = getOutAndClear();
        assertTrue(out.contains("\"status\": \"unchanged\""), "a second run changes nothing: " + out);
    }

    // ------------------------------------------------------------------ --classpath

    /** Records the context class loader of the thread that instantiates it, as {@code --widgets} does. */
    public static class ContextLoaderProbe extends DefaultWidget {
        static ClassLoader seen;
        static boolean markerVisible;

        public ContextLoaderProbe() {
            seen = Thread.currentThread().getContextClassLoader();
            markerVisible = seen.getResource(MARKER) != null;
        }

        @Override
        public String getName() {
            return "contextLoaderProbe";
        }

        @Override
        public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
            return "";
        }
    }

    /** Leaves a class loader on the thread that refuses to close, as a careless library might. */
    public static class LoaderSwapper extends DefaultWidget {
        public LoaderSwapper() {
            Thread.currentThread().setContextClassLoader(
                    new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader()) {
                        @Override
                        public void close() throws IOException {
                            throw new IOException("still in use");
                        }
                    });
        }

        @Override
        public String getName() {
            return "loaderSwapper";
        }

        @Override
        public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
            return "";
        }
    }

    @Test
    public void testClasspathLoaderIsTheContextLoaderDuringTheRunAndClosedAfterIt() throws IOException {
        Path extra = classpathDirectory();
        ClassLoader before = Thread.currentThread().getContextClassLoader();

        assertEquals(NRG.EXIT_OK, NRG.run("--classpath", extra.toString(), "--widgets", ContextLoaderProbe.class.getName()));

        assertTrue(ContextLoaderProbe.markerVisible, "widgets are created under the --classpath loader");
        assertNotSame(before, ContextLoaderProbe.seen);
        assertNull(ContextLoaderProbe.seen.getResource(MARKER), "the loader is closed when the run ends");
        assertSame(before, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testClasspathLoaderIsClosedAfterAUsageError() throws IOException {
        Path extra = classpathDirectory();
        ClassLoader before = Thread.currentThread().getContextClassLoader();

        assertEquals(NRG.EXIT_USAGE, NRG.run("--classpath", extra.toString(),
                "--widgets", ContextLoaderProbe.class.getName(), "--stdout", "--check"));

        assertTrue(getErrAndClear().contains("--stdout and --check are mutually exclusive"));
        assertTrue(ContextLoaderProbe.markerVisible);
        assertNull(ContextLoaderProbe.seen.getResource(MARKER), "closed");
        assertSame(before, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testClasspathLoaderIsClosedWhenAnExceptionEscapesTheRun() throws IOException {
        Path extra = classpathDirectory();
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        ClassLoader before = Thread.currentThread().getContextClassLoader();

        runWithFailingStdout("--classpath", extra.toString(), "--widgets", ContextLoaderProbe.class.getName(),
                "--format", "json", "-f", src.toString());

        assertTrue(ContextLoaderProbe.markerVisible);
        assertNull(ContextLoaderProbe.seen.getResource(MARKER), "closed");
        assertSame(before, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testClasspathWithoutExistingEntriesInstallsNoLoader() {
        ClassLoader before = Thread.currentThread().getContextClassLoader();
        File missing = tempDir.resolve("missing").toFile();

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_OK, NRG.run("--classpath", missing.getPath(),
                    "--widgets", ContextLoaderProbe.class.getName()));
            log.assertLogged(WARN, "--classpath entry does not exist: " + missing.getAbsolutePath());
        }
        assertSame(before, ContextLoaderProbe.seen);
    }

    @Test
    public void testTheCallersOwnClassLoaderIsLeftOpen() throws IOException {
        Path extra = classpathDirectory();
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        ClassLoader before = Thread.currentThread().getContextClassLoader();
        URLClassLoader callers = new URLClassLoader(new URL[]{extra.toUri().toURL()}, before);
        Thread.currentThread().setContextClassLoader(callers);
        try {
            assertEquals(NRG.EXIT_OK, NRG.run("--version"));
            assertEquals(NRG.EXIT_USAGE, NRG.run("--no-such-option"));
            runWithFailingStdout("--format", "json", "-f", src.toString());

            assertNotNull(callers.getResource(MARKER), "the caller's loader is not NRG's to close");
            assertSame(callers, Thread.currentThread().getContextClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(before);
            callers.close();
        }
    }

    @Test
    public void testALoaderThatCannotBeClosedIsLoggedAndReplaced() {
        ClassLoader before = Thread.currentThread().getContextClassLoader();

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_OK, NRG.run("--widgets", LoaderSwapper.class.getName()));
            log.assertLogged(DEBUG, "Could not close the --classpath class loader: still in use");
        }
        assertSame(before, Thread.currentThread().getContextClassLoader());
    }

    // ------------------------------------------------------------------ --help syntax, --claude-hook, --init-wrapper

    @Test
    public void testHelpSyntaxPrintsTheCard() {
        assertEquals(NRG.EXIT_OK, NRG.run("--help", "syntax"));
        assertTrue(getOutAndClear().contains("# NRG syntax card"));
    }

    @Test
    public void testHelpSyntaxFailsWhenTheCardIsMissingOrUnreadable() {
        Supplier<InputStream> saved = NRG.syntaxCard;
        try {
            NRG.syntaxCard = () -> null;
            assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--help", "syntax"));
            assertTrue(getErrAndClear().contains("The syntax card is missing from this build of NRG"));

            NRG.syntaxCard = () -> new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("disk gone");
                }
            };
            try (MemoryLogger log = MemoryLogger.capture()) {
                assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--help", "syntax"));
                log.assertLogged(ERROR, "Cannot read the syntax card");
            }
        } finally {
            NRG.syntaxCard = saved;
        }
    }

    @Test
    public void testClaudeHookReturnsTheStatusOfTheHook() {
        String event = "{\"cwd\": \"" + tempDir.toString().replace('\\', '/') + "\", "
                + "\"tool_input\": {\"file_path\": \"README.md\"}}";
        System.setIn(new ByteArrayInputStream(event.getBytes(StandardCharsets.UTF_8)));

        assertEquals(NRG.EXIT_USAGE, NRG.run("--claude-hook", "sideways"));
        assertTrue(getErrAndClear().contains("--claude-hook expects pre-edit or post-edit, got 'sideways'"));
    }

    @Test
    public void testInitWrapperReturnsTheStatusOfTheInstaller() {
        assertEquals(NRG.EXIT_USAGE, NRG.run("--init-wrapper", "--wrapper-version", "dev"));
        assertTrue(getErrAndClear().contains("'dev' is not a release version"));
    }

    // ------------------------------------------------------------------ --agent-rules and the queries

    @Test
    public void testAgentRulesRejectsMixedSources() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");

        assertEquals(NRG.EXIT_USAGE, NRG.run("--agent-rules", "-f", src.toString(), src.toString()));
        String err = getErrAndClear();
        assertTrue(err.contains("-f and positional file arguments are mutually exclusive"), err);
        assertTrue(err.contains("To view help, run with the -h option"), err);
    }

    @Test
    public void testAgentRulesCheckReportsAnOutdatedBlock() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        Path rules = write("AGENTS.md", "# Rules\n");

        assertEquals(NRG.EXIT_DRIFT, NRG.run("--agent-rules", "--check", "--write", rules.toString(), "-f", src.toString()));
        assertTrue(getErrAndClear().contains("Update with: nrg --agent-rules --write"));
    }

    @Test
    public void testAgentRulesThatCannotBeWrittenFailTheRun() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        write("blocker", "a file where a directory should be");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--agent-rules",
                    "--write", tempDir.resolve("blocker/AGENTS.md").toString(), "-f", src.toString()));
            log.assertLogged(ERROR, "Cannot write the agent rules");
        }
    }

    @Test
    public void testManifestFailsWhenATemplateCannotBeLoaded() {
        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--manifest", "-f", tempDir.resolve("missing.src.md").toString()));
        assertTrue(getErrAndClear().contains("Source file does not exist"));
    }

    // ------------------------------------------------------------------ sources

    @Test
    public void testValidateOfAMissingSourceIsAUsageError() {
        File missing = tempDir.resolve("missing.src.md").toFile();

        assertEquals(NRG.EXIT_USAGE, NRG.run("--validate", "-f", missing.getPath()));
        assertTrue(getErrAndClear().contains("Source file does not exist: " + missing.getAbsolutePath()));

        assertEquals(NRG.EXIT_USAGE, NRG.run("--validate", "--format", "json", "-f", missing.getPath()));
        String json = getOutAndClear();
        assertTrue(json.contains("\"mode\": \"validate\"") && json.contains("\"code\": \"source-not-found\""), json);
    }

    // ------------------------------------------------------------------ --stdout

    @Test
    public void testStdoutLanguageFilterOverSeveralFilesSeparatesThem() throws IOException {
        Path a = write("a.src.md", "<!--@nrg.languages=en-->\nA\n");
        Path b = write("b.src.md", "<!--@nrg.languages=en,ru-->\nB<!--en-->\nБ<!--ru-->\n");

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "--no-header", "--language", "en", a.toString(), b.toString()));
        assertEquals("=== a.md ===" + RN + "A" + RN + RN + "=== b.md ===" + RN + "B" + RN + RN, getOutAndClear());
    }

    @Test
    public void testStdoutSeparatesTheLanguagesOfOneFile() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nHello<!--en-->\nПривет<!--ru-->\n");

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "--no-header", "-f", src.toString()));
        assertEquals("=== README.md ===" + RN + "Hello" + RN + RN + "=== README.ru.md ===" + RN + "Привет" + RN + RN,
                getOutAndClear());
    }

    @Test
    public void testStdoutCountsATemplateThatCannotBeReadAsOneOutput() throws IOException {
        Path a = write("a.src.md", "<!--@nrg.languages=en-->\nA\n");
        Path broken = write("broken.src.md", "<!--@nrg.languages=en-->\n<!--@nrg.defaultLanguage=ru-->\nB\n");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--stdout", "--no-header", a.toString(), broken.toString()));
            log.assertLogged(DEBUG, "countTotalOutputs probe failed for " + broken.toFile());
            log.assertLogged(ERROR, "Generation failed: " + broken.toFile().getAbsolutePath());
        }
        assertEquals("=== a.md ===" + RN + "A" + RN + RN, getOutAndClear(),
                "two sources, so the readable one gets its separator");
    }

    // ------------------------------------------------------------------ variants

    @Test
    public void testStdoutVariantThatIsNotDeclaredIsSkipped() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.variants=a,b-->\nx\n");

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "--variant", "c", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("WARN: --variant 'c' not declared in README.src.md (available: [a, b]); skipping"));
        assertEquals("", getOutAndClear());
    }

    @Test
    public void testStdoutSeparatesVariantsOnlyWhenThereAreSeveral() throws IOException {
        Path two = write("two.src.md", "<!--@nrg.variants=a,b-->\n<!--@nrg.noHeader=true-->\nx\n");
        Path one = write("one.src.md", "<!--@nrg.variants=only-->\n<!--@nrg.noHeader=true-->\ny\n");

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "-f", two.toString()));
        assertEquals("=== two.a.md ===" + RN + "x" + RN + RN + "=== two.b.md ===" + RN + "x" + RN + RN, getOutAndClear());

        assertEquals(NRG.EXIT_OK, NRG.run("--stdout", "-f", one.toString()));
        assertEquals("y" + RN, getOutAndClear());
    }

    @Test
    public void testCheckOfVariantsReportsMissingOutputs() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.variants=a,b-->\nx\n");

        assertEquals(NRG.EXIT_DRIFT, NRG.run("--check", "-f", src.toString()));
        assertTrue(getErrAndClear().contains("Missing file: README.a.md"));
    }

    // ------------------------------------------------------------------ generation failures

    /** Fails while rendering, with its parameter as the message, or with none. */
    public static class FailingWidget extends DefaultWidget {
        @Override
        public String getName() {
            return "failing";
        }

        @Override
        public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
            String message = widgetTag.getParameters();
            throw message == null || message.isEmpty() ? new IllegalStateException() : new IllegalStateException(message);
        }
    }

    @Test
    public void testAFailingWidgetIsReportedWithItsMessageOrItsType() throws IOException {
        Path withMessage = write("a.src.md", "<!--@nrg.languages=en-->\n${widget:failing(boom)}\n");
        Path withoutMessage = write("b.src.md", "<!--@nrg.languages=en-->\n${widget:failing}\n");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json", "--widgets", FailingWidget.class.getName(),
                    withMessage.toString(), withoutMessage.toString()));
            log.assertLogged(ERROR, "Generation failed: " + withMessage.toFile().getAbsolutePath());
            log.assertLogged(ERROR, "Generation failed: " + withoutMessage.toFile().getAbsolutePath());
        }
        String json = getOutAndClear();
        assertTrue(json.contains("\"code\": \"generation-failed\""), json);
        assertTrue(json.contains("\"message\": \"boom\""), json);
        assertTrue(json.contains("\"message\": \"java.lang.IllegalStateException\""), json);
    }

    @Test
    public void testAnOutputThatCannotBeWrittenFailsTheGeneration() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        write("blocker", "a file where a directory should be");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json",
                    "--file-name-pattern", "blocker/<base>.md", "-f", src.toString()));
            log.assertLogged(ERROR, "Generation failed: " + src.toFile().getAbsolutePath());
        }
        assertTrue(getOutAndClear().contains("\"code\": \"generation-failed\""));
    }

    @Test
    public void testAnOutputDirectoryThatCannotBeCreatedFailsTheRun() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en,ru-->\nx\n");
        write("blocker", "a file where a directory should be");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json",
                    "--default-language-file-name-pattern", "blocker/sub/<base>.md", "-f", src.toString()));
            log.assertLogged(ERROR, "Cannot create directory for output file");
            assertFalse(log.contains(ERROR, "Generation failed"), "the other outputs are still written: " + log.getMessages());
        }
        String json = getOutAndClear();
        assertTrue(json.contains("\"ok\": false"), json);
        assertTrue(json.contains("\"exitCode\": 2"), json);
        assertTrue(json.contains("\"message\": \"cannot create directory "), json);
        assertTrue(Files.isRegularFile(tempDir.resolve("README.ru.md")), "the other language is still written");
    }

    @Test
    public void testEachWrittenOutputIsLoggedAtDebug() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
            log.assertLogged(DEBUG, "Generating file for language \"en\" - " + tempDir.resolve("README.md").toFile().getAbsolutePath());
        }
    }

    @Test
    public void testFreezeBlockErrorsStopTheGeneration() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\n"
                + "<!--nrg.freeze id=\"x\" source-lang=\"de\"-->\nkept\n<!--/nrg.freeze-->\n");
        String expected = "source-lang 'de' is not declared in nrg.languages";

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("-f", src.toString()));
        assertTrue(getErrAndClear().contains(expected));
        assertFalse(Files.exists(tempDir.resolve("README.md")));

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json", "-f", src.toString()));
        assertTrue(getOutAndClear().contains("\"code\": \"freeze-block\""));
        assertFalse(getErrAndClear().contains(expected), "the report carries it instead of stderr");
    }

    // ------------------------------------------------------------------ remote imports

    @Test
    public void testOfflineAppliesOnlyWhenAsked() throws IOException {
        Path src = write("README.src.md", "${widget:import(url='https://example.com/x.md', run-generator='false')}\n");

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--format", "json", "-f", src.toString()));
        assertTrue(getOutAndClear().contains("remote URLs disabled"));

        assertEquals(NRG.EXIT_TEMPLATE_ERROR, NRG.run("--offline", "--format", "json", "-f", src.toString()));
        assertTrue(getOutAndClear().contains("disabled by --offline / --sandbox"));
    }

    // ------------------------------------------------------------------ --check

    @Test
    public void testCheckReportsUpToDateOutputs() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        getOutAndClear();

        assertEquals(NRG.EXIT_OK, NRG.run("--check", "--format", "json", "-f", src.toString()));
        String json = getOutAndClear();
        assertTrue(json.contains("\"mode\": \"check\""), json);
        assertTrue(json.contains("\"output\": \"" + relative(tempDir.resolve("README.md")) + "\", "
                + "\"language\": \"en\", \"status\": \"ok\""), json);
    }

    @Test
    public void testCheckPathsThatMatchNothingAreAWarningInTheReport() throws IOException {
        Path src = write("README.src.md", "<!--@nrg.languages=en-->\nx\n");
        assertEquals(NRG.EXIT_OK, NRG.run("-f", src.toString()));
        getOutAndClear();

        assertEquals(NRG.EXIT_OK, NRG.run("--check", "--format", "json", "--check-paths", "nothing.md", "-f", src.toString()));
        String json = getOutAndClear();
        assertTrue(json.contains("\"severity\": \"warning\", \"code\": \"check-paths-unmatched\""), json);
    }

    private static String relative(Path p) {
        Path base = new File(".").getAbsoluteFile().toPath().normalize();
        Path absolute = p.toAbsolutePath().normalize();
        return (absolute.startsWith(base) ? base.relativize(absolute) : absolute).toString().replace(File.separatorChar, '/');
    }

    // ------------------------------------------------------------------ unifiedDiff

    private static String diff(String... lines) {
        StringBuilder sb = new StringBuilder("--- f.md (on disk)" + RN + "+++ f.md (generated)" + RN);
        for (String line : lines) {
            sb.append(line).append(RN);
        }
        return sb.toString();
    }

    @Test
    public void testUnifiedDiffStopsTheCommonPrefixAtTheShorterSide() {
        assertEquals(diff("@@ line 3 @@", " x", " y", "+z"), NRG.unifiedDiff("x\ny", "x\ny\nz", "f.md"));
    }

    @Test
    public void testUnifiedDiffDoesNotLetTheSuffixOverlapThePrefix() {
        assertEquals(diff("@@ line 2 @@", " x", "-x"), NRG.unifiedDiff("x\nx", "x", "f.md"));
        assertEquals(diff("@@ line 2 @@", " x", "+x"), NRG.unifiedDiff("x", "x\nx", "f.md"));
    }

    @Test
    public void testUnifiedDiffStopsTheCommonSuffixAtTheStart() {
        assertEquals(diff("@@ line 1 @@", "+x", " y", " z"), NRG.unifiedDiff("y\nz", "x\ny\nz", "f.md"));
    }

    @Test
    public void testUnifiedDiffShowsAtMostThreeLinesOfContext() {
        assertEquals(diff("@@ line 1 @@", "-x", "+y", " 1", " 2", " 3"),
                NRG.unifiedDiff("x\n1\n2\n3\n4", "y\n1\n2\n3\n4", "f.md"));
        assertEquals(diff("@@ line 5 @@", " 2", " 3", " 4", "-x", "+y"),
                NRG.unifiedDiff("1\n2\n3\n4\nx", "1\n2\n3\n4\ny", "f.md"));
    }

    @Test
    public void testUnifiedDiffAgainstAnEmptyFile() {
        assertEquals(diff("@@ line 1 @@", "+x"), NRG.unifiedDiff("", "x", "f.md"));
        assertEquals(diff("@@ line 1 @@", "-x"), NRG.unifiedDiff("x", null, "f.md"));
    }

    // ------------------------------------------------------------------ output names

    @Test
    public void testDeprecatedReadmeFileUsesTheDefaultNames() {
        File source = new File(tempDir.toFile(), "README.src.md");

        assertEquals(new File(tempDir.toFile(), "README.md"), NRG.getReadmeFile("en", source, "en"));
        assertEquals(new File(tempDir.toFile(), "README.ru.md"), NRG.getReadmeFile("ru", source, "en"));
    }
}
