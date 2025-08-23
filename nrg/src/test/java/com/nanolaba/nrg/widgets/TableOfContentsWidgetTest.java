package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.NoHeadCommentGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TableOfContentsWidgetTest extends DefaultNRGTest {


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
}