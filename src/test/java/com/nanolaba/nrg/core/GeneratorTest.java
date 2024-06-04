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

}