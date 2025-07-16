package com.nanolaba.nrg;

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

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(version)) {
            printVersion();
        } else if (cmd.hasOption(help)) {
            printHelp(options);
        } else {
            Charset sourceCharset = cmd.getParsedOptionValue(charset, DEFAULT_CHARSET);
            LOG.debug("Source charset: {}", sourceCharset.displayName());
            if (cmd.hasOption(file)) {
                generate(cmd.getParsedOptionValue(file), sourceCharset);
            }
        }
    }

    private static void generate(File sourceFile, Charset charset) {
        if (sourceFile.exists()) {
            Code.run(() -> createFiles(new Generator(sourceFile, charset, additionalWidgets)));
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

    public static File getReadmeFile(String language, GeneratorConfig config) {
        String path = StringUtils.substringBeforeLast(config.getSourceFile().getAbsolutePath(), "." + NRGConstants.DEFAULT_SOURCE_EXTENSION) +
                (language.equals(config.getDefaultLanguage()) ? ".md" : "." + language + ".md");

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
