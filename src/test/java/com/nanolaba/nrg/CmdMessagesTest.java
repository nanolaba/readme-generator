package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CmdMessagesTest extends ConsoleOutputSupportTest {

    @Test
    public void printVersionTest() throws IOException {
        ReadmeGenerator.main("--version");
        String result = getOutAndClear();
        originalOut.println(result);

        assertNotNull(result);

        assertFalse(result.contains("${project.version}"));
        assertFalse(result.contains("unrecognized version"));
        assertFalse(result.contains("@"));

        assertTrue(result.contains("Nanolaba Readme Generator"));
        assertTrue(result.contains("https://github.com/nanolaba/readme-generator"));
    }

    @Test
    public void printHelpTest() throws IOException {
        ReadmeGenerator.main("--help");
        String result = getOutAndClear();
        originalOut.println(result);

        assertNotNull(result);

        assertTrue(result.contains("usage: "));
    }

    @Test
    public void printMessageForIncorrectArgumentsTest() throws IOException {
        ReadmeGenerator.main("--incorrect arguments");
        String result = getErrAndClear();
        originalOut.println(result);

        assertNotNull(result);

        assertTrue(result.contains("Incorrect command line arguments:"));
        assertTrue(result.contains("To view help, run with the -h option"));
    }

    @Test
    public void printMessageForIncorrectCharsetTest() throws IOException {
        ReadmeGenerator.main("--charset", "UTF-9", "-f", "test.src.md");
        String result = getErrAndClear();
        originalOut.println(result);

        assertNotNull(result);

        assertTrue(result.contains("java.nio.charset.UnsupportedCharsetException: UTF-9"));
    }

    @Test
    public void printMessageForNonexistentFileTest() throws IOException {
        ReadmeGenerator.main("-f", "nonexistent file");
        String result = getErrAndClear();
        originalOut.println(result);

        assertNotNull(result);

        assertTrue(result.contains("Source file does not exist: "));
    }
}