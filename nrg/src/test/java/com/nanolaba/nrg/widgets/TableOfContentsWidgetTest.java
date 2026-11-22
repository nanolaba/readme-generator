package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.logging.MemoryLogger;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NoHeadCommentGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.ERROR;
import static com.nanolaba.logging.LogEntry.LogEntryLevel.WARN;
import static org.junit.jupiter.api.Assertions.*;

class TableOfContentsWidgetTest extends DefaultNRGTest {

    @AfterEach
    public void restoreDefaultLogger() {
        LOG.init();
    }

    @Test
    public void testTOCWidget() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru,fr-->\n" +
                        "${widget:tableOfContents}\n" +
                        "# MainHeader\n" +
                        "## AAA<!--en-->\n" +
                        "## ЯЯЯ<!--ru-->\n" +
                        "### aaa\n" +
                        "### ююю<!--ru-->\n" +
                        "### bbb\n" +
                        "### ccc<!--toc.ignore-->\n" +
                        "## BBB\n" +
                        "### ccc\n" +
                        "### ddd\n" +
                        "## CCC"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("1. [MainHeader]"));
        assertTrue(bodyEn.contains(

                "- [AAA](#aaa)" + RN +
                        "\t- [aaa](#aaa)" + RN +
                        "\t- [bbb](#bbb)" + RN +
                        "- [BBB](#bbb)" + RN +
                        "\t- [ccc](#ccc)" + RN +
                        "\t- [ddd](#ddd)" + RN +
                        "- [CCC](#ccc)"));
    }


    @Test
    public void testTOCWidget1() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru,fr-->\n" +
                        "## Header before TOC\n" +
                        "${widget:tableOfContents(title = \"TOC\", ordered = \"true\")}\n" +
                        "# MainHeader\n" +
                        "## AAA<!--en-->\n" +
                        "## ЯЯЯ<!--ru-->\n" +
                        "### aaa\n" +
                        "### ююю<!--ru-->\n" +
                        "### bbb\n" +
                        "## BBB\n" +
                        "### ccc\n" +
                        "### ddd\n" +
                        "## CCC"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("[MainHeader]"));
        assertFalse(bodyEn.contains("[Header before TOC]"));
        assertTrue(bodyEn.contains(
                "## TOC" + RN +
                        "1. [AAA](#aaa)" + RN +
                        "\t1. [aaa](#aaa)" + RN +
                        "\t2. [bbb](#bbb)" + RN +
                        "2. [BBB](#bbb)" + RN +
                        "\t1. [ccc](#ccc)" + RN +
                        "\t2. [ddd](#ddd)" + RN +
                        "3. [CCC](#ccc)"));
    }

    @Test
    public void testTOCWidget2() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru,fr-->\n" +
                        "someTextBeforeTOC\n" +
                        "#headerBeforeTOC\n" +
                        "##headerBeforeTOC2\n" +
                        "###headerBeforeTOC3\n" +
                        "${widget:tableOfContents(title = \"${en:'Table of contents', ru:'Содержание'}\", ordered = \"true\")}\n" +
                        "someTextBeforeHeader\n" +
                        "# MainHeader\n" +
                        "## AAA<!--en-->\n" +
                        "## IGNORED<!--en--><!--toc.ignore-->\n" +
                        "someText\n" +
                        "someText1<!--en-->\n" +
                        "someText2<!--ru-->\n" +
                        "## ЯЯЯ<!--ru-->\n" +
                        "### aaa\n" +
                        "### ююю<!--ru-->\n" +
                        "### bbb\n" +
                        "## BBB\n" +
                        "### ccc\n" +
                        "### ddd\n" +
                        "## CCC\n" +
                        "### ccc\n" +
                        "#### cccc\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertEquals(bodyEn,
                "someTextBeforeTOC" + RN +
                        "#headerBeforeTOC" + RN +
                        "##headerBeforeTOC2" + RN +
                        "###headerBeforeTOC3" + RN +
                        "## Table of contents" + RN +
                        "1. [AAA](#aaa)" + RN +
                        "	1. [aaa](#aaa)" + RN +
                        "	2. [bbb](#bbb)" + RN +
                        "2. [BBB](#bbb)" + RN +
                        "	1. [ccc](#ccc)" + RN +
                        "	2. [ddd](#ddd)" + RN +
                        "3. [CCC](#ccc)" + RN +
                        "	1. [ccc](#ccc)" + RN +
                        "		1. [cccc](#cccc)" + RN +
                        RN +
                        "someTextBeforeHeader" + RN +
                        "# MainHeader" + RN +
                        "## AAA" + RN +
                        "## IGNORED" + RN +
                        "someText" + RN +
                        "someText1" + RN +
                        "### aaa" + RN +
                        "### bbb" + RN +
                        "## BBB" + RN +
                        "### ccc" + RN +
                        "### ddd" + RN +
                        "## CCC" + RN +
                        "### ccc" + RN +
                        "#### cccc" + RN
        );
    }

    @Test
    public void testTOCWidget3() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru,fr-->\n" +
                        "${widget:tableOfContents(title = \"${en:'Table of contents', ru:'Содержание'}\", ordered = \"true\")}\n" +
                        "## A<!--en-->\n" +
                        "## B<!--en-->\n" +
                        "## Б<!--ru-->\n" +
                        "## ${en:'ENG', ru:'РУС'}\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertEquals(bodyEn,
                "## Table of contents" + RN +
                        "1. [A](#a)" + RN +
                        "2. [B](#b)" + RN +
                        "3. [ENG](#eng)" + RN +
                        "" + RN +
                        "## A" + RN +
                        "## B" + RN +
                        "## ENG" + RN
        );
    }

    @Test
    @DisplayName("No message 'Unknown widget name: tableOfContents' in the console")
    public void testTOCWidget4() {
        AtomicBoolean logged = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);

        LOG.init(entry -> {
            logged.set(true);
            String msg = entry.getFormattedMessage();
            System.out.println(msg);
            if (msg.contains("Unknown widget name: tableOfContents")) {
                failed.set(true);
            }
        });

        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru,fr-->\n" +
                        "${widget:tableOfContents(title = \"${en:'Table of contents', ru:'Содержание'}\", ordered = \"true\")}\n" +
                        "## A<!--en-->\n" +
                        "## B<!--en-->\n" +
                        "## Б<!--ru-->\n" +
                        "## ${en:'ENG', ru:'РУС'}\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertEquals(bodyEn,
                "## Table of contents" + RN +
                        "1. [A](#a)" + RN +
                        "2. [B](#b)" + RN +
                        "3. [ENG](#eng)" + RN +
                        "" + RN +
                        "## A" + RN +
                        "## B" + RN +
                        "## ENG" + RN
        );

        if (!logged.get()) {
            fail("Logger is turned off");
        }

        if (failed.get()) {
            fail("The message has been found");
        }
    }

    @Test
    public void testTOCMaxDepth() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", max-depth = \"3\")}\n" +
                        "# MainHeader\n" +
                        "## AAA\n" +
                        "### aaa\n" +
                        "#### aaaa\n" +
                        "##### aaaaa\n" +
                        "## BBB\n" +
                        "### bbb\n" +
                        "#### bbbb\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [AAA](#aaa)"));
        assertTrue(bodyEn.contains("\t1. [aaa](#aaa)"));
        assertTrue(bodyEn.contains("\t1. [bbb](#bbb)"));
        assertFalse(bodyEn.contains("[aaaa]"));
        assertFalse(bodyEn.contains("[aaaaa]"));
        assertFalse(bodyEn.contains("[bbbb]"));
        assertFalse(bodyEn.contains("[MainHeader]"));
    }

    @Test
    public void testTOCMinDepthIncludesH1() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", min-depth = \"1\", max-depth = \"2\")}\n" +
                        "# MainHeader\n" +
                        "## AAA\n" +
                        "### aaa\n" +
                        "## BBB\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [MainHeader](#mainheader)"));
        assertTrue(bodyEn.contains("\t1. [AAA](#aaa)"));
        assertTrue(bodyEn.contains("\t2. [BBB](#bbb)"));
        assertFalse(bodyEn.contains("[aaa]"));
    }

    @Test
    public void testTOCMinDepthSkipsShallowHeaders() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", min-depth = \"3\")}\n" +
                        "# MainHeader\n" +
                        "## AAA\n" +
                        "### aaa\n" +
                        "### bbb\n" +
                        "## BBB\n" +
                        "### ccc\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("[MainHeader]"));
        assertFalse(bodyEn.contains("[AAA]"));
        assertFalse(bodyEn.contains("[BBB]"));
        assertTrue(bodyEn.contains("1. [aaa](#aaa)"));
        assertTrue(bodyEn.contains("2. [bbb](#bbb)"));
        assertTrue(bodyEn.contains("1. [ccc](#ccc)"));
        assertFalse(bodyEn.contains("\t"));
    }

    @Test
    public void testTOCRespectsIgnoreWithDepthParams() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", min-depth = \"1\", max-depth = \"3\")}\n" +
                        "# MainHeader<!--toc.ignore-->\n" +
                        "## AAA\n" +
                        "### aaa<!--toc.ignore-->\n" +
                        "### bbb\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("[MainHeader]"));
        assertFalse(bodyEn.contains("[aaa]"));
        assertTrue(bodyEn.contains("[AAA]"));
        assertTrue(bodyEn.contains("[bbb]"));
    }

    @Test
    public void testTOCInvalidDepthFallsBackToDefaults() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", max-depth = \"9\", min-depth = \"zero\")}\n" +
                        "# MainHeader\n" +
                        "## AAA\n" +
                        "### aaa\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("[MainHeader]"));
        assertTrue(bodyEn.contains("1. [AAA](#aaa)"));
        assertTrue(bodyEn.contains("\t1. [aaa](#aaa)"));
    }

    @Test
    public void testTOCMinItemsThresholdMet() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", ordered = \"true\", min-items = \"3\")}\n" +
                        "## AAA\n" +
                        "## BBB\n" +
                        "## CCC\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("## TOC"));
        assertTrue(bodyEn.contains("1. [AAA](#aaa)"));
        assertTrue(bodyEn.contains("2. [BBB](#bbb)"));
        assertTrue(bodyEn.contains("3. [CCC](#ccc)"));
    }

    @Test
    public void testTOCMinItemsThresholdNotMetProducesNothing() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", ordered = \"true\", min-items = \"3\")}\n" +
                        "## AAA\n" +
                        "## BBB\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("## TOC"));
        assertFalse(bodyEn.contains("[AAA]"));
        assertFalse(bodyEn.contains("[BBB]"));
        assertTrue(bodyEn.contains("## AAA"));
        assertTrue(bodyEn.contains("## BBB"));
    }

    @Test
    public void testTOCMinItemsCountsAfterTocIgnore() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", min-items = \"3\")}\n" +
                        "## AAA\n" +
                        "## BBB<!--toc.ignore-->\n" +
                        "## CCC\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("## TOC"));
        assertFalse(bodyEn.contains("[AAA](#aaa)"));
        assertFalse(bodyEn.contains("[CCC](#ccc)"));
    }

    @Test
    public void testTOCMinItemsCountsAfterDepthFilter() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", min-items = \"3\", max-depth = \"2\")}\n" +
                        "## AAA\n" +
                        "## BBB\n" +
                        "### ccc\n" +
                        "### ddd\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("## TOC"));
        assertFalse(bodyEn.contains("[AAA](#aaa)"));
        assertFalse(bodyEn.contains("[BBB](#bbb)"));
    }

    @Test
    public void testTOCMinItemsInvalidFallsBackToDefault() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", min-items = \"zero\")}\n" +
                        "## AAA\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("## TOC"));
        assertTrue(bodyEn.contains("[AAA](#aaa)"));
    }

    @Test
    public void testTOCMinItemsZeroFallsBackToDefault() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", min-items = \"0\")}\n" +
                        "## AAA\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("## TOC"));
        assertTrue(bodyEn.contains("[AAA](#aaa)"));
    }

    @Test
    public void testTOCAnchorStyleGithubIsDefault() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents}\n" +
                        "## Hello World\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("[Hello World](#hello-world)"));
    }

    @Test
    public void testTOCAnchorStyleGitlabKeepsUnderscoresAndRepeatedHyphens() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(anchor-style = \"gitlab\")}\n" +
                        "## snake_case name\n" +
                        "## a -- b\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("[snake_case name](#snake_case-name)"));
        assertTrue(bodyEn.contains("[a -- b](#a----b)"));
    }

    @Test
    public void testTOCAnchorStyleGithubCollapsesConsecutiveHyphens() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents}\n" +
                        "## a -- b\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("[a -- b](#a-b)"));
    }

    @Test
    public void testTOCAnchorStyleBitbucketUsesPrefix() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(anchor-style = \"bitbucket\")}\n" +
                        "## Hello World\n" +
                        "## snake_case name\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("[Hello World](#markdown-header-hello-world)"));
        assertTrue(bodyEn.contains("[snake_case name](#markdown-header-snakecase-name)"));
    }

    @Test
    public void testTOCAnchorStyleInvalidProducesEmptyOutput() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(title = \"TOC\", anchor-style = \"confluence\")}\n" +
                        "## Hello\n" +
                        "## World\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("## TOC"));
        assertFalse(bodyEn.contains("[Hello](#"));
        assertFalse(bodyEn.contains("[World](#"));
        assertTrue(bodyEn.contains("## Hello"));
        assertTrue(bodyEn.contains("## World"));
    }

    @Test
    public void testTOCAnchorStylePreservesUnicodeForGitlab() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(anchor-style = \"gitlab\")}\n" +
                        "## Раздел\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("[Раздел](#раздел)"));
    }

    @Test
    public void testTOCMinGreaterThanMaxFallsBackToDefaults() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", min-depth = \"4\", max-depth = \"2\")}\n" +
                        "# MainHeader\n" +
                        "## AAA\n" +
                        "### aaa\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertFalse(bodyEn.contains("[MainHeader]"));
        assertTrue(bodyEn.contains("1. [AAA](#aaa)"));
        assertTrue(bodyEn.contains("\t1. [aaa](#aaa)"));
    }

    @Test
    public void testTOCIgnoresHashLinesInsideFencedCodeBlocks() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "## B\n" +
                        "## C\n" +
                        "\n" +
                        "```bash\n" +
                        "# This is a bash comment, not a heading\n" +
                        "ls\n" +
                        "```\n" +
                        "\n" +
                        "## D\n" +
                        "## E\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [A](#a)"));
        assertTrue(bodyEn.contains("2. [B](#b)"));
        assertTrue(bodyEn.contains("3. [C](#c)"));
        assertTrue(bodyEn.contains("4. [D](#d)"));
        assertTrue(bodyEn.contains("5. [E](#e)"));
    }

    @Test
    public void testTOCSeesClosingFenceThatCarriesLanguageMarker() {
        // Regression for #56: the scan runs over non-root output, where language markers are
        // still present. "```<!--en-->" was not recognised as a closing fence, so the fence
        // state flipped and every later heading was treated as code.
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "```java<!--en-->\n" +
                        "// english<!--en-->\n" +
                        "```<!--en-->\n" +
                        "```java<!--ru-->\n" +
                        "// russian<!--ru-->\n" +
                        "``` <!-- ru -->\n" +
                        "\n" +
                        "```\n" +
                        "# not a heading\n" +
                        "```\n" +
                        "## B\n" +
                        "### B1\n" +
                        "## C\n"
        );

        for (String language : new String[]{"en", "ru"}) {
            String body = generator.getResult(language).getContent().toString();
            assertTrue(body.contains("1. [A](#a)"), body);
            assertTrue(body.contains("2. [B](#b)"), body);
            assertTrue(body.contains("1. [B1](#b1)"), body);
            assertTrue(body.contains("3. [C](#c)"), body);
            assertFalse(body.contains("[not a heading]"), body);
        }
    }

    @Test
    public void testTOCSurvivesWrappedImportOnLineWithLanguageMarker(@TempDir Path dir) throws IOException {
        // The exact shape reported in #56: the widget's multi-line output replaces the tag in
        // place, so the marker ends up glued to the closing fence the import emitted.
        String sample = "public class Sample {\n" +
                "    // nrg:begin:demo\n" +
                "    void demo() {}\n" +
                "    // nrg:end:demo\n" +
                "}\n";
        Files.write(dir.resolve("Sample.java"), sample.getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("SampleRu.java"), sample.replace("demo()", "demoRu()").getBytes(StandardCharsets.UTF_8));

        Generator generator = new NoHeadCommentGenerator(dir.resolve("README.src.md").toFile(),
                "<!--@nrg.languages=en,ru-->\n" +
                        "# Title\n" +
                        "${widget:tableOfContents(title='Contents')}\n" +
                        "## Section one\n" +
                        "${widget:import(path='Sample.java', region='demo', wrap='true')}<!--en-->\n" +
                        "${widget:import(path='SampleRu.java', region='demo', wrap='true')}<!--ru-->\n" +
                        "\n" +
                        "```\n" +
                        "some output\n" +
                        "```\n" +
                        "### Section one point one\n" +
                        "## Section two\n" +
                        "### Section two point one\n" +
                        "## Section three\n"
        );

        String en = generator.getResult("en").getContent().toString();
        String ru = generator.getResult("ru").getContent().toString();
        assertTrue(en.contains("void demo() {}"), en);
        assertTrue(ru.contains("void demoRu() {}"), ru);
        for (String body : new String[]{en, ru}) {
            assertTrue(body.contains("- [Section one](#section-one)"), body);
            assertTrue(body.contains("\t- [Section one point one](#section-one-point-one)"), body);
            assertTrue(body.contains("- [Section two](#section-two)"), body);
            assertTrue(body.contains("\t- [Section two point one](#section-two-point-one)"), body);
            assertTrue(body.contains("- [Section three](#section-three)"), body);
        }
    }

    @Test
    public void testTOCIgnoresHashLinesInsideTildeFencedCodeBlocks() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "## B\n" +
                        "\n" +
                        "~~~\n" +
                        "# Hash inside tilde fence\n" +
                        "~~~\n" +
                        "\n" +
                        "## C\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [A](#a)"));
        assertTrue(bodyEn.contains("2. [B](#b)"));
        assertTrue(bodyEn.contains("3. [C](#c)"));
    }

    @Test
    public void testTOCIgnoresNestedFences() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "\n" +
                        "````markdown\n" +
                        "```bash\n" +
                        "# nested-but-still-inside\n" +
                        "```\n" +
                        "# also-still-inside\n" +
                        "````\n" +
                        "\n" +
                        "## B\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [A](#a)"));
        assertTrue(bodyEn.contains("2. [B](#b)"));
    }

    @Test
    public void testTOCIgnoresHashLinesInsideIndentedCodeBlocks() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "## B\n" +
                        "\n" +
                        "    # this is indented code, not a heading\n" +
                        "    ls\n" +
                        "\n" +
                        "## C\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [A](#a)"));
        assertTrue(bodyEn.contains("2. [B](#b)"));
        assertTrue(bodyEn.contains("3. [C](#c)"));
    }

    @Test
    public void testTOCIgnoresHashLinesInsideInlineBackticks() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "## B\n" +
                        "Use `# inline` to comment, not as a heading.\n" +
                        "## C\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [A](#a)"));
        assertTrue(bodyEn.contains("2. [B](#b)"));
        assertTrue(bodyEn.contains("3. [C](#c)"));
    }

    @Test
    public void testTOCIgnoresEscapedHeadings() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## A\n" +
                        "## B\n" +
                        "\\# Not a heading\n" +
                        "## C\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [A](#a)"));
        assertTrue(bodyEn.contains("2. [B](#b)"));
        assertTrue(bodyEn.contains("3. [C](#c)"));
        assertFalse(bodyEn.contains("[Not a heading]"));
    }

    @Test
    public void testTOCNumberingMonotonicAcrossManyFences() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\")}\n" +
                        "## H1\n" +
                        "```\n" +
                        "# c1\n" +
                        "```\n" +
                        "## H2\n" +
                        "```\n" +
                        "# c2\n" +
                        "```\n" +
                        "## H3\n"
        );

        String bodyEn = generator.getResult("en").getContent().toString();
        LOG.info(bodyEn);

        assertTrue(bodyEn.contains("1. [H1](#h1)"));
        assertTrue(bodyEn.contains("2. [H2](#h2)"));
        assertTrue(bodyEn.contains("3. [H3](#h3)"));
    }

    @Test
    public void testTOCNumberingStyleDefaultIsByteIdentical() {
        String src = "<!--@nrg.languages=en-->\n" +
                "${widget:tableOfContents(ordered = \"true\", numbering-style = \"default\")}\n" +
                "## A\n" +
                "### a\n" +
                "## B\n";
        String srcNoStyle = "<!--@nrg.languages=en-->\n" +
                "${widget:tableOfContents(ordered = \"true\")}\n" +
                "## A\n" +
                "### a\n" +
                "## B\n";

        String withStyle = new NoHeadCommentGenerator(new File("README.src.md"), src)
                .getResult("en").getContent().toString();
        String withoutStyle = new NoHeadCommentGenerator(new File("README.src.md"), srcNoStyle)
                .getResult("en").getContent().toString();

        assertEquals(withoutStyle, withStyle, "default numbering-style must be byte-identical to omitting the parameter");
    }

    @Test
    public void testTOCNumberingStyleDotted() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", min-depth = \"1\")}\n" +
                        "# H1\n" +
                        "## H1.1\n" +
                        "### H1.1.1\n" +
                        "#### H1.1.1.1\n" +
                        "## H1.2\n" +
                        "# H2\n" +
                        "## H2.1\n"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- 1 [H1](#h1)"));
        assertTrue(body.contains("\t- 1.1 [H1.1](#h11)"));
        assertTrue(body.contains("\t\t- 1.1.1 [H1.1.1](#h111)"));
        assertTrue(body.contains("\t\t\t- 1.1.1.1 [H1.1.1.1](#h1111)"));
        assertTrue(body.contains("\t- 1.2 [H1.2](#h12)"));
        assertTrue(body.contains("- 2 [H2](#h2)"));
        assertTrue(body.contains("\t- 2.1 [H2.1](#h21)"));
    }

    @Test
    public void testTOCNumberingStyleLegal() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"legal\", min-depth = \"1\")}\n" +
                        "# H1\n" +
                        "## H1.1\n" +
                        "### H1.1.1\n" +
                        "## H1.2\n" +
                        "# H2\n"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- 1. [H1](#h1)"));
        assertTrue(body.contains("\t- 1.1. [H1.1](#h11)"));
        assertTrue(body.contains("\t\t- 1.1.1. [H1.1.1](#h111)"));
        assertTrue(body.contains("\t- 1.2. [H1.2](#h12)"));
        assertTrue(body.contains("- 2. [H2](#h2)"));
    }

    @Test
    public void testTOCNumberingStyleAppendix() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"appendix\", min-depth = \"1\")}\n" +
                        "# Alpha\n" +
                        "## A.1\n" +
                        "### A.1.1\n" +
                        "## A.2\n" +
                        "# Bravo\n" +
                        "## B.1\n"
        );

        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- A [Alpha](#alpha)"));
        assertTrue(body.contains("\t- A.1 [A.1](#a1)"));
        assertTrue(body.contains("\t\t- A.1.1 [A.1.1](#a11)"));
        assertTrue(body.contains("\t- A.2 [A.2](#a2)"));
        assertTrue(body.contains("- B [Bravo](#bravo)"));
        assertTrue(body.contains("\t- B.1 [B.1](#b1)"));
    }

    @Test
    public void testTOCNumberingStyleAppendixWithStart() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"appendix\", start = \"C\", min-depth = \"1\")}\n" +
                        "# X\n" +
                        "## X.1\n" +
                        "# Y\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- C [X](#x)"));
        assertTrue(body.contains("\t- C.1 [X.1](#x1)"));
        assertTrue(body.contains("- D [Y](#y)"));
    }

    @Test
    public void testTOCNumberingStyleArabicFlat() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"arabic\", min-depth = \"1\")}\n" +
                        "# H1\n" +
                        "## H1.1\n" +
                        "### H1.1.1\n" +
                        "## H1.2\n" +
                        "# H2\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        // No tabs anywhere on the rendered TOC lines, single global counter
        assertTrue(body.contains("- 1 [H1](#h1)"));
        assertTrue(body.contains("- 2 [H1.1](#h11)"));
        assertTrue(body.contains("- 3 [H1.1.1](#h111)"));
        assertTrue(body.contains("- 4 [H1.2](#h12)"));
        assertTrue(body.contains("- 5 [H2](#h2)"));
        assertFalse(body.contains("\t- "));
    }

    @Test
    public void testTOCNumberingStyleRomanLowerAndUpper() {
        String src = "<!--@nrg.languages=en-->\n" +
                "${widget:tableOfContents(ordered = \"true\", numbering-style = \"%STYLE%\")}\n" +
                "## A\n" +
                "## B\n" +
                "## C\n" +
                "## D\n";

        String lower = new NoHeadCommentGenerator(new File("README.src.md"),
                src.replace("%STYLE%", "roman")).getResult("en").getContent().toString();
        assertTrue(lower.contains("- i [A](#a)"));
        assertTrue(lower.contains("- ii [B](#b)"));
        assertTrue(lower.contains("- iii [C](#c)"));
        assertTrue(lower.contains("- iv [D](#d)"));

        String upper = new NoHeadCommentGenerator(new File("README.src.md"),
                src.replace("%STYLE%", "roman-upper")).getResult("en").getContent().toString();
        assertTrue(upper.contains("- I [A](#a)"));
        assertTrue(upper.contains("- II [B](#b)"));
        assertTrue(upper.contains("- III [C](#c)"));
        assertTrue(upper.contains("- IV [D](#d)"));
    }

    @Test
    public void testTOCNumberingStyleAlphaLowerAndUpper() {
        String src = "<!--@nrg.languages=en-->\n" +
                "${widget:tableOfContents(ordered = \"true\", numbering-style = \"%STYLE%\")}\n" +
                "## A\n" +
                "## B\n" +
                "## C\n";

        String lower = new NoHeadCommentGenerator(new File("README.src.md"),
                src.replace("%STYLE%", "alpha")).getResult("en").getContent().toString();
        assertTrue(lower.contains("- a [A](#a)"));
        assertTrue(lower.contains("- b [B](#b)"));
        assertTrue(lower.contains("- c [C](#c)"));

        String upper = new NoHeadCommentGenerator(new File("README.src.md"),
                src.replace("%STYLE%", "alpha-upper")).getResult("en").getContent().toString();
        assertTrue(upper.contains("- A [A](#a)"));
        assertTrue(upper.contains("- B [B](#b)"));
        assertTrue(upper.contains("- C [C](#c)"));
    }

    @Test
    public void testTOCNumberingStyleDottedWithStart() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", start = \"5\", min-depth = \"1\")}\n" +
                        "# H1\n" +
                        "## H1.1\n" +
                        "# H2\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- 5 [H1](#h1)"));
        assertTrue(body.contains("\t- 5.1 [H1.1](#h11)"));
        assertTrue(body.contains("- 6 [H2](#h2)"));
    }

    @Test
    public void testTOCNumberingStyleDottedWithMinDepth() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", min-depth = \"3\")}\n" +
                        "# Top\n" +
                        "## Mid\n" +
                        "### a\n" +
                        "#### a1\n" +
                        "### b\n" +
                        "## Mid2\n" +
                        "### c\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertFalse(body.contains("[Top]"));
        assertFalse(body.contains("[Mid]"));
        assertFalse(body.contains("[Mid2]"));
        assertTrue(body.contains("- 1 [a](#a)"));
        assertTrue(body.contains("\t- 1.1 [a1](#a1)"));
        assertTrue(body.contains("- 2 [b](#b)"));
        assertTrue(body.contains("- 3 [c](#c)"));
    }

    @Test
    public void testTOCNumberingStyleDottedRespectsTocIgnore() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\")}\n" +
                        "## A\n" +
                        "## B<!--toc.ignore-->\n" +
                        "## C\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertFalse(body.contains("[B]"));
        assertTrue(body.contains("- 1 [A](#a)"));
        assertTrue(body.contains("- 2 [C](#c)"));
    }

    @Test
    public void testTOCNumberingStyleInvalidFallsBackToDefault() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dottd\")}\n" +
                        "## A\n" +
                        "## B\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("1. [A](#a)"));
        assertTrue(body.contains("2. [B](#b)"));
    }

    @Test
    public void testTOCNumberingStyleInvalidStartFallsBackToNaturalFirst() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", start = \"A\")}\n" +
                        "## A\n" +
                        "## B\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- 1 [A](#a)"));
        assertTrue(body.contains("- 2 [B](#b)"));
    }

    @Test
    public void testTOCNumberingStyleIgnoredWhenNotOrdered() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(numbering-style = \"dotted\")}\n" +
                        "## A\n" +
                        "## B\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- [A](#a)"));
        assertTrue(body.contains("- [B](#b)"));
        assertFalse(body.contains("- 1 [A]"));
        assertFalse(body.contains("- 1.1"));
    }

    @Test
    public void testTOCNumberingStyleDottedSurvivesFences() {
        Generator generator = new NoHeadCommentGenerator(new File("README.src.md"),
                "<!--@nrg.languages=en-->\n" +
                        "${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\")}\n" +
                        "## A\n" +
                        "```\n" +
                        "# c1\n" +
                        "```\n" +
                        "## B\n" +
                        "```\n" +
                        "# c2\n" +
                        "```\n" +
                        "## C\n"
        );
        String body = generator.getResult("en").getContent().toString();
        LOG.info(body);

        assertTrue(body.contains("- 1 [A](#a)"));
        assertTrue(body.contains("- 2 [B](#b)"));
        assertTrue(body.contains("- 3 [C](#c)"));
    }

    @Test
    public void testTocAliasRendersSameAsPrimaryName() {
        String bodyPrimary = "# Top\n" +
                "${widget:tableOfContents}\n" +
                "## Chapter A\n" +
                "## Chapter B\n";
        String bodyAlias = bodyPrimary.replace("widget:tableOfContents", "widget:toc");

        String outPrimary = new Generator(new File("README.src.md"), bodyPrimary)
                .getResult("en").getContent().toString();
        String outAlias = new Generator(new File("README.src.md"), bodyAlias)
                .getResult("en").getContent().toString();

        assertEquals(outPrimary, outAlias,
                "primary and alias must render byte-identically");
        assertTrue(outPrimary.contains("Chapter A"), outPrimary);
        assertTrue(outPrimary.contains("Chapter B"), outPrimary);
    }

    private static String renderEn(String source) {
        return new NoHeadCommentGenerator(new File("README.src.md"), source).getResult("en").getContent().toString();
    }

    private static Path write(Path dir, String name, String content) throws IOException {
        return Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testTOCWithoutNumberingParametersLogsNoWarning() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents}\n## A\n## B\n");

            assertTrue(body.contains("- [A](#a)" + RN + "- [B](#b)"), body);
            log.assertNoneAtOrAbove(WARN);
        }
    }

    @Test
    public void testTOCNumberingStyleWithoutOrderedWarnsAndKeepsNesting() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(numbering-style = \"dotted\")}\n## A\n### A1\n## B\n");

            assertTrue(body.contains("- [A](#a)" + RN + "\t- [A1](#a1)" + RN + "- [B](#b)" + RN), body);
            log.assertLogged(WARN, "numbering-style='dotted' has no effect without ordered='true'");
        }
    }

    @Test
    public void testTOCNumberingStyleWithOrderedDoesNotWarn() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\")}\n## A\n");

            assertTrue(body.contains("- 1 [A](#a)"), body);
            log.assertNoneAtOrAbove(WARN);
        }
    }

    @Test
    public void testTOCExplicitDefaultNumberingStyleIsNotAnError() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"default\")}\n## A\n");

            assertTrue(body.contains("1. [A](#a)"), body);
            log.assertNoneAtOrAbove(WARN);
        }
    }

    @Test
    public void testTOCUnknownNumberingStyleLogsError() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"dottd\")}\n## A\n");

            log.assertLogged(ERROR, "unknown numbering-style 'dottd'");
        }
    }

    @Test
    public void testTOCExplicitGithubAnchorStyleIsCaseInsensitive() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(anchor-style = \" GitHub \")}\n## snake_case name\n");

            assertTrue(body.contains("- [snake_case name](#snake-case-name)"), body);
            log.assertNoneAtOrAbove(WARN);
        }
    }

    @Test
    public void testTOCUnknownAnchorStyleLogsError() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            renderEn("${widget:tableOfContents(anchor-style = \"confluence\")}\n## A\n");

            log.assertLogged(ERROR, "unknown anchor-style 'confluence' (expected github|gitlab|bitbucket)");
        }
    }

    @Test
    public void testTOCInvalidDepthsLogErrors() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            renderEn("${widget:tableOfContents(min-depth = \"zero\", max-depth = \"9\")}\n## A\n");

            log.assertLogged(ERROR, "min-depth must be an integer, got 'zero'");
            log.assertLogged(ERROR, "max-depth must be between 1 and 6, got '9'");
        }
    }

    @Test
    public void testTOCOutOfRangeMinDepthKeepsDefault() {
        String body = renderEn("${widget:tableOfContents(min-depth = \"0\")}\n# Top\n## A\n");

        assertFalse(body.contains("[Top]"), body);
        assertTrue(body.contains("- [A](#a)"), body);
    }

    @Test
    public void testTOCDepthSixIsAccepted() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(min-depth = \"6\", max-depth = \"6\")}\n## A\n###### Deep\n");

            assertFalse(body.contains("[A]"), body);
            assertTrue(body.contains("- [Deep](#deep)"), body);
            log.assertNoneAtOrAbove(WARN);
        }
    }

    @Test
    public void testTOCMinDepthAboveMaxDepthLogsError() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            renderEn("${widget:tableOfContents(min-depth = \"4\", max-depth = \"2\")}\n## A\n");

            log.assertLogged(ERROR, "min-depth (4) must not exceed max-depth (2); using defaults");
        }
    }

    @Test
    public void testTOCMinItemsOneIsAccepted() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(min-items = \"1\")}\n## A\n");

            assertTrue(body.contains("- [A](#a)"), body);
            log.assertNoneAtOrAbove(WARN);
        }
    }

    @Test
    public void testTOCInvalidMinItemsLogErrors() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            renderEn("${widget:tableOfContents(min-items = \"0\")}\n## A\n");
            renderEn("${widget:tableOfContents(min-items = \"zero\")}\n## A\n");

            log.assertLogged(ERROR, "min-items must be >= 1, got '0'");
            log.assertLogged(ERROR, "min-items must be an integer, got 'zero'");
        }
    }

    @Test
    public void testTOCStartWithDefaultNumberingStyleWarnsAndIsIgnored() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(ordered = \"true\", start = \"5\")}\n## A\n## B\n");

            assertTrue(body.contains("1. [A](#a)" + RN + "2. [B](#b)"), body);
            log.assertLogged(WARN, "'start' is ignored when numbering-style is 'default'");
        }
    }

    @Test
    public void testTOCInvalidStartLogsError() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            String body = renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"roman-upper\", start = \"5\")}\n## A\n");

            assertTrue(body.contains("- I [A](#a)"), body);
            log.assertLogged(ERROR, "invalid 'start' value '5' for numbering-style 'roman-upper'; falling back to natural first value");
        }
    }

    @Test
    public void testTOCNaturalFirstStartIsAccepted() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertTrue(renderEn(flatToc("arabic", "1")).contains("- 1 [A](#a)" + RN + "- 2 [B](#b)"));
            assertTrue(renderEn(flatToc("alpha", "a")).contains("- a [A](#a)" + RN + "- b [B](#b)"));
            assertTrue(renderEn(flatToc("roman", "i")).contains("- i [A](#a)" + RN + "- ii [B](#b)"));
            log.assertNoneAtOrAbove(WARN);
        }
    }

    private static String flatToc(String style, String start) {
        return "${widget:tableOfContents(ordered = \"true\", numbering-style = \"" + style + "\", start = \"" + start + "\")}\n" +
                "## A\n" +
                "## B\n";
    }

    @Test
    public void testTOCNumberingStyleArabicWithStart() {
        assertTrue(renderEn(flatToc("arabic", "3")).contains("- 3 [A](#a)" + RN + "- 4 [B](#b)"));
    }

    @Test
    public void testTOCNumberingStyleAlphaWithStart() {
        assertTrue(renderEn(flatToc("alpha", "z")).contains("- z [A](#a)" + RN + "- aa [B](#b)"));
        assertTrue(renderEn(flatToc("alpha", "y")).contains("- y [A](#a)" + RN + "- z [B](#b)"));
        assertTrue(renderEn(flatToc("alpha-upper", "AA")).contains("- AA [A](#a)" + RN + "- AB [B](#b)"));
        assertTrue(renderEn(flatToc("alpha-upper", "az")).contains("- AZ [A](#a)" + RN + "- BA [B](#b)"));
    }

    @Test
    public void testTOCNumberingStyleRomanWithStart() {
        assertTrue(renderEn(flatToc("roman", "iv")).contains("- iv [A](#a)" + RN + "- v [B](#b)"));
        assertTrue(renderEn(flatToc("roman", "VI")).contains("- vi [A](#a)" + RN + "- vii [B](#b)"));
        assertTrue(renderEn(flatToc("roman-upper", "xlix")).contains("- XLIX [A](#a)" + RN + "- L [B](#b)"));
    }

    @Test
    public void testTOCRomanCountersPastTheLargestNumeralFallBackToDigits() {
        assertTrue(renderEn(flatToc("roman-upper", "MMMCMXCIX")).contains("- MMMCMXCIX [A](#a)" + RN + "- 4000 [B](#b)"));
    }

    @Test
    public void testTOCRejectsNonCanonicalStartValues() {
        String[][] cases = {
                {"roman", "IIII"}, {"roman", "IM"}, {"roman", "MMMM"}, {"roman", "0"}, {"roman", "x1"},
                {"alpha", "a1"}, {"alpha", "{"}, {"alpha", "`"}
        };
        for (String[] c : cases) {
            try (MemoryLogger log = MemoryLogger.capture()) {
                String body = renderEn(flatToc(c[0], c[1]));

                String first = c[0].equals("roman") ? "i" : "a";
                assertTrue(body.contains("- " + first + " [A](#a)"), c[1] + ": " + body);
                log.assertLogged(ERROR, "invalid 'start' value '" + c[1] + "' for numbering-style '" + c[0] + "'");
            }
        }
    }

    @Test
    public void testTOCNumberingStyleDottedCountsSiblingsAtMaxDepth() {
        String body = renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", max-depth = \"3\")}\n" +
                "## A\n" +
                "### A1\n" +
                "### A2\n" +
                "#### hidden\n" +
                "## B\n");

        assertTrue(body.contains("- 1 [A](#a)" + RN +
                "\t- 1.1 [A1](#a1)" + RN +
                "\t- 1.2 [A2](#a2)" + RN +
                "- 2 [B](#b)" + RN), body);
    }

    @Test
    public void testTOCNumberingStyleDottedIgnoresHeadingsBelowMaxDepth() {
        // The hidden heading must not count as the predecessor of B, or B would read 1.2.
        String body = renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", min-depth = \"1\", max-depth = \"2\")}\n" +
                "# A\n" +
                "### hidden\n" +
                "## B\n");

        assertTrue(body.contains("- 1 [A](#a)" + RN + "\t- 1.1 [B](#b)" + RN), body);
        assertFalse(body.contains("[hidden]"), body);
    }

    @Test
    public void testTOCNumberingStyleDottedJumpsOverALevel() {
        String body = renderEn("${widget:tableOfContents(ordered = \"true\", numbering-style = \"dotted\", min-depth = \"1\")}\n" +
                "# A\n" +
                "### deep\n" +
                "## B\n" +
                "# C\n");

        assertTrue(body.contains("- 1 [A](#a)" + RN +
                "\t\t- 1.1.1 [deep](#deep)" + RN +
                "\t- 1.2 [B](#b)" + RN +
                "- 2 [C](#c)" + RN), body);
    }

    @Test
    public void testTOCFenceLineWithInfoStringDoesNotCloseAnOpenFence() {
        String body = renderEn("${widget:tableOfContents}\n" +
                "## A\n" +
                "```\n" +
                "```not-a-close\n" +
                "# inside\n" +
                "```\n" +
                "## B\n");

        assertTrue(body.contains("- [A](#a)" + RN + "- [B](#b)" + RN), body);
        assertFalse(body.contains("[inside]"), body);
    }

    @Test
    public void testTOCOnTheFirstLineOfTheDocument() {
        // Without the head comment the tag is on line 0 of the scanned text.
        for (String tag : new String[]{"tableOfContents", "toc"}) {
            String body = new Generator(new File("README.src.md"),
                    "<!--@nrg.noHeader=true-->\n${widget:" + tag + "}\n## A\n## B\n")
                    .getResult("en").getContent().toString();

            assertEquals("- [A](#a)" + RN + "- [B](#b)" + RN + RN + "## A" + RN + "## B" + RN, body, tag);
        }
    }

    @Test
    public void testTOCScanStartsAtTheTagsOwnLine() {
        // The line that carries the tag is the first line of the scanned range, so a heading
        // written on that line is listed too — tag and all, since the scan does not render it.
        String body = renderEn("## Guide ${widget:toc}\n## A\n");

        assertTrue(body.startsWith("## Guide - [Guide ${widget:toc}](#guide-widgettoc)" + RN + "- [A](#a)" + RN), body);
    }

    @Test
    public void testTOCKeepsAnEscapedPlaceholderInAHeadingLiteral() {
        // The scan must not be a root generator: it would unescape \${price}, and the entry
        // would then be rendered with the property value.
        String body = renderEn("<!--@price=10-->\n${widget:toc}\n## Cost \\${price}\n");

        assertTrue(body.contains("- [Cost ${price}](#cost-price)"), body);
        assertTrue(body.contains("## Cost ${price}"), body);
    }

    @Test
    public void testTOCInsideAnImportedTemplateSeesTheImportersProperties(@TempDir Path dir) throws IOException {
        write(dir, "child.src.md", "${widget:toc}\n" +
                "## Always\n" +
                "${widget:if(cond='${edition} == pro')}\n" +
                "## Pro only\n" +
                "${widget:endIf}\n");
        Path root = write(dir, "README.src.md", "<!--@edition=pro-->\n${widget:import(path='child.src.md')}\n");

        String body = new NoHeadCommentGenerator(root.toFile(), StandardCharsets.UTF_8).getResult("en").getContent().toString();

        assertTrue(body.contains("- [Always](#always)" + RN + "- [Pro only](#pro-only)" + RN), body);
    }

    @Test
    public void testTOCInsideAnImportedTemplateKnowsTheDefaultLanguage(@TempDir Path dir) throws IOException {
        // ru is the default language, so the English output is README.en.md; the scan has to
        // render the languages widget in the heading with the same links as the real pass.
        write(dir, "child.src.md", "${widget:toc}\n## Read in ${widget:languages}\n");
        Path root = write(dir, "README.src.md", "<!--@nrg.languages=ru,en-->\n${widget:import(path='child.src.md')}\n");

        String ru = new NoHeadCommentGenerator(root.toFile(), StandardCharsets.UTF_8).getResult("ru").getContent().toString();

        assertTrue(ru.contains("- [Read in [ **ru** | [en](README.en.md) ]](#"), ru);
        assertTrue(ru.contains("## Read in [ **ru** | [en](README.en.md) ]"), ru);
        assertFalse(ru.contains("(README.md)"), ru);
    }

    @Test
    public void testTOCInsideAnImportedTemplateKeepsTheImportChain(@TempDir Path dir) throws IOException {
        // The scan renders the child again; a cycle back to the root must be reported from the
        // root's point of view, as the real pass would, not as if the child were the root.
        Path child = write(dir, "child.src.md", "${widget:toc}\n## Child\n${widget:import(path='README.src.md')}\n");
        Path root = write(dir, "README.src.md", "${widget:import(path='child.src.md')}\n");
        Generator generator = new NoHeadCommentGenerator(root.toFile(), StandardCharsets.UTF_8);

        RuntimeException e = assertThrows(RuntimeException.class, () -> generator.getResult("en"));

        String rootKey = GeneratorConfig.importKey(root.toFile());
        String childKey = GeneratorConfig.importKey(child.toFile());
        assertEquals("import: circular import: " + rootKey + " -> " + childKey + " -> " + rootKey, e.getMessage());
    }
}