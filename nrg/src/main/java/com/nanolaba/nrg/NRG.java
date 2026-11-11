package com.nanolaba.nrg;

import com.nanolaba.logging.ConsoleLogger;
import com.nanolaba.logging.LOG;
import com.nanolaba.logging.LogEntry;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.LineEndings;
import com.nanolaba.nrg.core.NRGConstants;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.core.OutputFileNameResolver;
import com.nanolaba.nrg.core.OutputFileNameValidator;
import com.nanolaba.nrg.core.PathPatternMatcher;
import com.nanolaba.nrg.core.SourceFileResolver;
import com.nanolaba.nrg.core.Validator;
import com.nanolaba.nrg.core.freeze.FreezeValidator;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.sugar.Code;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.nanolaba.nrg.core.NRGConstants.DEFAULT_CHARSET;

/**
 * CLI entry point for Nanolaba Readme Generator.
 *
 * <p>Parses command-line options ({@code -f}, {@code --charset}, {@code --version},
 * {@code --check}, {@code --validate}, etc.), wires up {@link Generator} for the requested
 * source file, and writes one output file per language declared via {@code nrg.languages}.
 * Library users can also call {@link #addWidget(NRGWidget)} before {@link #main(String...)}
 * to register custom widgets globally, or invoke {@link #run(String...)} directly to capture
 * the exit code without the {@link System#exit(int)} call.
 *
 * <p>Multiple source files may be passed positionally; {@code -f} remains a single-file alias
 * and is mutually exclusive with positional arguments. Patterns use {@code glob:} syntax
 * (see {@link com.nanolaba.nrg.core.SourceFileResolver}). With {@code --fail-fast}, the batch
 * stops at the first non-zero result; the default aggregates so every file's diagnostics
 * surface in a single run.
 */
public class NRG {

    public static final String ENV_LOG_LEVEL = "NRG_LOG_LEVEL";

    /** Exit status: everything is fine. */
    public static final int EXIT_OK = 0;
    /** Exit status of {@code --check}: a generated file differs from the template or is missing. */
    public static final int EXIT_DRIFT = 1;
    /** Exit status: a template is invalid ({@code --validate}) or generation failed. */
    public static final int EXIT_TEMPLATE_ERROR = 2;
    /** Exit status: the command line is wrong — unknown or conflicting options, no source files. */
    public static final int EXIT_USAGE = 64;

    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR;

        public static LogLevel parse(String raw) {
            if (raw == null) {
                return null;
            }
            try {
                return LogLevel.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static List<NRGWidget> additionalWidgets = new ArrayList<>();

    public static boolean exitOnFailure = true;

    public static void main(String... args) {
        int code = run(args);
        if (code != 0 && exitOnFailure) {
            System.exit(code);
        }
    }

    public static int run(String... args) {
        Thread thread = Thread.currentThread();
        ClassLoader callerClassLoader = thread.getContextClassLoader();
        try {
            return parseCommandLine(args);
        } catch (ParseException e) {
            System.err.println("Incorrect command line arguments: " + e.getMessage());
            System.err.println("To view help, run with the -h option");
            return EXIT_USAGE;
        } finally {
            // --classpath installs its own loader for the run. run() is also called in-process
            // (the Maven plugin, library users, tests), so the caller's loader is put back and
            // ours is closed instead of staying behind with its jars open.
            ClassLoader used = thread.getContextClassLoader();
            thread.setContextClassLoader(callerClassLoader);
            if (used != callerClassLoader && used instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) used).close();
                } catch (IOException e) {
                    LOG.debug("Could not close the --classpath class loader: {}", e.getMessage());
                }
            }
        }
    }

    private static int parseCommandLine(String[] args) throws ParseException {
        Options options = new Options();

        Option version = new Option(null, "version", false, "print the version information and exit");
        options.addOption(version);
        Option help = new Option("h", "help", false, "print this message and exit");
        options.addOption(help);
        Option file = new Option("f", "file", true, "source file");
        file.setArgName("filename");
        file.setConverter(File::new);
        options.addOption(file);
        Option charset = new Option(null, "charset", true, "source file encoding (default: " + DEFAULT_CHARSET.displayName() + ")");
        charset.setArgName("charset");
        charset.setConverter(Charset::forName);
        options.addOption(charset);
        Option logLevel = new Option(null, "log-level", true,
                "log verbosity: trace|debug|info|warn|error (default: info, overrides $" + ENV_LOG_LEVEL + ")");
        logLevel.setArgName("level");
        options.addOption(logLevel);
        Option stdout = new Option(null, "stdout", false,
                "print generated output to stdout instead of writing files");
        options.addOption(stdout);
        Option language = new Option(null, "language", true,
                "when combined with --stdout, print only the given language variant");
        language.setArgName("code");
        options.addOption(language);
        Option widgets = new Option(null, "widgets", true,
                "comma-separated fully-qualified class names of custom NRGWidget implementations");
        widgets.setArgName("FQCN,FQCN,...");
        options.addOption(widgets);
        Option classpath = new Option(null, "classpath", true,
                "additional JARs or directories (" + File.pathSeparator + "-separated) to resolve --widgets classes");
        classpath.setArgName("path");
        options.addOption(classpath);
        Option check = new Option(null, "check", false,
                "verify generated output matches files on disk; exit 1 and print a diff when they differ");
        options.addOption(check);
        Option checkPaths = Option.builder()
                .longOpt("check-paths")
                .hasArg()
                .argName("pattern")
                .desc("limit --check to outputs matching these glob patterns " +
                        "(repeatable, cwd-relative); without it all generated outputs are checked")
                .build();
        checkPaths.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(checkPaths);
        Option allowExec = new Option(null, "allow-exec", false,
                "allow the 'exec' widget to run external commands (disabled by default)");
        options.addOption(allowExec);
        Option validate = new Option(null, "validate", false,
                "validate the source template and imports without generating output; exit 2 on errors");
        options.addOption(validate);
        Option fileNamePattern = new Option(null, "file-name-pattern", true,
                "output filename pattern with placeholders <base>, <lang>, <LANG> (overrides nrg.fileNamePattern)");
        fileNamePattern.setArgName("pattern");
        options.addOption(fileNamePattern);
        Option defaultLangFileNamePattern = new Option(null, "default-language-file-name-pattern", true,
                "output filename pattern for the default language (overrides nrg.defaultLanguageFileNamePattern)");
        defaultLangFileNamePattern.setArgName("pattern");
        options.addOption(defaultLangFileNamePattern);
        Option failFast = new Option(null, "fail-fast", false,
                "stop on the first non-zero result instead of aggregating across all files");
        options.addOption(failFast);
        Option lineEnding = new Option(null, "line-ending", true,
                "output line ending: auto|lf|crlf (default: auto — preserve existing file's convention, " +
                        "fall back to platform default for new files; --check ignores LE-only differences in auto mode)");
        lineEnding.setArgName("auto|lf|crlf");
        options.addOption(lineEnding);
        Option noHeader = new Option(null, "no-header", false,
                "suppress the auto-generated head comment lines at the top of every output file");
        options.addOption(noHeader);
        Option root = new Option(null, "root", true,
                "directory the templates are confined to: imports, descriptors and output files must stay inside it " +
                        "(default: the nearest ancestor of the source file that contains .git, else the source file's directory)");
        root.setArgName("dir");
        options.addOption(root);
        Option headerText = new Option(null, "header-text", true,
                "replace the default head comment with this text (\\n, \\r, \\t, \\\\ are interpreted)");
        headerText.setArgName("text");
        options.addOption(headerText);
        Option format = new Option(null, "format", true,
                "output style: text (default), json (one JSON report on stdout, logs on stderr) " +
                        "or github (workflow annotations on stdout)");
        format.setArgName("text|json|github");
        options.addOption(format);
        Option initWrapper = new Option(null, "init-wrapper", false,
                "write nrgw, nrgw.cmd and .nrg/wrapper.properties into the current directory and exit");
        options.addOption(initWrapper);
        Option wrapperVersion = new Option(null, "wrapper-version", true,
                "with --init-wrapper: the NRG release to pin (default: this NRG's version)");
        wrapperVersion.setArgName("version");
        options.addOption(wrapperVersion);
        Option wrapperSha256 = new Option(null, "wrapper-sha256", true,
                "with --init-wrapper: sha256 of the release zip (default: the checksum published with the release)");
        wrapperSha256.setArgName("hex");
        options.addOption(wrapperSha256);
        Option manifest = new Option(null, "manifest", false,
                "print the outputs and inputs of each template (all root *.src.md files when none is given)");
        options.addOption(manifest);
        Option isGenerated = new Option(null, "is-generated", true,
                "exit 0 and print the template when the file is generated by NRG, exit 1 otherwise");
        isGenerated.setArgName("file");
        options.addOption(isGenerated);
        Option affected = Option.builder()
                .longOpt("affected")
                .hasArg()
                .argName("file")
                .desc("print the templates that read any of these files (repeatable)")
                .build();
        affected.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(affected);
        Option locate = new Option(null, "locate", true,
                "print the template line that produces a line of a generated file (FILE:LINE or FILE:FIRST-LAST)");
        locate.setArgName("file:line");
        options.addOption(locate);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        RunReport.Format outputFormat = RunReport.Format.parse(cmd.getOptionValue(format));
        if (outputFormat == null) {
            throw new ParseException("Invalid format: '" + cmd.getOptionValue(format) + "' (expected text|json|github)");
        }
        applyLogLevel(resolveLogLevel(cmd.getOptionValue(logLevel)), outputFormat == RunReport.Format.JSON);

        if (cmd.hasOption(version)) {
            printVersion();
            return 0;
        }
        if (cmd.hasOption(help)) {
            printHelp(options);
            return 0;
        }
        if (cmd.hasOption(initWrapper)) {
            try {
                return new WrapperInstaller().install(new File("."),
                        cmd.getOptionValue(wrapperVersion), cmd.getOptionValue(wrapperSha256));
            } catch (IOException e) {
                LOG.error(e, () -> "Cannot write the wrapper files");
                return EXIT_TEMPLATE_ERROR;
            }
        }
        if (cmd.hasOption(wrapperVersion) || cmd.hasOption(wrapperSha256)) {
            throw new ParseException("--wrapper-version and --wrapper-sha256 require --init-wrapper");
        }

        Charset sourceCharset = cmd.getParsedOptionValue(charset, DEFAULT_CHARSET);
        LOG.debug("Source charset: {}", sourceCharset.displayName());

        ClassLoader extraCl = buildExtraClassLoader(cmd.getOptionValue(classpath));
        if (extraCl != null) {
            Thread.currentThread().setContextClassLoader(extraCl);
        }

        List<NRGWidget> cliWidgets = NRGUtil.loadWidgets(cmd.getOptionValue(widgets), extraCl);

        boolean toStdout = cmd.hasOption(stdout);
        boolean checkMode = cmd.hasOption(check);
        boolean validateMode = cmd.hasOption(validate);
        if (toStdout && checkMode) {
            throw new ParseException("--stdout and --check are mutually exclusive");
        }
        if (validateMode && (toStdout || checkMode)) {
            throw new ParseException("--validate is mutually exclusive with --stdout and --check");
        }
        if (toStdout && outputFormat != RunReport.Format.TEXT) {
            throw new ParseException("--format " + cmd.getOptionValue(format) + " cannot be combined with --stdout");
        }
        String[] checkPathsValues = cmd.getOptionValues(checkPaths);
        List<String> checkPathsList = checkPathsValues == null
                ? Collections.<String>emptyList()
                : Arrays.asList(checkPathsValues);
        boolean hasCheckPathsFilter = false;
        for (String p : checkPathsList) {
            if (p != null && !p.isEmpty()) {
                hasCheckPathsFilter = true;
                break;
            }
        }
        if (hasCheckPathsFilter && !checkMode) {
            throw new ParseException("--check-paths requires --check");
        }
        PathPatternMatcher checkPathFilter = PathPatternMatcher.compile(checkPathsList);
        String langValue = cmd.getOptionValue(language);
        if (langValue != null && !toStdout) {
            LOG.warn("--language has no effect without --stdout; ignoring");
            langValue = null;
        }
        String fileNamePatternValue = cmd.getOptionValue(fileNamePattern);
        String defaultLangFileNamePatternValue = cmd.getOptionValue(defaultLangFileNamePattern);
        boolean noHeaderFlag = cmd.hasOption(noHeader);
        String headerTextValue = cmd.getOptionValue(headerText);
        if (noHeaderFlag && headerTextValue != null) {
            throw new ParseException("--no-header and --header-text are mutually exclusive");
        }
        File rootDir = null;
        if (cmd.hasOption(root)) {
            rootDir = new File(cmd.getOptionValue(root));
            if (!rootDir.isDirectory()) {
                throw new ParseException("--root is not a directory: " + rootDir.getAbsolutePath());
            }
        }
        LineEndings.Mode lineEndingMode;
        try {
            lineEndingMode = LineEndings.Mode.parse(cmd.getOptionValue(lineEnding));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage());
        }

        int queryModes = (cmd.hasOption(manifest) ? 1 : 0) + (cmd.hasOption(isGenerated) ? 1 : 0)
                + (cmd.hasOption(affected) ? 1 : 0) + (cmd.hasOption(locate) ? 1 : 0);
        if (queryModes > 1) {
            throw new ParseException("--manifest, --is-generated, --affected and --locate are mutually exclusive");
        }
        if (queryModes == 1 && (toStdout || checkMode || validateMode)) {
            throw new ParseException("--manifest, --is-generated, --affected and --locate cannot be combined with "
                    + "--stdout, --check or --validate");
        }

        List<File> sources = collectSourceFiles(cmd, file);
        if (sources == null) {
            return EXIT_USAGE;
        }
        if (queryModes == 1) {
            List<NRGWidget> widgetsForQuery = new ArrayList<>(cliWidgets == null ? Collections.emptyList() : cliWidgets);
            widgetsForQuery.addAll(additionalWidgets);
            try {
                ManifestCommand query = new ManifestCommand(sourceCharset, widgetsForQuery).load(sources);
                if (cmd.hasOption(locate)) {
                    return query.printLocate(cmd.getOptionValue(locate), outputFormat);
                }
                if (cmd.hasOption(isGenerated)) {
                    return query.printIsGenerated(new File(cmd.getOptionValue(isGenerated)), outputFormat);
                }
                if (cmd.hasOption(affected)) {
                    List<File> changed = new ArrayList<>();
                    for (String f : cmd.getOptionValues(affected)) {
                        changed.add(new File(f));
                    }
                    return query.printAffected(changed, outputFormat);
                }
                return query.printManifest(outputFormat);
            } catch (IOException e) {
                LOG.error(e, () -> "Cannot list the templates");
                return EXIT_TEMPLATE_ERROR;
            }
        }
        if (sources.isEmpty()) {
            return EXIT_OK;
        }

        RunReport report = new RunReport(validateMode ? "validate" : checkMode ? "check" : "generate",
                new File("."));
        boolean failFastFlag = cmd.hasOption(failFast);
        int totalOutputs = 0;
        if (toStdout) {
            totalOutputs = countTotalOutputs(sources, sourceCharset, cliWidgets, langValue);
        }
        int aggregateCode = EXIT_OK;
        MatchTracker checkMatchTracker = new MatchTracker();
        for (File sourceFile : sources) {
            int code;
            if (validateMode) {
                code = runValidation(sourceFile, rootDir, cliWidgets, report, outputFormat);
            } else {
                code = generate(sourceFile, sourceCharset, toStdout, langValue, cliWidgets,
                        checkMode, cmd.hasOption(allowExec),
                        fileNamePatternValue, defaultLangFileNamePatternValue,
                        totalOutputs > 1, lineEndingMode,
                        noHeaderFlag, headerTextValue,
                        checkPathFilter, checkMatchTracker, rootDir, report, outputFormat);
            }
            // Codes are ordered by severity, so the batch reports the worst one.
            aggregateCode = Math.max(aggregateCode, code);
            if (code != EXIT_OK && failFastFlag) {
                break;
            }
        }
        if (checkMode && hasCheckPathsFilter && !checkMatchTracker.anyMatched()) {
            String message = "--check-paths matched no generated outputs (patterns: " + checkPathFilter.getPatterns() + ")";
            if (outputFormat == RunReport.Format.TEXT) {
                System.err.println("WARN: " + message);
            } else {
                report.addDiagnostic(new Validator.Diagnostic(null, 0, Validator.Severity.WARNING,
                        "check-paths-unmatched", message, "fix the patterns; nothing was checked"));
            }
        }
        if (outputFormat == RunReport.Format.JSON) {
            System.out.println(report.toJson(aggregateCode));
        } else if (outputFormat == RunReport.Format.GITHUB) {
            report.toGithubAnnotations().forEach(System.out::println);
        }
        return aggregateCode;
    }

    /**
     * Combines the legacy single {@code -f} flag and the new positional source-file arguments
     * into one ordered, deduplicated list. Rejects mixing the two and rejects an overall
     * zero-match outcome. Returns {@code null} when an error has already been logged so the
     * caller can short-circuit with exit code 1; an empty list means "no -f and no positional
     * arguments — the CLI was invoked with only flags, exit cleanly".
     *
     * <p>The {@code -f} alias is treated as a literal single-file reference (no glob expansion),
     * so the historical {@code Source file does not exist:} error from {@link #generate} still
     * surfaces unchanged when a missing path is passed via {@code -f}.
     */
    private static List<File> collectSourceFiles(CommandLine cmd, Option fileOpt) throws ParseException {
        // Declared 'throws ParseException' because cmd.getParsedOptionValue(fileOpt) is declared
        // to throw it; in practice the value was already validated by the parser above.
        boolean hasFlag = cmd.hasOption(fileOpt);
        String[] positional = cmd.getArgs();
        boolean hasPositional = positional != null && positional.length > 0;
        if (hasFlag && hasPositional) {
            System.err.println("Incorrect command line arguments: -f and positional file arguments are mutually exclusive");
            System.err.println("To view help, run with the -h option");
            return null;
        }
        if (hasFlag) {
            // Preserve legacy semantics: -f points at exactly one file, no glob expansion.
            // The generate()/runValidation() helpers already emit "Source file does not exist:"
            // for missing paths, so we keep that diagnostic verbatim by skipping the resolver.
            File singleFile = (File) cmd.getParsedOptionValue(fileOpt);
            return Collections.singletonList(singleFile);
        }
        if (!hasPositional) {
            return Collections.emptyList();
        }
        List<String> patterns = new ArrayList<>();
        for (String p : positional) {
            if (p != null && !p.isEmpty()) {
                patterns.add(p);
            }
        }
        if (patterns.isEmpty()) {
            return Collections.emptyList();
        }
        SourceFileResolver.Result r = SourceFileResolver.resolve(patterns);
        // LOG.warn writes to stdout in the in-house logger; routing per-pattern empty warnings
        // straight to stderr keeps them close to the eventual "No source files matched" error
        // and matches what users expect from a warning of this kind.
        for (String p : r.getEmptyPatterns()) {
            System.err.println("WARN: No source files matched pattern: " + p);
        }
        if (r.getFiles().isEmpty()) {
            System.err.println("No source files matched any of the supplied patterns");
            return null;
        }
        return r.getFiles();
    }

    /**
     * Pre-walks every source file's {@link GeneratorConfig} once to count the total number of
     * (file, language) outputs the batch will produce. Used by {@code --stdout} to decide whether
     * per-file separator headers are needed when the batch is reduced to a single language each
     * (e.g. two single-language files = 2 outputs => headers).
     *
     * <p>{@code languageFilter}, when non-null, restricts the count to files that actually
     * declare it — matching the runtime behaviour where filtered-out files are skipped with a
     * warning. This keeps the historical "no header for single-language single-file output"
     * behaviour intact when {@code -f --language X} is used on a multi-language source.
     *
     * <p>Cheap: {@link Generator}'s constructor only reads config; full rendering is deferred to
     * {@link Generator#getResult(String)} and is performed exactly once per language.
     */
    private static int countTotalOutputs(List<File> sources, Charset charset, List<NRGWidget> cliWidgets,
                                         String languageFilter) {
        int total = 0;
        for (File f : sources) {
            try {
                List<NRGWidget> widgets = new ArrayList<>();
                if (cliWidgets != null) {
                    widgets.addAll(cliWidgets);
                }
                widgets.addAll(additionalWidgets);
                Generator probe = new Generator(f, charset, widgets);
                List<String> declared = probe.getConfig().getLanguages();
                if (languageFilter != null) {
                    if (declared.contains(languageFilter)) {
                        total += 1;
                    }
                } else {
                    total += declared.size();
                }
            } catch (Exception e) {
                // Counting is best-effort; if the source can't even be parsed, assume 1 output.
                LOG.debug("countTotalOutputs probe failed for {}: {}", f, e.getMessage());
                total += 1;
            }
        }
        return total;
    }

    private static int runValidation(File sourceFile, File rootDir, List<NRGWidget> cliWidgets,
                                     RunReport report, RunReport.Format format) {
        if (!sourceFile.exists()) {
            return missingSource(sourceFile, report);
        }
        List<NRGWidget> widgets = new ArrayList<>();
        if (cliWidgets != null) {
            widgets.addAll(cliWidgets);
        }
        widgets.addAll(additionalWidgets);
        List<Validator.Diagnostic> diagnostics = new Validator(sourceFile, rootDir, widgets).validate();
        boolean anyError = false;
        for (Validator.Diagnostic d : diagnostics) {
            if (format == RunReport.Format.TEXT) {
                System.err.println(d.toString());
            }
            if (d.isError()) anyError = true;
        }
        report.addDiagnostics(diagnostics);
        report.addFile(sourceFile, null, null, anyError ? "invalid" : "ok");
        return anyError ? EXIT_TEMPLATE_ERROR : EXIT_OK;
    }

    private static int missingSource(File sourceFile, RunReport report) {
        LOG.error("Source file does not exist: {}", sourceFile.getAbsolutePath());
        report.addDiagnostic(new Validator.Diagnostic(sourceFile, 0, Validator.Severity.ERROR, "source-not-found",
                "source file does not exist: " + sourceFile.getPath(), null));
        return EXIT_USAGE;
    }

    /** Records a failure that stops generation of {@code sourceFile}; text mode has already logged it. */
    private static int generationFailed(File sourceFile, RunReport report, String code, String message) {
        report.addDiagnostic(new Validator.Diagnostic(sourceFile, 0, Validator.Severity.ERROR, code, message, null));
        return EXIT_TEMPLATE_ERROR;
    }

    private static ClassLoader buildExtraClassLoader(String classpathValue) {
        if (classpathValue == null || classpathValue.isEmpty()) {
            return null;
        }
        List<URL> urls = new ArrayList<>();
        for (String entry : classpathValue.split(Pattern.quote(File.pathSeparator))) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            File f = new File(trimmed);
            if (!f.exists()) {
                LOG.warn("--classpath entry does not exist: {}", f.getAbsolutePath());
                continue;
            }
            try {
                urls.add(f.toURI().toURL());
            } catch (MalformedURLException e) {
                LOG.error(e, () -> "Cannot convert --classpath entry to URL: " + trimmed);
            }
        }
        if (urls.isEmpty()) {
            return null;
        }
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    private static LogLevel resolveLogLevel(String cliValue) throws ParseException {
        String raw = cliValue;
        if (raw == null || raw.isEmpty()) {
            raw = System.getenv(ENV_LOG_LEVEL);
        }
        if (raw == null || raw.isEmpty()) {
            return LogLevel.INFO;
        }
        LogLevel parsed = LogLevel.parse(raw);
        if (parsed == null) {
            throw new ParseException("Invalid log level: '" + raw + "' (expected trace|debug|info|warn|error)");
        }
        return parsed;
    }

    public static void applyLogLevel(LogLevel level) {
        applyLogLevel(level, false);
    }

    /**
     * @param everythingToStderr send every level to stderr — {@code --format json} keeps stdout
     *                           for the report alone, while the stock logger prints below ERROR
     *                           to stdout
     */
    private static void applyLogLevel(LogLevel level, boolean everythingToStderr) {
        int threshold = level.ordinal();
        ConsoleLogger logger = everythingToStderr
                ? new ConsoleLogger() {
                    @Override
                    protected PrintStream getOutputStream(LogEntry entry) {
                        return System.err;
                    }
                }
                : new ConsoleLogger();
        logger.setTraceEnabled(LogLevel.TRACE.ordinal() >= threshold);
        logger.setDebugEnabled(LogLevel.DEBUG.ordinal() >= threshold);
        logger.setInfoEnabled(LogLevel.INFO.ordinal() >= threshold);
        logger.setWarnEnabled(LogLevel.WARN.ordinal() >= threshold);
        logger.setErrorEnabled(true);
        LOG.init(logger);
    }

    private static int generate(File sourceFile, Charset charset, boolean toStdout, String languageFilter,
                                List<NRGWidget> cliWidgets, boolean checkMode, boolean allowExec,
                                String fileNamePatternOverride, String defaultLangFileNamePatternOverride,
                                boolean perFileStdoutHeader, LineEndings.Mode lineEndingMode,
                                boolean noHeaderOverride, String headerTextOverride,
                                PathPatternMatcher checkPathFilter, MatchTracker checkMatchTracker,
                                File rootDir, RunReport report, RunReport.Format format) {
        if (!sourceFile.exists()) {
            return missingSource(sourceFile, report);
        }
        try {
            List<NRGWidget> widgets = new ArrayList<>();
            if (cliWidgets != null) {
                widgets.addAll(cliWidgets);
            }
            widgets.addAll(additionalWidgets);
            Generator generator = new Generator(sourceFile, charset, widgets);
            if (rootDir != null) {
                generator.getConfig().setProjectRoot(rootDir);
            }
            if (fileNamePatternOverride != null) {
                generator.getConfig().getProperties().setProperty(
                        NRGConstants.PROPERTY_FILE_NAME_PATTERN, fileNamePatternOverride);
            }
            if (defaultLangFileNamePatternOverride != null) {
                generator.getConfig().getProperties().setProperty(
                        NRGConstants.PROPERTY_DEFAULT_LANGUAGE_FILE_NAME_PATTERN,
                        defaultLangFileNamePatternOverride);
            }
            if (noHeaderOverride) {
                generator.getConfig().getProperties().setProperty(
                        NRGConstants.PROPERTY_NO_HEADER, "true");
            }
            if (headerTextOverride != null) {
                generator.getConfig().getProperties().setProperty(
                        NRGConstants.PROPERTY_HEADER_TEXT, headerTextOverride);
            }
            Optional<String> patternError = OutputFileNameValidator.findError(
                    sourceFile, generator.getConfig().getDefaultLanguage(),
                    generator.getConfig().getLanguages(), generator.getConfig().getProperties(),
                    generator.getConfig().getProjectRoot());
            if (patternError.isPresent()) {
                LOG.error("File name pattern error: {}", patternError.get());
                return generationFailed(sourceFile, report, "file-name-pattern", patternError.get());
            }
            List<Validator.Diagnostic> freezeDiags = FreezeValidator.validate(
                    sourceFile,
                    FileUtils.readFileToString(sourceFile, charset),
                    generator.getConfig().getLanguages());
            boolean hasFreezeError = false;
            for (Validator.Diagnostic d : freezeDiags) {
                if (d.isError()) {
                    if (format == RunReport.Format.TEXT) {
                        System.err.println(d);
                    }
                    report.addDiagnostic(d);
                    hasFreezeError = true;
                }
            }
            if (hasFreezeError) {
                return EXIT_TEMPLATE_ERROR;
            }
            generator.getConfig().setExecAllowed(allowExec);
            if (checkMode) {
                return performCheck(generator, lineEndingMode, checkPathFilter, checkMatchTracker, report, format);
            }
            if (toStdout) {
                printToStdout(generator, languageFilter, perFileStdoutHeader);
                return EXIT_OK;
            }
            createFiles(generator, lineEndingMode, report);
            return EXIT_OK;
        } catch (IOException e) {
            LOG.error(e, () -> "Generation failed: " + sourceFile.getAbsolutePath());
            return generationFailed(sourceFile, report, "generation-failed", String.valueOf(e.getMessage()));
        } catch (RuntimeException e) {
            // A widget or the template itself gave up (an import outside the project root, a
            // failed remote fetch, ...). Reported as a failed generation of this file instead of
            // escaping run(), so a batch still reports the other files and its exit status.
            LOG.error(e, () -> "Generation failed: " + sourceFile.getAbsolutePath());
            return generationFailed(sourceFile, report, "generation-failed",
                    e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private static int performCheck(Generator generator, LineEndings.Mode lineEndingMode,
                                    PathPatternMatcher filter, MatchTracker tracker,
                                    RunReport report, RunReport.Format format)
            throws IOException {
        boolean failed = false;
        File sourceFile = generator.getConfig().getSourceFile();
        for (String language : generator.getConfig().getLanguages()) {
            File readmeFile = getReadmeFile(language, generator.getConfig());
            // Force rendering for every declared language so widget/template errors continue to
            // surface in --check, even when the resulting output is excluded from the filter.
            String generatedRaw = generator.getResult(language).getContent().toString();
            if (!filter.isUnconstrained() && !filter.matches(readmeFile)) {
                continue;
            }
            tracker.record();
            if (!readmeFile.exists()) {
                System.err.println("Missing file: " + readmeFile.getName() + " (would be created by generation)");
                report.addFile(sourceFile, readmeFile, language, "missing");
                failed = true;
                continue;
            }
            // Apply the same line-ending mode to the generated content as createFiles would,
            // so AUTO mode does not flag a CRLF-vs-LF-only difference between the in-memory
            // platform-LS output and an existing file written under a different convention.
            String targetEnding = LineEndings.resolve(lineEndingMode, readmeFile, StandardCharsets.UTF_8);
            String generated = LineEndings.applyTo(generatedRaw, targetEnding);
            String existing = FileUtils.readFileToString(readmeFile, StandardCharsets.UTF_8);
            if (!generated.equals(existing)) {
                System.err.print(unifiedDiff(existing, generated, readmeFile.getName()));
                report.addFile(sourceFile, readmeFile, language, "stale");
                failed = true;
            } else {
                report.addFile(sourceFile, readmeFile, language, "ok");
            }
        }
        if (failed && format == RunReport.Format.TEXT) {
            System.err.println("Regenerate with: nrg -f " + sourceFile.getPath());
        }
        return failed ? EXIT_DRIFT : EXIT_OK;
    }

    /**
     * Renders a minimal unified-diff hunk between {@code existingContent} (on disk) and
     * {@code generatedContent} for the {@code --check} mode output. Trims common prefix
     * and suffix lines, then emits a single hunk with up to three lines of context.
     */
    static String unifiedDiff(String existingContent, String generatedContent, String filename) {
        if (Objects.equals(existingContent, generatedContent)) {
            return "";
        }
        List<String> a = splitLines(existingContent);
        List<String> b = splitLines(generatedContent);

        int prefix = 0;
        int maxPrefix = Math.min(a.size(), b.size());
        while (prefix < maxPrefix && a.get(prefix).equals(b.get(prefix))) {
            prefix++;
        }
        int suffix = 0;
        int maxSuffix = Math.min(a.size() - prefix, b.size() - prefix);
        while (suffix < maxSuffix && a.get(a.size() - 1 - suffix).equals(b.get(b.size() - 1 - suffix))) {
            suffix++;
        }

        int ctx = 3;
        int ctxStart = Math.max(0, prefix - ctx);
        int ctxEndA = Math.min(a.size(), a.size() - suffix + ctx);

        StringBuilder sb = new StringBuilder();
        String ls = System.lineSeparator();
        sb.append("--- ").append(filename).append(" (on disk)").append(ls);
        sb.append("+++ ").append(filename).append(" (generated)").append(ls);
        sb.append("@@ line ").append(prefix + 1).append(" @@").append(ls);

        for (int i = ctxStart; i < prefix; i++) {
            sb.append(' ').append(a.get(i)).append(ls);
        }
        for (int i = prefix; i < a.size() - suffix; i++) {
            sb.append('-').append(a.get(i)).append(ls);
        }
        for (int i = prefix; i < b.size() - suffix; i++) {
            sb.append('+').append(b.get(i)).append(ls);
        }
        for (int i = a.size() - suffix; i < ctxEndA; i++) {
            sb.append(' ').append(a.get(i)).append(ls);
        }
        return sb.toString();
    }

    private static List<String> splitLines(String s) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(s.split("\\R", -1)));
    }

    private static void createFiles(Generator generator, LineEndings.Mode lineEndingMode, RunReport report)
            throws IOException {
        File sourceFile = generator.getConfig().getSourceFile();
        for (String language : generator.getConfig().getLanguages()) {
            File readmeFile = getReadmeFile(language, generator.getConfig());
            LOG.debug("Generating file for language \"{}\" - {}", language, readmeFile.getAbsolutePath());
            File parent = readmeFile.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    LOG.error("Cannot create directory for output file: {}", parent.getAbsolutePath());
                    report.addDiagnostic(new Validator.Diagnostic(sourceFile, 0, Validator.Severity.ERROR,
                            "generation-failed", "cannot create directory " + parent.getPath(), null));
                    continue;
                }
            }
            String targetEnding = LineEndings.resolve(lineEndingMode, readmeFile, StandardCharsets.UTF_8);
            String content = LineEndings.applyTo(
                    generator.getResult(language).getContent(), targetEnding);
            boolean unchanged = readmeFile.isFile()
                    && content.equals(FileUtils.readFileToString(readmeFile, StandardCharsets.UTF_8));
            FileUtils.write(readmeFile, content, StandardCharsets.UTF_8);
            report.addFile(sourceFile, readmeFile, language, unchanged ? "unchanged" : "written");
            LOG.info("File \"{}\" created, total size {}", readmeFile.getName(), FileUtils.byteCountToDisplaySize(readmeFile.length()));
        }
    }

    /**
     * Streams the generator's rendered output to stdout. The behavior depends on whether a
     * single-language filter is requested and whether the multi-file caller has signalled that
     * separators should be forced even for single-language files (i.e. multiple sources in one
     * batch).
     *
     * <p>When {@code languageFilter} is non-null but the file does not declare it, this is a
     * <em>skip</em> rather than an error: in a multi-file batch other files may declare it, so
     * we log a warning and move on instead of failing the whole run.
     */
    private static void printToStdout(Generator generator, String languageFilter, boolean forceHeader) {
        List<String> languages = generator.getConfig().getLanguages();
        if (languageFilter != null) {
            if (!languages.contains(languageFilter)) {
                System.err.println("WARN: --language '" + languageFilter
                        + "' not declared in " + generator.getConfig().getSourceFile().getName()
                        + " (available: " + languages + "); skipping");
                return;
            }
            if (forceHeader) {
                File readmeFile = getReadmeFile(languageFilter, generator.getConfig());
                System.out.println("=== " + readmeFile.getName() + " ===");
            }
            System.out.print(generator.getResult(languageFilter).getContent());
            if (forceHeader) {
                System.out.println();
            }
            return;
        }
        boolean multiple = languages.size() > 1 || forceHeader;
        for (String lang : languages) {
            if (multiple) {
                File readmeFile = getReadmeFile(lang, generator.getConfig());
                System.out.println("=== " + readmeFile.getName() + " ===");
            }
            System.out.print(generator.getResult(lang).getContent());
            if (multiple) {
                System.out.println();
            }
        }
    }

    public static File getReadmeFile(String language, GeneratorConfig config) {
        return OutputFileNameResolver.resolve(
                config.getSourceFile(), config.getDefaultLanguage(), language, config.getProperties());
    }

    /**
     * Returns the output file for the given language using built-in default naming only.
     *
     * @param language        the target language code, or {@code null} for the default language.
     * @param sourceFile      the {@code *.src.md} source file.
     * @param defaultLanguage the configured default language code.
     * @return the resolved output {@link File} (e.g. {@code README.md} or {@code README.<lang>.md}).
     * @deprecated use {@link #getReadmeFile(String, GeneratorConfig)}; this overload uses
     * built-in default naming only and ignores any configured filename patterns.
     */
    @Deprecated
    public static File getReadmeFile(String language, File sourceFile, String defaultLanguage) {
        return OutputFileNameResolver.resolve(sourceFile, defaultLanguage, language, new Properties());
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("nrg", null, options, null, true);
    }

    private static void printVersion() {
        System.out.println("Nanolaba Readme Generator " + getVersion());
        System.out.println("https://github.com/nanolaba/readme-generator");
    }

    public static String getVersion() {
        URL versionFile = Thread.currentThread().getContextClassLoader().getResource("version.txt");
        return versionFile == null ?
                "<unrecognized version>" :
                Code.run(() -> {
                    try (InputStream stream = versionFile.openConnection().getInputStream()) {
                        return IOUtils.toString(stream, StandardCharsets.UTF_8);
                    }
                });
    }

    public static void addWidget(NRGWidget widget) {
        Objects.requireNonNull(widget);
        additionalWidgets.add(widget);
    }

    /**
     * Counts how many outputs survived the {@code --check-paths} filter across the whole run,
     * so the multi-source loop can warn once when zero outputs matched any pattern.
     */
    private static final class MatchTracker {
        private int count;
        void record() { count++; }
        boolean anyMatched() { return count > 0; }
    }
}
