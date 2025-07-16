package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorConfigTest extends DefaultNRGTest {

    @Test
    public void testPrintEmptyProperties() {
        new GeneratorConfig(new File("README.src.md"), "", null);
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
        new GeneratorConfig(new File("README.src.md"), "<!--@nrg.languages=en,ru-->", null);
        String output = getOutAndClear();
        assertTrue(output.contains("properties: {nrg.languages=en,ru}"));
    }

    @Test
    public void testGetWidget() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "", null);

        assertNull(config.getWidget(null));
        assertNull(config.getWidget(""));
        assertNull(config.getWidget("123"));

        assertNotNull(config.getWidget("languages"));
    }

    @Test
    public void testConfigLanguages1() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--@" + PROPERTY_LANGUAGES + "=ru-->", null);
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("ru", langs.get(0));
        assertEquals("ru", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages2() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text<!-- @" + PROPERTY_LANGUAGES + " = zz -->some text", null);
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("zz", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages3() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text<!-- @" + PROPERTY_LANGUAGES + " = aa ,bb, cc , dd-->some text", null);
        List<String> langs = config.getLanguages();
        assertEquals(4, langs.size());
        assertEquals("aa", langs.get(0));
        assertEquals("bb", langs.get(1));
        assertEquals("cc", langs.get(2));
        assertEquals("dd", langs.get(3));
        assertEquals("aa", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguagesWithDefaultLanguage1() {
        GeneratorConfig config = new Generator(new File("README.src.md"),
                "<!-- @" + PROPERTY_LANGUAGES + "=xx-->" + RN +
                "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("xx", langs.get(0));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguagesWithDefaultLanguage2() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text" +
                "<!-- @" + PROPERTY_LANGUAGES + " = zz, xx -->some text" +
                        "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->", null);
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("xx", langs.get(1));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testParseConfigWithContent() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "<!--@nrg.languages=en,ru-->\n<!--@nrg.defaultLanguage=en-->\n# Nanolaba Readme Generator (NRG)", null);
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("en", langs.get(0));
        assertEquals("ru", langs.get(1));
        assertEquals("en", config.getDefaultLanguage());
    }
}