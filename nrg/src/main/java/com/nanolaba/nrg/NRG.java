package com.nanolaba.nrg;

import com.nanolaba.logging.ConsoleLogger;
import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.core.OutputFileNameResolver;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.sugar.Code;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.nanolaba.nrg.core.NRGConstants.DEFAULT_CHARSET;

public class NRG {

    public static final String ENV_LOG_LEVEL = "NRG_LOG_LEVEL";

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
        try {
            return parseCommandLine(args);
        } catch (ParseException e) {
            System.err.println("Incorrect command line arguments: " + e.getMessage());
            System.err.println("To view help, run with the -h option");
            return 1;
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
        Option allowExec = new Option(null, "allow-exec", false,
                "allow the 'exec' widget to run external commands (disabled by default)");
        options.addOption(allowExec);
        Option validate = new Option(null, "validate", false,
                "validate the source template and imports without generating output; exit 1 on errors");
        options.addOption(validate);
        Option fileNamePattern = new Option(null, "file-name-pattern", true,
                "output filename pattern with placeholders <base>, <lang>, <LANG> (overrides nrg.fileNamePattern)");
        fileNamePattern.setArgName("pattern");
        options.addOption(fileNamePattern);
        Option defaultLangFileNamePattern = new Option(null, "default-language-file-name-pattern", true,
                "output filename pattern for the default language (overrides nrg.defaultLanguageFileNamePattern)");
        defaultLangFileNamePattern.setArgName("pattern");
        options.addOption(defaultLangFileNamePattern);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        applyLogLevel(resolveLogLevel(cmd.getOptionValue(logLevel)));

        if (cmd.hasOption(version)) {
            printVersion();
            return 0;
        }
        if (cmd.hasOption(help)) {
            printHelp(options);
            return 0;
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
        String langValue = cmd.getOptionValue(language);
        if (langValue != null && !toStdout) {
            LOG.warn("--language has no effect without --stdout; ignoring");
            langValue = null;
        }
        String fileNamePatternValue = cmd.getOptionValue(fileNamePattern);
        String defaultLangFileNamePatternValue = cmd.getOptionValue(defaultLangFileNamePattern);

        if (cmd.hasOption(file)) {
            File sourceFile = cmd.getParsedOptionValue(file);
            if (validateMode) {
                return runValidation(sourceFile);
            }
            return generate(sourceFile, sourceCharset, toStdout, langValue, cliWidgets, checkMode,
                    cmd.hasOption(allowExec), fileNamePatternValue, defaultLangFileNamePatternValue);
        }
        return 0;
    }

    private static int runValidation(File sourceFile) {
        if (!sourceFile.exists()) {
            LOG.error("Source file does not exist: {}", sourceFile.getAbsolutePath());
            return 1;
        }
        java.util.List<com.nanolaba.nrg.core.Validator.Diagnostic> diagnostics =
                new com.nanolaba.nrg.core.Validator(sourceFile).validate();
        if (diagnostics.isEmpty()) {
            return 0;
        }
        boolean anyError = false;
        for (com.nanolaba.nrg.core.Validator.Diagnostic d : diagnostics) {
            System.err.println(d.toString());
            if (d.isError()) anyError = true;
        }
        return anyError ? 1 : 0;
    }

    private static ClassLoader buildExtraClassLoader(String classpathValue) {
        if (classpathValue == null || classpathValue.isEmpty()) {
            return null;
        }
        List<URL> urls = new ArrayList<>();
        for (String entry : classpathValue.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
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
        int threshold = level.ordinal();
        ConsoleLogger logger = new ConsoleLogger();
        logger.setTraceEnabled(LogLevel.TRACE.ordinal() >= threshold);
        logger.setDebugEnabled(LogLevel.DEBUG.ordinal() >= threshold);
        logger.setInfoEnabled(LogLevel.INFO.ordinal() >= threshold);
        logger.setWarnEnabled(LogLevel.WARN.ordinal() >= threshold);
        logger.setErrorEnabled(true);
        LOG.init(logger);
    }

    private static int generate(File sourceFile, Charset charset, boolean toStdout, String languageFilter,
                                List<NRGWidget> cliWidgets, boolean checkMode, boolean allowExec,
                                String fileNamePatternOverride, String defaultLangFileNamePatternOverride) {
        if (!sourceFile.exists()) {
            LOG.error("Source file does not exist: {}", sourceFile.getAbsolutePath());
            return 1;
        }
        try {
            List<NRGWidget> widgets = new ArrayList<>();
            if (cliWidgets != null) {
                widgets.addAll(cliWidgets);
            }
            widgets.addAll(additionalWidgets);
            Generator generator = new Generator(sourceFile, charset, widgets);
            if (fileNamePatternOverride != null) {
                generator.getConfig().getProperties().setProperty(
                        com.nanolaba.nrg.core.NRGConstants.PROPERTY_FILE_NAME_PATTERN, fileNamePatternOverride);
            }
            if (defaultLangFileNamePatternOverride != null) {
                generator.getConfig().getProperties().setProperty(
                        com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE_FILE_NAME_PATTERN,
                        defaultLangFileNamePatternOverride);
            }
            java.util.Optional<String> patternError = com.nanolaba.nrg.core.OutputFileNameValidator.findError(
                    sourceFile, generator.getConfig().getDefaultLanguage(),
                    generator.getConfig().getLanguages(), generator.getConfig().getProperties());
            if (patternError.isPresent()) {
                LOG.error("File name pattern error: {}", patternError.get());
                return 1;
            }
            generator.getConfig().setExecAllowed(allowExec);
            if (checkMode) {
                return performCheck(generator);
            }
            if (toStdout) {
                printToStdout(generator, languageFilter);
                return 0;
            }
            createFiles(generator);
            return 0;
        } catch (IOException e) {
            LOG.error(e, () -> "Generation failed: " + sourceFile.getAbsolutePath());
            return 1;
        }
    }

    private static int performCheck(Generator generator) throws IOException {
        boolean failed = false;
        for (String language : generator.getConfig().getLanguages()) {
            File readmeFile = getReadmeFile(language, generator.getConfig());
            String generated = generator.getResult(language).getContent().toString();
            if (!readmeFile.exists()) {
                System.err.println("Missing file: " + readmeFile.getName() + " (would be created by generation)");
                failed = true;
                continue;
            }
            String existing = FileUtils.readFileToString(readmeFile, StandardCharsets.UTF_8);
            if (!generated.equals(existing)) {
                System.err.print(unifiedDiff(existing, generated, readmeFile.getName()));
                failed = true;
            }
        }
        return failed ? 1 : 0;
    }

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
        return new ArrayList<>(java.util.Arrays.asList(s.split("\\R", -1)));
    }

    private static void createFiles(Generator generator) throws IOException {
        for (String language : generator.getConfig().getLanguages()) {
            File readmeFile = getReadmeFile(language, generator.getConfig());
            LOG.debug("Generating file for language \"{}\" - {}", language, readmeFile.getAbsolutePath());
            File parent = readmeFile.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    LOG.error("Cannot create directory for output file: {}", parent.getAbsolutePath());
                    continue;
                }
            }
            FileUtils.write(readmeFile, generator.getResult(language).getContent(), StandardCharsets.UTF_8);
            LOG.info("File \"{}\" created, total size {}", readmeFile.getName(), FileUtils.byteCountToDisplaySize(readmeFile.length()));
        }
    }

    private static void printToStdout(Generator generator, String languageFilter) {
        List<String> languages = generator.getConfig().getLanguages();
        if (languageFilter != null) {
            if (!languages.contains(languageFilter)) {
                LOG.error("Unknown language '{}'; available: {}", languageFilter, languages);
                return;
            }
            System.out.print(generator.getResult(languageFilter).getContent());
            return;
        }
        boolean multiple = languages.size() > 1;
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
     * @deprecated use {@link #getReadmeFile(String, GeneratorConfig)}; this overload uses
     * built-in default naming only and ignores any configured filename patterns.
     */
    @Deprecated
    public static File getReadmeFile(String language, File sourceFile, String defaultLanguage) {
        return OutputFileNameResolver.resolve(sourceFile, defaultLanguage, language, new java.util.Properties());
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
}
