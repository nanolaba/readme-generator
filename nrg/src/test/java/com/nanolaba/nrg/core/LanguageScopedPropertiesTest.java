package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageScopedPropertiesTest extends DefaultNRGTest {

    @Test
    public void testLanguageScopedKeyWinsOverBareKey() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                        "<!--@greeting=Hi-->\n" +
                        "<!--@greeting.ru=Привет-->\n" +
                        "${greeting}\n");

        String en = generator.getResult("en").getContent().toString();
        String ru = generator.getResult("ru").getContent().toString();

        assertTrue(en.contains("Hi"), en);
        assertFalse(en.contains("Привет"), en);
        assertTrue(ru.contains("Привет"), ru);
        assertFalse(ru.contains("Hi"), ru);
    }

    @Test
    public void testIssue42Fixture_perLanguageOverridesPlusSharedFallback() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,zh,ja,de-->\n" +
                        "<!--@screenshot.en=./public/show-en.png-->\n" +
                        "<!--@screenshot.zh=./public/show-zh.png-->\n" +
                        "<!--@screenshot=./public/show.png-->\n" +
                        "<img src=\"${screenshot}\" />\n");

        String en = generator.getResult("en").getContent().toString();
        String zh = generator.getResult("zh").getContent().toString();
        String ja = generator.getResult("ja").getContent().toString();
        String de = generator.getResult("de").getContent().toString();

        assertTrue(en.contains("<img src=\"./public/show-en.png\" />"), en);
        assertTrue(zh.contains("<img src=\"./public/show-zh.png\" />"), zh);
        assertTrue(ja.contains("<img src=\"./public/show.png\" />"), ja);
        assertTrue(de.contains("<img src=\"./public/show.png\" />"), de);
    }

    @Test
    public void testUnknownVariableLeavesLiteralUnchanged() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                        "${notDefined}\n");

        String en = generator.getResult("en").getContent().toString();
        String ru = generator.getResult("ru").getContent().toString();

        assertTrue(en.contains("${notDefined}"), en);
        assertTrue(ru.contains("${notDefined}"), ru);
    }
}
