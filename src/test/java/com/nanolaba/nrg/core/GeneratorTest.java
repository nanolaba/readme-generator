package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorTest {

    @Test
    public void testConfigLanguages1() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--@" + PROPERTY_LANGUAGES + "=ru-->");
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("ru", langs.get(0));
        assertEquals("ru", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages2() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text<!-- @" + PROPERTY_LANGUAGES + " = zz -->some text");
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("zz", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages3() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text<!-- @" + PROPERTY_LANGUAGES + " = aa ,bb, cc , dd-->some text");
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
                "<!-- @" + PROPERTY_LANGUAGES + "=xx-->" + System.lineSeparator() +
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
                "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->");
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("xx", langs.get(1));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testParseConfigWithContent() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                "<!--@nrg.defaultLanguage=en-->\n" +
                "# Nanolaba Readme Generator (NRG)\n");
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("en", langs.get(0));
        assertEquals("ru", langs.get(1));
        assertEquals("en", config.getDefaultLanguage());
    }

    @Test
    public void testIncorrectDefaultLanguage() {
        assertThrows(IllegalStateException.class,
                () -> new Generator(new File("README.src.md"),
                        "<!-- @" + PROPERTY_LANGUAGES + "=xx-->" + System.lineSeparator() +
                        "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=zz-->"));
    }

}