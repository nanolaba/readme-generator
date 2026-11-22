package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableOfContentsWidgetSlugifyTest extends DefaultNRGTest {

    private TableOfContentsWidget.Config config;
    private List<TableOfContentsWidget.Header> headers;

    @BeforeEach
    void setUp() {
        config = new TableOfContentsWidget.Config();
        headers = new ArrayList<>();
    }

    @Test
    @DisplayName("Простой заголовок без специальных символов")
    void testSimpleTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Simple Title", config, headers);
        assertEquals("simple-title", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с жирным текстом")
    void testBoldText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## My **Bold** Title", config, headers);
        assertEquals("my-bold-title", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с курсивом")
    void testItalicText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("### Title with *italic* text", config, headers);
        assertEquals("title-with-italic-text", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с кодом")
    void testCodeText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("#### Using `code` in title", config, headers);
        assertEquals("using-code-in-title", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок со ссылкой")
    void testLinkInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Check [Documentation](https://example.com)", config, headers);
        assertEquals("check-documentation", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с HTML тегами")
    void testHtmlTags() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Title with <strong>HTML</strong>", config, headers);
        assertEquals("title-with-html", header.getAnchor());
    }

    @ParameterizedTest
    @DisplayName("Заголовки с различными знаками пунктуации")
    @CsvSource({
            "'# FAQ: How to use this?', 'faq-how-to-use-this'",
            "'## Step 1: Install & Configure', 'step-1-install--configure'",
            "'### Version 2.0 - What\\'s New!', 'version-20---whats-new'",
            "'#### Section A.1.2 (Important)', 'section-a12-important'",
            "'# Title with \"quotes\" and \\'apostrophes\\'', 'title-with-quotes-and-apostrophes'"
    })
    void testPunctuationMarks(String input, String expected) {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header(input, config, headers);
        assertEquals(expected, header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с множественными пробелами")
    void testMultipleSpaces() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Multiple    Spaces   Here", config, headers);
        assertEquals("multiple----spaces---here", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с URL")
    void testUrlInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Visit https://example.com for details", config, headers);
        assertEquals("visit--for-details", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с зачеркнутым текстом")
    void testStrikethroughText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Old ~~deprecated~~ method", config, headers);
        assertEquals("old-deprecated-method", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с подчеркиваниями")
    void testUnderscores() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Method_name_with_underscores", config, headers);
        assertEquals("method_name_with_underscores", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок только из специальных символов")
    void testSpecialCharactersOnly() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# !!!@@@###", config, headers);
        assertEquals("", header.getAnchor());
    }

    @Test
    @DisplayName("Пустой заголовок")
    void testEmptyTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("#   ", config, headers);
        assertEquals("", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с дефисами")
    void testDashesInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Pre-formatted--title---here", config, headers);
        assertEquals("pre-formatted--title---here", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с цифрами")
    void testNumbersInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Chapter 123 Section 4.5.6", config, headers);
        assertEquals("chapter-123-section-456", header.getAnchor());
    }

    @Test
    @DisplayName("Сложный заголовок с различными элементами")
    void testComplexTitle() {
        String complexTitle = "### **Step 2**: Configure [API Keys](https://docs.example.com) & `Environment` Variables!";
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header(complexTitle, config, headers);
        assertEquals("step-2-configure-api-keys--environment-variables", header.getAnchor());
    }


    @Test
    @DisplayName("Заголовок начинающийся и заканчивающийся дефисами")
    void testLeadingTrailingDashes() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# -start and end-", config, headers);
        assertEquals("-start-and-end-", header.getAnchor());
    }

    @Test
    @DisplayName("GitHub keeps every hyphen: one per space, none collapsed or trimmed")
    void testGithubKeepsEveryHyphen() {
        assertEquals("feedback--support", anchor("## Feedback & Support"));
        assertEquals("a----b", anchor("## A -- B"));
        assertEquals("-quick-start", anchor("## 🚀 Quick start"));
        assertEquals("install-", anchor("## Install 🚀"));
        assertEquals("tabseparated", anchor("## Tab\tseparated"));
    }

    @ParameterizedTest
    @DisplayName("GitLab: the examples of its documentation")
    @CsvSource(delimiter = '|', value = {
            "# This heading has spaces in it | this-heading-has-spaces-in-it",
            "## This heading has a :thumbsup: in it | this-heading-has-a-thumbsup-in-it",
            "# This heading has Unicode in it: 한글 | this-heading-has-unicode-in-it-한글",
            "## This heading has 3.5 in it (and parentheses) | this-heading-has-35-in-it-and-parentheses",
            "## This heading has  multiple spaces and --- hyphens_and_underscores"
                    + " | this-heading-has--multiple-spaces-and-----hyphens_and_underscores"
    })
    void testGitlabDocumentationExamples(String input, String expected) {
        config.setAnchorStyle(TableOfContentsWidget.AnchorStyle.GITLAB);
        assertEquals(expected, anchor(input));
    }

    @Test
    @DisplayName("Bitbucket collapses runs of hyphens and spaces and drops underscores")
    void testBitbucketCollapsesHyphens() {
        config.setAnchorStyle(TableOfContentsWidget.AnchorStyle.BITBUCKET);
        assertEquals("markdown-header-a-bc-d", anchor("## -A -- b_c  d-"));
    }

    private String anchor(String line) {
        return new TableOfContentsWidget.Header(line, config, headers).getAnchor();
    }

    @Test
    @DisplayName("Русские заголовки")
    void testRussianTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Заголовок на русском", config, headers);
        assertEquals("заголовок-на-русском", header.getAnchor());
    }

    @Test
    @DisplayName("Китайские заголовки")
    void testChineseTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# 中文标题", config, headers);
        assertEquals("中文标题", header.getAnchor());
    }

    @Test
    @DisplayName("Заголовок с Unicode символами")
    void testUnicodeCharacters() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Título con acentós and émojis 🎉", config, headers);
        assertEquals("título-con-acentós-and-émojis-", header.getAnchor());
    }
}