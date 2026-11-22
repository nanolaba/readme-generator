package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests of {@link SourceOrigin}'s text form, the one {@code --locate} prints.
 *
 * <p>{@link SourceMapTest} covers how origins are collected during generation; this class pins
 * every optional part of {@link SourceOrigin#toString()} on hand-built instances.
 */
class SourceOriginTest {

    @Test
    void lineWithoutATemplateIsDescribedByItsNote() {
        assertEquals("<generated>", SourceOrigin.note(null).toString());
        assertEquals("head comment", SourceOrigin.note("head comment").toString());
    }

    @Test
    void plainTemplateLineIsFileAndLine() {
        assertEquals("README.src.md:12", SourceOrigin.of(new File("docs/README.src.md"), 12).toString());
    }

    @Test
    void everyOptionalPartIsAppendedInOrder() {
        SourceOrigin importLine = SourceOrigin.of(new File("README.src.md"), 7);
        SourceOrigin origin = SourceOrigin.of(new File("part.src.md"), 3)
                .producedBy("toc")
                .withNote("frozen region")
                .importedVia(importLine);

        assertEquals("part.src.md:3 widget toc (frozen region) <- README.src.md:7", origin.toString());
    }

    @Test
    void eachOptionalPartAlone() {
        File part = new File("part.src.md");
        assertEquals("part.src.md:3 widget toc", SourceOrigin.of(part, 3).producedBy("toc").toString());
        assertEquals("part.src.md:3 (frozen region)", SourceOrigin.of(part, 3).withNote("frozen region").toString());
        assertEquals("part.src.md:3 <- README.src.md:7",
                SourceOrigin.of(part, 3).importedVia(SourceOrigin.of(new File("README.src.md"), 7)).toString());
    }
}
