package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorTest {

    @Test
    public void testConfigLanguages1() {
        GeneratorConfig config = new Generator("<!--" + GeneratorConfig.PROPERTY_LANGUAGES + "=ru-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("ru", langs.get(0));
        assertEquals("ru", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages2() {
        GeneratorConfig config = new Generator("<!--comment-->some text<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + " = zz -->some text").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("zz", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages3() {
        GeneratorConfig config = new Generator("<!--comment-->some text<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + " = aa ,bb, cc , dd-->some text").getConfig();
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
        GeneratorConfig config = new Generator("<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + "=xx-->" + System.lineSeparator() +
                                               "<!--" + GeneratorConfig.PROPERTY_DEFAULT_LANGUAGE + "=xx-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("xx", langs.get(0));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguagesWithDefaultLanguage2() {
        GeneratorConfig config = new Generator("<!--comment-->some text" +
                                               "<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + " = zz, xx -->some text" +
                                               "<!--" + GeneratorConfig.PROPERTY_DEFAULT_LANGUAGE + "=xx-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testIncorrectDefaultLanguage() {
        assertThrows(IllegalStateException.class,
                () -> new Generator("<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + "=xx-->" + System.lineSeparator() +
                                    "<!--" + GeneratorConfig.PROPERTY_DEFAULT_LANGUAGE + "=zz-->"));
    }
}