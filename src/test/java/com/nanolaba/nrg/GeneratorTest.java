package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nanolaba.nrg.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {

    @Test
    public void testConfigLanguages1() {
        GeneratorConfig config = new Generator("<!--" + PROPERTY_LANGUAGES + "=ru-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("ru", langs.get(0));
        assertEquals("ru", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages2() {
        GeneratorConfig config = new Generator("<!--comment-->some text<!-- " + PROPERTY_LANGUAGES + " = zz -->some text").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("zz", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages3() {
        GeneratorConfig config = new Generator("<!--comment-->some text<!-- " + PROPERTY_LANGUAGES + " = aa ,bb, cc , dd-->some text").getConfig();
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
        GeneratorConfig config = new Generator("<!-- " + PROPERTY_LANGUAGES + "=xx-->" + System.lineSeparator() +
                                               "<!--" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("xx", langs.get(0));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguagesWithDefaultLanguage2() {
        GeneratorConfig config = new Generator("<!--comment-->some text" +
                                               "<!-- " + PROPERTY_LANGUAGES + " = zz, xx -->some text" +
                                               "<!--" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("xx", langs.get(1));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testParseConfigWithContent() {
        GeneratorConfig config = new Generator("<!--nrg.languages=en,ru-->\n" +
                                               "<!--nrg.defaultLanguage=en-->\n" +
                                               "# Nanolaba Readme Generator (NRG)\n").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("en", langs.get(0));
        assertEquals("ru", langs.get(1));
        assertEquals("en", config.getDefaultLanguage());
    }

    @Test
    public void testIncorrectDefaultLanguage() {
        assertThrows(IllegalStateException.class,
                () -> new Generator("<!-- " + PROPERTY_LANGUAGES + "=xx-->" + System.lineSeparator() +
                                    "<!--" + PROPERTY_DEFAULT_LANGUAGE + "=zz-->"));
    }

    @Test
    public void testLineVisibility() {
        Generator generator = new Generator("");

        assertTrue(generator.isLineVisible("", "ru"));
        assertTrue(generator.isLineVisible(" ", "ru"));
        assertTrue(generator.isLineVisible("test <!--ru-->", "ru"));
        assertTrue(generator.isLineVisible("test <!-- ru -->", "ru"));
        assertTrue(generator.isLineVisible("test <!-- ru --> test", "ru"));
        assertTrue(generator.isLineVisible("test <!-- ru --><!--en-->", "ru"));
        assertTrue(generator.isLineVisible("<!--ru-->", "ru"));
        assertTrue(generator.isLineVisible("<!--ru--> test", "ru"));
        assertTrue(generator.isLineVisible("<!-- ru -->", "ru"));
        assertTrue(generator.isLineVisible("<!-- ru --> test", "ru"));
        assertTrue(generator.isLineVisible("<!--  ru  --> test", "ru"));

        assertFalse(generator.isLineVisible("<!--en-->", "ru"));
        assertFalse(generator.isLineVisible("<!--en--> test", "ru"));
        assertFalse(generator.isLineVisible("test <!--en-->", "ru"));
        assertFalse(generator.isLineVisible("test <!-- en-->", "ru"));
        assertFalse(generator.isLineVisible("test <!-- en -->", "ru"));
        assertFalse(generator.isLineVisible("test  <!-- en -->", "ru"));

        assertFalse(generator.isLineVisible("<!--nrg.someproperty-->", "ru"));
        assertFalse(generator.isLineVisible("<!--nrg.some property-->", "ru"));
        assertFalse(generator.isLineVisible("<!--nrg.some=property-->", "ru"));
        assertFalse(generator.isLineVisible("<!--nrg.some = property-->", "ru"));
        assertFalse(generator.isLineVisible("<!--  nrg.some  =  property  -->", "ru"));
    }

    @Test
    public void testRemoveNrgDataFromText() {
        Generator generator = new Generator("<!--" + PROPERTY_LANGUAGES + "=ru-->");

        assertEquals("", generator.removeNrgDataFromText(""));
        assertEquals(" ", generator.removeNrgDataFromText(" "));
        assertEquals(" ", generator.removeNrgDataFromText(" <!--ru-->"));
        assertEquals(" ", generator.removeNrgDataFromText("<!--ru--> "));
        assertEquals("123 ", generator.removeNrgDataFromText("123<!--ru--> "));
        assertEquals("123<!--en--> ", generator.removeNrgDataFromText("123<!--en--> "));
        assertEquals("", generator.removeNrgDataFromText("<!--nrg.test-->"));
        assertEquals("", generator.removeNrgDataFromText("<!--nrg.test -->"));
        assertEquals("", generator.removeNrgDataFromText("<!--nrg.test = test-->"));
        assertEquals("", generator.removeNrgDataFromText("<!-- nrg.test = test --><!--ru-->"));
        assertEquals("123", generator.removeNrgDataFromText("1<!-- nrg.test = test -->2<!--ru-->3"));
        assertEquals("123", generator.removeNrgDataFromText("1<!-- nrg.test  =  test  -->2<!--ru-->3"));
    }
}