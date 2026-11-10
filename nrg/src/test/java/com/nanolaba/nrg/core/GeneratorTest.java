package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_HEADER_TEXT;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_NO_HEADER;
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
    public void testNoHeaderPropertySuppressesHeadComment() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en-->" + RN +
                "<!--@" + PROPERTY_NO_HEADER + "=true-->" + RN +
                "Body";

        Generator generator = new Generator(new File("README.src.md"), src);
        String out = generator.getResult("en").getContent().toString();
        assertFalse(out.contains("automatically generated"),
                "default head comment should be suppressed, got: " + out);
        assertTrue(out.contains("Body"));
    }

    @Test
    public void testHeaderTextPropertyReplacesDefaultHeadComment() {
        Generator generator = new Generator(new File("README.src.md"), "Body");
        generator.getConfig().getProperties().setProperty(PROPERTY_HEADER_TEXT, "<!-- Custom header -->");

        String out = generator.getResult("en").getContent().toString();
        assertFalse(out.contains("automatically generated"),
                "default head comment should be replaced, got: " + out);
        assertTrue(out.startsWith("<!-- Custom header -->" + RN),
                "custom header should be the first line, got: " + out);
    }

    @Test
    public void testHeaderTextEscapesProduceMultiLineHeader() {
        Generator generator = new Generator(new File("README.src.md"), "");
        generator.getConfig().getProperties().setProperty(PROPERTY_HEADER_TEXT, "Line A\\nLine B");

        String out = generator.getResult("en").getContent().toString();
        assertTrue(out.startsWith("Line A" + RN + "Line B" + RN),
                "\\n should expand to a line separator, got: " + out);
    }

    @Test
    public void testDefaultHeaderNamesSourceTemplate() {
        Generator generator = new Generator(new File("README.src.md"), "Body");

        String out = generator.getResult("en").getContent().toString();
        String firstLines = out.substring(0, out.indexOf("Body"));
        assertTrue(firstLines.contains("from README.src.md"),
                "default header must name the root template, got: " + firstLines);
        assertTrue(firstLines.contains("change README.src.md and regenerate"),
                "default header must say how to fix the file, got: " + firstLines);
        assertFalse(firstLines.contains(com.nanolaba.nrg.NRG.getVersion()),
                "default header must not carry the NRG version, got: " + firstLines);
    }

    @Test
    public void testDefaultHeaderSourcePathIsRelativeToOutputFile() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en,ru-->" + RN +
                "<!--@nrg.fileNamePattern=docs/<lang>/<base>.md-->" + RN +
                "Body";
        Generator generator = new Generator(new File("README.src.md"), src);

        String out = generator.getResult("ru").getContent().toString();
        assertTrue(out.contains("from ../../README.src.md"),
                "source path must be relative to docs/ru/README.md, got: " + out);
    }

    @Test
    public void testHeaderTextPlaceholders() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en,ru-->" + RN + "Body";
        Generator generator = new Generator(new File("README.src.md"), src);
        generator.getConfig().getProperties().setProperty(PROPERTY_HEADER_TEXT,
                "<!-- <source> / <lang> / <version> -->");

        String out = generator.getResult("ru").getContent().toString();
        assertTrue(out.startsWith("<!-- README.src.md / ru / " + com.nanolaba.nrg.NRG.getVersion() + " -->" + RN),
                "placeholders must be substituted, got: " + out);
    }

    @Test
    public void testHeaderTextAsCommentFromTemplateProperty() {
        String src = "<!--@" + PROPERTY_LANGUAGES + "=en-->" + RN +
                "<!--@" + PROPERTY_HEADER_TEXT + "=<\\!-- Generated from <source>, do not edit --\\>-->" + RN +
                "Body";
        Generator generator = new Generator(new File("README.src.md"), src);

        String out = generator.getResult("en").getContent().toString();
        assertTrue(out.startsWith("<!-- Generated from README.src.md, do not edit -->" + RN),
                "<\\!-- and --\\> must produce a well-formed comment, got: " + out);
    }

    @Test
    public void testNoHeaderWinsOverHeaderText() {
        Generator generator = new Generator(new File("README.src.md"), "Body");
        generator.getConfig().getProperties().setProperty(PROPERTY_NO_HEADER, "true");
        generator.getConfig().getProperties().setProperty(PROPERTY_HEADER_TEXT, "should not appear");

        String out = generator.getResult("en").getContent().toString();
        assertFalse(out.contains("should not appear"),
                "noHeader should win over headerText, got: " + out);
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