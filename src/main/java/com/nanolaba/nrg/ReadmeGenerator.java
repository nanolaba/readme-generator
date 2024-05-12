package com.nanolaba.nrg;

import com.nanolaba.logging.LOG;
import com.nanolaba.sugar.Code;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ReadmeGenerator {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static void main(String... args) throws IOException {
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
            try {
                String sourceBody = FileUtils.readFileToString(sourceFile, charset);
                Generator generator = new Generator(sourceBody);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.error("Source file does not exist: {}", sourceFile.getAbsolutePath());
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("nrg", null, options, null, true);
    }

    private static void printVersion() {
        URL versionFile = Thread.currentThread().getContextClassLoader().getResource("version.txt");
        Object version = versionFile == null ?
                "<unrecognized version>" :
                Code.run(() -> {
                    try (InputStream stream = versionFile.openConnection().getInputStream()) {
                        return IOUtils.toString(stream, StandardCharsets.UTF_8);
                    }
                });
        System.out.println("Nanolaba Readme Generator " + version);
        System.out.println("https://github.com/nanolaba/readme-generator");
    }
}
