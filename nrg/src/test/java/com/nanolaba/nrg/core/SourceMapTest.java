package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The per-line source map behind {@code --locate}: every output line maps to the template line
 * (and import chain) that produced it, through ignore blocks, block widgets, imports with line
 * and region selection, wrapping, heading shifts, the table of contents and frozen regions —
 * and collecting it never changes the output.
 */
class SourceMapTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private Path write(String name, String body) throws IOException {
        Path p = tempDir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, body.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    private static final class Mapped {
        final String[] lines;
        final List<SourceOrigin> map;

        Mapped(GenerationResult r) {
            this.lines = r.getContent().toString().split("\\R", -1);
            this.map = r.getSourceMap();
        }

        /** Origin of the first output line equal to {@code text}. */
        SourceOrigin of(String text) {
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].equals(text)) {
                    return map.get(i);
                }
            }
            throw new AssertionError("no output line '" + text + "' in " + String.join("|", lines));
        }
    }

    private Mapped render(Path src, String language) throws IOException {
        Generator generator = new Generator(src.toFile(), StandardCharsets.UTF_8);
        generator.getConfig().setCollectSourceMap(true);
        GenerationResult result = generator.getResult(language);
        String content = result.getContent().toString();
        int lines = content.split("\\R", -1).length - (content.endsWith("\n") || content.endsWith("\r") ? 1 : 0);
        assertEquals(lines, result.getSourceMap().size(), "one origin per output line");

        Generator plain = new Generator(src.toFile(), StandardCharsets.UTF_8);
        assertEquals(plain.getResult(language).getContent().toString(), content, "collecting must not change the output");
        assertNull(plain.getResult(language).getSourceMap(), "no map unless asked for");
        return new Mapped(result);
    }

    private static void assertOrigin(SourceOrigin o, String file, int line) {
        assertNotNull(o.getFile(), o.toString());
        assertEquals(file, o.getFile().getName(), o.toString());
        assertEquals(line, o.getLine(), o.toString());
    }

    @Test
    public void testPlainLinesIgnoreBlocksAndLanguageMarkers() throws IOException {
        Path src = write("README.src.md",
                "<!--@nrg.languages=en,ru-->\n" +       // 1 (invisible)
                        "<!--nrg.ignore.begin-->\n" +   // 2
                        "draft\n" +                     // 3
                        "<!--nrg.ignore.end-->\n" +     // 4
                        "Hello<!--en-->\n" +            // 5
                        "Привет<!--ru-->\n" +           // 6
                        "Shared\n");                    // 7
        Mapped m = render(src, "ru");

        assertEquals("head comment", m.map.get(0).getNote());
        assertOrigin(m.of("Привет"), "README.src.md", 6);
        assertOrigin(m.of("Shared"), "README.src.md", 7);
    }

    @Test
    public void testBlockWidgetsKeepLinesInStep() throws IOException {
        Path src = write("README.src.md",
                "${widget:if(cond='false')}\n" +        // 1
                        "hidden\n" +                    // 2
                        "${widget:endIf}\n" +           // 3
                        "${widget:details(summary='S')}\n" + // 4
                        "inside\n" +                    // 5
                        "${widget:endDetails}\n" +      // 6
                        "after\n");                     // 7
        Mapped m = render(src, "en");

        assertOrigin(m.of("inside"), "README.src.md", 5);
        assertOrigin(m.of("after"), "README.src.md", 7);
        assertOrigin(m.of("</details>"), "README.src.md", 6);
    }

    @Test
    public void testImportsWithSelectionWrapAndNesting() throws IOException {
        write("docs/part.src.md", "# Part\n\nPart text\n${widget:import(path='deep.src.md')}\n");
        write("docs/deep.src.md", "Deep text\n");
        write("Code.java", "class A {\n    // nrg:begin:main\n    int x;\n    // nrg:end:main\n}\n");
        write("notes.txt", "one\ntwo\nthree\nfour\n");
        Path src = write("README.src.md",
                "Intro\n" +                                                              // 1
                        "${widget:import(path='docs/part.src.md', heading-offset='1')}\n" + // 2
                        "${widget:import(path='Code.java', region='main', wrap='true')}\n" + // 3
                        "${widget:import(path='notes.txt', lines='2-3', run-generator='false')}\n"); // 4
        Mapped m = render(src, "en");

        SourceOrigin part = m.of("Part text");
        assertOrigin(part, "part.src.md", 3);
        assertOrigin(part.getImportedFrom(), "README.src.md", 2);
        assertOrigin(m.of("## Part"), "part.src.md", 1);

        SourceOrigin deep = m.of("Deep text");
        assertOrigin(deep, "deep.src.md", 1);
        assertOrigin(deep.getImportedFrom(), "part.src.md", 4);
        assertOrigin(deep.getImportedFrom().getImportedFrom(), "README.src.md", 2);

        assertOrigin(m.of("int x;"), "Code.java", 3);
        SourceOrigin fence = m.of("```java");
        assertOrigin(fence, "README.src.md", 3);
        assertEquals("import", fence.getWidget());

        assertOrigin(m.of("two"), "notes.txt", 2);
        assertOrigin(m.of("three"), "notes.txt", 3);
    }

    @Test
    public void testWidgetOutputMapsToTheWidgetLine() throws IOException {
        Path src = write("README.src.md",
                "${widget:toc}\n" +   // 1
                        "## A\n" +    // 2
                        "## B\n");    // 3
        Mapped m = render(src, "en");

        SourceOrigin toc = m.of("- [B](#b)");
        assertOrigin(toc, "README.src.md", 1);
        assertEquals("toc", toc.getWidget());
        assertOrigin(m.of("## B"), "README.src.md", 3);
    }

    @Test
    public void testFrozenRegionKeepsLaterLinesAligned() throws IOException {
        write("README.md", "<!--nrg.freeze id=\"x\"-->\nkept 1\nkept 2\nkept 3\n<!--/nrg.freeze-->\n");
        Path src = write("README.src.md",
                "<!--nrg.freeze id=\"x\"-->\n" + // 1
                        "placeholder\n" +       // 2
                        "<!--/nrg.freeze-->\n" + // 3
                        "After\n");             // 4
        Mapped m = render(src, "en");

        SourceOrigin kept = m.of("kept 3");
        assertOrigin(kept, "README.src.md", 1);
        assertTrue(kept.getNote().contains("frozen region"), kept.toString());
        assertOrigin(m.of("After"), "README.src.md", 4);
    }
}
