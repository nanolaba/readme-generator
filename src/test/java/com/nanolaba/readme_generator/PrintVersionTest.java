package com.nanolaba.readme_generator;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PrintVersionTest extends CheckConsoleOutputTest {

    @Test
    public void printVersionTest() throws ParseException, IOException {
        ReadmeGenerator.main("--version");
        String result = getOutAndClear();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.contains("${project.version}"));
        assertFalse(result.contains("unrecognized version"));
        assertTrue(result.contains("Nanolaba Readme Generator"));
        assertTrue(result.contains("https://github.com/nanolaba/readme-generator"));
    }
}