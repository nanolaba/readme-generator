package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest extends DefaultNRGTest {

    @Test
    public void testEmptySourceGenerator() {
        Generator generator = new Generator(new File("README.src.md"), "");

        Collection<GenerationResult> results = generator.getResults();
        assertEquals(1, results.size());

        GenerationResult result = results.iterator().next();
        assertEquals("en", result.getLanguage());
        assertTrue(result.getContent().toString().contains("This file was automatically generated"));

    }

    @Test
    public void testIncorrectDefaultLanguage() {
        assertThrows(IllegalStateException.class,
                () -> new Generator(new File("README.src.md"),
                        "<!-- @" + PROPERTY_LANGUAGES + "=xx-->" + RN +
                        "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=zz-->"));
    }

    @Test
    public void testInlineIgnoreRemovesLine() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en,ru-->" + RN +
                "visible" + RN +
                "hidden<!--nrg.ignore-->" + RN +
                "also visible";

        Generator generator = new Generator(new File("README.src.md"), src);
        for (GenerationResult result : generator.getResults()) {
            String body = result.getContent().toString();
            assertTrue(body.contains("visible"), "'visible' missing in " + result.getLanguage());
            assertTrue(body.contains("also visible"), "'also visible' missing in " + result.getLanguage());
            assertFalse(body.contains("hidden"), "'hidden' leaked in " + result.getLanguage());
        }
    }

    @Test
    public void testBlockIgnoreRemovesRange() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en,ru-->" + RN +
                "before" + RN +
                "<!--nrg.ignore.begin-->" + RN +
                "drop-1" + RN +
                "drop-2" + RN +
                "<!--nrg.ignore.end-->" + RN +
                "after";

        Generator generator = new Generator(new File("README.src.md"), src);
        for (GenerationResult result : generator.getResults()) {
            String body = result.getContent().toString();
            assertTrue(body.contains("before"));
            assertTrue(body.contains("after"));
            assertFalse(body.contains("drop-1"));
            assertFalse(body.contains("drop-2"));
            assertFalse(body.contains("nrg.ignore.begin"));
            assertFalse(body.contains("nrg.ignore.end"));
        }
    }

    @Test
    public void testIgnoredLinesDoNotDefineProperties() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en-->" + RN +
                "<!--@foo=real-->" + RN +
                "<!--nrg.ignore.begin-->" + RN +
                "<!--@foo=hidden-->" + RN +
                "<!--nrg.ignore.end-->" + RN +
                "${foo}";

        Generator generator = new Generator(new File("README.src.md"), src);
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("real"));
        assertFalse(body.contains("hidden"));
    }

    @Test
    public void testExtraEndMarkerIsDropped() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en-->" + RN +
                "keep" + RN +
                "<!--nrg.ignore.end-->";

        Generator generator = new Generator(new File("README.src.md"), src);
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("keep"));
        assertFalse(body.contains("nrg.ignore.end"));
    }

}