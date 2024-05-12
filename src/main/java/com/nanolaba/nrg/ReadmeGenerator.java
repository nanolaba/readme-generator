package com.nanolaba.nrg;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.URL;

public class ReadmeGenerator {

    public static void main(String... args) throws IOException {
        try {
            parseCommandLine(args);
        } catch (ParseException e) {
            System.err.println("Incorrect command line arguments: " + e.getMessage());
            System.err.println("To view help, run with the -h option");
        }
    }

    private static void parseCommandLine(String[] args) throws ParseException, IOException {
        Options options = new Options();

        Option version = new Option(null, "version", false, "print the version information and exit");
        options.addOption(version);
        Option help = new Option("h", "help", false, "print this message and exit");
        options.addOption(help);
        Option file = new Option("f", "file", true, "source file");
        file.setArgName("filename");
        options.addOption(file);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(version)) {
            printVersion();
        } else if (cmd.hasOption(help)) {
            printHelp(options);
        } else {
            if (cmd.hasOption(file)) {
                generate(cmd.getOptionValue(file));
            }
        }
    }

    private static void generate(String fileName) {

    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("nrg", null, options, null, true);
    }

    private static void printVersion() throws IOException {
        URL versionFile = Thread.currentThread().getContextClassLoader().getResource("version.txt");
        Object version = versionFile == null ? "<unrecognized version>" : versionFile.getContent();
        System.out.println("Nanolaba Readme Generator " + version);
        System.out.println("https://github.com/nanolaba/readme-generator");
    }
}
