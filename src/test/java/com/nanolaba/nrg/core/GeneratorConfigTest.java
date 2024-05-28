package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorConfigTest extends DefaultNRGTest {

    @Test
    public void testPrintEmptyProperties() {
        new GeneratorConfig(new File("README.src.md"), "");
        String output = getOutAndClear();
        assertTrue(output.contains("Generator configuration:"));
        assertTrue(output.contains("widgets: ["));
        assertTrue(output.contains("sourceFile: README.src.md"));
        assertTrue(output.contains("languages: [en]"));
        assertTrue(output.contains("defaultLanguage: en"));
        assertTrue(output.contains("properties: {}"));

        assertFalse(output.contains("sourceFileBody: "));
    }

    @Test
    public void testPrintProperties() {
        new GeneratorConfig(new File("README.src.md"), """
                <!--@nrg.languages=en,ru-->
                """);
        String output = getOutAndClear();
        assertTrue(output.contains("properties: {nrg.languages=en,ru}"));
    }

    @Test
    public void testGetWidget() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "");

        assertNull(config.getWidget(null));
        assertNull(config.getWidget(""));
        assertNull(config.getWidget("123"));

        assertNotNull(config.getWidget("languages"));
    }

}