package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableOfContentsWidgetTest {

    @Test
    public void testTOCWidget() {
        Generator generator = new Generator(new File("README.src.md"),
                """
                        <!--@nrg.languages=en,ru,fr-->
                        ${nrg.widget:tableOfContents}
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

                "- [AAA](#aaa)" + System.lineSeparator() +
                "\t- [aaa](#aaa)" + System.lineSeparator() +
                "\t- [bbb](#bbb)" + System.lineSeparator() +
                "- [BBB](#bbb)" + System.lineSeparator() +
                "\t- [ccc](#ccc)" + System.lineSeparator() +
                "\t- [ddd](#ddd)" + System.lineSeparator() +
                "- [CCC](#ccc)"));
    }


    @Test
    public void testTOCWidget1() {
        Generator generator = new Generator(new File("README.src.md"),
                """
                        <!--@nrg.languages=en,ru,fr-->
                        ## Header before TOC
                        ${nrg.widget:tableOfContents(title = "TOC", ordered = "true")}
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
                "## TOC" + System.lineSeparator() +
                "1. [AAA](#aaa)" + System.lineSeparator() +
                "\t1. [aaa](#aaa)" + System.lineSeparator() +
                "\t2. [bbb](#bbb)" + System.lineSeparator() +
                "2. [BBB](#bbb)" + System.lineSeparator() +
                "\t1. [ccc](#ccc)" + System.lineSeparator() +
                "\t2. [ddd](#ddd)" + System.lineSeparator() +
                "3. [CCC](#ccc)"));
    }

    @Test
    public void testTOCWidget2() {
        Generator generator = new Generator(new File("README.src.md"),
                """
                        <!--@nrg.languages=en,ru,fr-->
                        ${nrg.widget:tableOfContents(title = "${en:'Table of contents', ru:'Содержание'}", ordered = "true")}
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
        assertFalse(bodyEn.contains("Содержание"));
        assertTrue(bodyEn.contains(
                "## Table of contents" + System.lineSeparator() +
                "1. [AAA](#aaa)" + System.lineSeparator() +
                "\t1. [aaa](#aaa)" + System.lineSeparator() +
                "\t2. [bbb](#bbb)" + System.lineSeparator() +
                "2. [BBB](#bbb)" + System.lineSeparator() +
                "\t1. [ccc](#ccc)" + System.lineSeparator() +
                "\t2. [ddd](#ddd)" + System.lineSeparator() +
                "3. [CCC](#ccc)"));
    }
}