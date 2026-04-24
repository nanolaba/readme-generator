package com.nanolaba.nrg;

import com.nanolaba.logging.ConsoleLogger;
import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGConstants;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.sugar.Code;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

    public static void main(String... args) {
        try {
            parseCommandLine(args);
        } catch (ParseException e) {
            System.err.println("Incorrect command line arguments: " + e.getMessage());
            System.err.println("To view help, run with the -h option");
        }
    }

    private static void parseCommandLine(String[] args) throws ParseException {
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

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        applyLogLevel(resolveLogLevel(cmd.getOptionValue(logLevel)));

        if (cmd.hasOption(version)) {
            printVersion();
        } else if (cmd.hasOption(help)) {
            printHelp(options);
        } else {
            Charset sourceCharset = cmd.getParsedOptionValue(charset, DEFAULT_CHARSET);
            LOG.debug("Source charset: {}", sourceCharset.displayName());

            boolean toStdout = cmd.hasOption(stdout);
            String langValue = cmd.getOptionValue(language);
            if (langValue != null && !toStdout) {
                LOG.warn("--language has no effect without --stdout; ignoring");
                langValue = null;
            }

            if (cmd.hasOption(file)) {
                generate(cmd.getParsedOptionValue(file), sourceCharset, toStdout, langValue);
            }
        }
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

    private static void generate(File sourceFile, Charset charset, boolean toStdout, String languageFilter) {
        if (sourceFile.exists()) {
            Code.run(() -> {
                Generator generator = new Generator(sourceFile, charset, additionalWidgets);
                if (toStdout) {
                    printToStdout(generator, languageFilter);
                } else {
                    createFiles(generator);
                }
            });
        } else {
            LOG.error("Source file does not exist: {}", sourceFile.getAbsolutePath());
        }
    }

    private static void createFiles(Generator generator) throws IOException {
        for (String language : generator.getConfig().getLanguages()) {
            File readmeFile = getReadmeFile(language, generator.getConfig());
            LOG.debug("Generating file for language \"{}\" - {}", language, readmeFile.getAbsolutePath());
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
        return getReadmeFile(language, config.getSourceFile(), config.getDefaultLanguage());
    }

    public static File getReadmeFile(String language, File sourceFile, String defaultLanguage) {
        String path = StringUtils.substringBeforeLast(sourceFile.getAbsolutePath(), "." + NRGConstants.DEFAULT_SOURCE_EXTENSION) +
                (language.equals(defaultLanguage) ? ".md" : "." + language + ".md");

        return new File(path);
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
