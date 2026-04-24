package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.NoHeadCommentGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

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
}