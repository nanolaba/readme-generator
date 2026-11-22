package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.widgets.BlockOpening;
import com.nanolaba.nrg.widgets.BlockWidget;
import com.nanolaba.nrg.widgets.DefaultWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_FILE_NAME_PATTERN;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_HEADER_TEXT;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_NO_HEADER;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

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

    @Test
    public void testHeaderTextEscapes() {
        assertEquals("\tX" + RN, header("\\tX"));
        assertEquals("\rX" + RN, header("\\rX"));
        assertEquals("\\X" + RN, header("\\\\X"));
        assertEquals(">X" + RN, header("\\>X"));
        assertEquals("\\qX" + RN, header("\\qX"), "an unknown escape stays as written");
        assertEquals("X\\" + RN, header("X\\"), "a trailing backslash stays as written");
    }

    private static String header(String headerText) {
        Generator generator = new Generator(new File("README.src.md"), "");
        generator.getConfig().getProperties().setProperty(PROPERTY_HEADER_TEXT, headerText);
        return generator.getResult("en").getContent().toString();
    }

    @Test
    public void testDefaultHeaderWithoutSourceFile() {
        String out = new Generator(null, "Body").getResult("en").getContent().toString();
        assertTrue(out.startsWith("<!-- This file was automatically generated by Nanolaba Readme Generator (NRG)"
                + " from the source template -->" + RN), out);
    }

    @Test
    public void testDefaultHeaderNamesTheTemplateWhenTheOutputHasNoDirectory() {
        // An empty pattern is an error that is reported elsewhere.
        Generator emptyPattern = new Generator(new File("docs/README.src.md"), "Body");
        emptyPattern.getConfig().getProperties().setProperty(PROPERTY_FILE_NAME_PATTERN, "");
        String out = emptyPattern.getResult("en").getContent().toString();
        assertTrue(out.contains(" from README.src.md -->"), out);

        // A pattern that climbs above the root of the file system leaves the output no parent.
        StringBuilder up = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            up.append("../");
        }
        Generator aboveRoot = new Generator(new File("docs/README.src.md"), "Body");
        aboveRoot.getConfig().getProperties().setProperty(PROPERTY_FILE_NAME_PATTERN, up.toString());
        out = aboveRoot.getResult("en").getContent().toString();
        assertTrue(out.contains(" from README.src.md -->"), out);
    }

    @Test
    public void testSourceMapNamesTheFirstUnescapedWidgetOfALine() throws IOException {
        NRGWidget stamp = new DefaultWidget() {
            @Override
            public String getName() {
                return "stamp";
            }

            @Override
            public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
                return "STAMP";
            }
        };
        Path src = write("README.src.md", "\\${widget:esc} ${widget:stamp}\n");
        GenerationResult result = mapped(new Generator(src.toFile(), StandardCharsets.UTF_8,
                Collections.singletonList(stamp)));

        assertEquals("README.src.md:1 widget stamp", originOf(result, "${widget:esc} STAMP").toString());
    }

    @Test
    public void testSourceMapCountsAHeadThatEndsInACarriageReturn() throws IOException {
        Path src = write("README.src.md", "Body\n");
        Generator generator = new Generator(src.toFile(), StandardCharsets.UTF_8) {
            @Override
            protected String generateHeadComment(String language) {
                return "<!-- generated -->\r";
            }
        };
        GenerationResult result = mapped(generator);

        assertEquals("head comment", originOf(result, "<!-- generated -->").toString());
        assertEquals("README.src.md:1", originOf(result, "Body").toString());
    }

    @Test
    public void testSourceMapStaysAlignedAfterAdjacentFrozenRegions() throws IOException {
        write("README.md", "<!--nrg.freeze id=\"a\"-->\ndisk a\n<!--/nrg.freeze-->\n" +
                "<!--nrg.freeze id=\"b\"-->\ndisk b1\ndisk b2\ndisk b3\n<!--/nrg.freeze-->\n");
        Path src = write("README.src.md",
                "<!--nrg.freeze id=\"a\"-->\n" + // 1
                        "placeholder a\n" +      // 2
                        "<!--/nrg.freeze-->\n" + // 3
                        "<!--nrg.freeze id=\"b\"-->\n" + // 4
                        "placeholder b\n" +      // 5
                        "<!--/nrg.freeze-->\n" + // 6
                        "After\n");              // 7
        GenerationResult result = mapped(new Generator(src.toFile(), StandardCharsets.UTF_8));

        assertEquals("README.src.md:4 (frozen region: content kept from the file on disk)",
                originOf(result, "disk b3").toString());
        assertEquals("README.src.md:6", originOf(result, "<!--/nrg.freeze-->", 2).toString());
        assertEquals("README.src.md:7", originOf(result, "After").toString());
    }

    @Test
    public void testSourceMapCopesWithABlockOpenerThatEmitsLineBreaks() throws IOException {
        // A property may hold line breaks; in a details summary they make the block pre-pass
        // emit more lines than it has origins for. Those lines are of unknown origin.
        Path src = write("README.src.md", "${widget:details(summary='${s}')}\ninside\n${widget:endDetails}");
        Generator generator = new Generator(src.toFile(), StandardCharsets.UTF_8);
        generator.getConfig().getProperties().setProperty("s", "one\ntwo\nthree");
        GenerationResult result = mapped(generator);

        assertEquals("README.src.md:1", originOf(result, "<summary>one").toString());
        assertEquals("unknown", originOf(result, "</details>").toString());
    }

    @Test
    public void testSourceMapIgnoresOriginsReportedOutsideTheRenderingOfALine() throws IOException {
        Path src = write("README.src.md", "${widget:early}\ninside\n${widget:endEarly}\n");
        GenerationResult result = mapped(new Generator(src.toFile(), StandardCharsets.UTF_8,
                Collections.<NRGWidget>singletonList(new EarlyReportingWidget())));

        assertEquals("README.src.md:1", originOf(result, "opened").toString());
        assertEquals("README.src.md:2", originOf(result, "inside").toString());
    }

    /** Reports origins from the block pre-pass, before any line is rendered. */
    private static final class EarlyReportingWidget extends BlockWidget {
        @Override
        public String getName() {
            return "early";
        }

        @Override
        protected BlockOpening onBlockOpen(Map<String, String> params, GeneratorConfig config, String language,
                                           boolean parentActive, int lineNo) {
            config.reportWidgetOrigins(Collections.singletonList(SourceOrigin.note("reported early")));
            return BlockOpening.of(Collections.singletonList("opened"), true);
        }

        @Override
        protected List<String> onBlockClose(int lineNo) {
            return Collections.emptyList();
        }

        @Override
        protected boolean isInlineForm(Map<String, String> params) {
            return false;
        }
    }

    private Path write(String name, String body) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, body.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private static GenerationResult mapped(Generator generator) {
        generator.getConfig().setCollectSourceMap(true);
        return generator.getResult("en");
    }

    private static SourceOrigin originOf(GenerationResult result, String line) {
        return originOf(result, line, 1);
    }

    /** Origin of the {@code occurrence}-th output line equal to {@code line}; checks one origin per line. */
    private static SourceOrigin originOf(GenerationResult result, String line, int occurrence) {
        String content = result.getContent().toString();
        String[] lines = content.split("\\R", -1);
        int count = lines.length - (content.endsWith("\n") || content.endsWith("\r") ? 1 : 0);
        assertEquals(count, result.getSourceMap().size(), "one origin per output line of " + content);
        int seen = 0;
        for (int i = 0; i < count; i++) {
            if (lines[i].equals(line) && ++seen == occurrence) {
                return result.getSourceMap().get(i);
            }
        }
        throw new AssertionError("no output line '" + line + "' in " + content);
    }
}