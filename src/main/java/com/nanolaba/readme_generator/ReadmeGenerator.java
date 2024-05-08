package com.nanolaba.readme_generator;

import org.apache.commons.cli.*;

public class ReadmeGenerator {

    public static void main(String[] args) throws ParseException {
        Options options = new Options();

        Option version = new Option("version", "print the version information and exit");
        options.addOption(version);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(version)) {
            printVersion();
        }

    }

    private static void printVersion() {

    }
}
