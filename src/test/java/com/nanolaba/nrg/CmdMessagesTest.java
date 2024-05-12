package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CmdMessagesTest extends ConsoleOutputSupportTest {

    @Test
    public void printVersionTest() throws IOException {
        ReadmeGenerator.main("--version");
        String result = getOutAndClear();

        assertNotNull(result);

        assertFalse(result.isEmpty());
        assertFalse(result.contains("${project.version}"));
        assertFalse(result.contains("unrecognized version"));

        assertTrue(result.contains("Nanolaba Readme Generator"));
        assertTrue(result.contains("https://github.com/nanolaba/readme-generator"));
    }

    @Test
    public void printHelpTest() throws IOException {
        ReadmeGenerator.main("--help");
        String result = getOutAndClear();

        assertNotNull(result);

        assertFalse(result.isEmpty());

        assertTrue(result.contains("usage: "));
    }

    @Test
    public void printMessageForIncorrectArgumentsTest() throws IOException {
        ReadmeGenerator.main("--incorrect arguments");
        String result = getErrAndClear();

        assertNotNull(result);

        assertFalse(result.isEmpty());

        assertTrue(result.contains("Incorrect command line arguments:"));
        assertTrue(result.contains("To view help, run with the -h option"));
    }
}