package com.nanolaba.readme_generator;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.URL;

public class ReadmeGenerator {

    public static void main(String... args) throws ParseException, IOException {
        Options options = new Options();

        Option version = new Option("version", "version", false, "print the version information and exit");
        options.addOption(version);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(version)) {
            printVersion();
        } else {

        }

    }

    private static void printVersion() throws IOException {
        URL versionFile = Thread.currentThread().getContextClassLoader().getResource("version.txt");
        Object version = versionFile == null ? "<unrecognized version>" : versionFile.getContent();
        System.out.println("Nanolaba Readme Generator " + version);
        System.out.println("https://github.com/nanolaba/readme-generator");
    }
}
