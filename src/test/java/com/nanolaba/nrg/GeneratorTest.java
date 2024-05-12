package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratorTest {

    @Test
    public void testConfigLanguages() {
        List<String> languages = new Generator("<!--" + GeneratorConfig.PROPERTY_LANGUAGES + "=ru-->").getConfig().getLanguages();
        assertEquals(1, languages.size());
        assertEquals("ru", languages.get(0));

        languages = new Generator("<!--comment-->some text<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + " = zz -->some text").getConfig().getLanguages();
        assertEquals(1, languages.size());
        assertEquals("zz", languages.get(0));

        languages = new Generator("<!--comment-->some text<!-- " + GeneratorConfig.PROPERTY_LANGUAGES + " = aa ,bb, cc , dd-->some text").getConfig().getLanguages();
        assertEquals(4, languages.size());
        assertEquals("aa", languages.get(0));
        assertEquals("bb", languages.get(1));
        assertEquals("cc", languages.get(2));
        assertEquals("dd", languages.get(3));
    }
}