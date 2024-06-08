package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.NoHeadCommentGenerator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class TableOfContentsWidgetTest extends DefaultNRGTest {


    @Test
    public void testTOCWidget() {
        Generator generator = new Generator(new File("README.src.md"),
                """
                        <!--@nrg.languages=en,ru,fr-->
                        ${widget:tableOfContents}
                        # MainHeader
                        ## AAA<!--en-->
                        ## ЯЯЯ<!--ru-->
                        ### aaa
                        ### ююю<!--ru-->
                        ### bbb
                        ## BBB
                        ### ccc
                        ### ddd
                        ## CCC
                        """
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
                """
                        <!--@nrg.languages=en,ru,fr-->
                        ## Header before TOC
                        ${widget:tableOfContents(title = "TOC", ordered = "true")}
                        # MainHeader
                        ## AAA<!--en-->
                        ## ЯЯЯ<!--ru-->
                        ### aaa
                        ### ююю<!--ru-->
                        ### bbb
                        ## BBB
                        ### ccc
                        ### ddd
                        ## CCC
                        """
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
                """
                        <!--@nrg.languages=en,ru,fr-->
                        someTextBeforeTOC
                        #headerBeforeTOC
                        ##headerBeforeTOC2
                        ###headerBeforeTOC3
                        ${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}
                        someTextBeforeHeader
                        # MainHeader
                        ## AAA<!--en-->
                        someText
                        someText1<!--en-->
                        someText2<!--ru-->
                        ## ЯЯЯ<!--ru-->
                        ### aaa
                        ### ююю<!--ru-->
                        ### bbb
                        ## BBB
                        ### ccc
                        ### ddd
                        ## CCC
                        ### ccc
                        #### cccc
                        """
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
                "" + RN +
                "someTextBeforeHeader" + RN +
                "# MainHeader" + RN +
                "## AAA" + RN +
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
                """
                        <!--@nrg.languages=en,ru,fr-->
                        ${widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}
                        ## A<!--en-->
                        ## B<!--en-->
                        ## Б<!--ru-->
                        ## ${en:'ENG', ru:'РУС'}
                        """
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
}