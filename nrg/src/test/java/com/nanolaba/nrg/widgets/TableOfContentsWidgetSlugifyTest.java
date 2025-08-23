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
        assertEquals("simple-title", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с жирным текстом")
    void testBoldText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## My **Bold** Title", config, headers);
        assertEquals("my-bold-title", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с курсивом")
    void testItalicText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("### Title with *italic* text", config, headers);
        assertEquals("title-with-italic-text", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с кодом")
    void testCodeText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("#### Using `code` in title", config, headers);
        assertEquals("using-code-in-title", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок со ссылкой")
    void testLinkInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Check [Documentation](https://example.com)", config, headers);
        assertEquals("check-documentation", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с HTML тегами")
    void testHtmlTags() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Title with <strong>HTML</strong>", config, headers);
        assertEquals("title-with-html", getAnchorFromHeader(header));
    }

    @ParameterizedTest
    @DisplayName("Заголовки с различными знаками пунктуации")
    @CsvSource({
            "'# FAQ: How to use this?', 'faq-how-to-use-this'",
            "'## Step 1: Install & Configure', 'step-1-install-configure'",
            "'### Version 2.0 - What\\'s New!', 'version-20-whats-new'",
            "'#### Section A.1.2 (Important)', 'section-a12-important'",
            "'# Title with \"quotes\" and \\'apostrophes\\'', 'title-with-quotes-and-apostrophes'"
    })
    void testPunctuationMarks(String input, String expected) {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header(input, config, headers);
        assertEquals(expected, getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с множественными пробелами")
    void testMultipleSpaces() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Multiple    Spaces   Here", config, headers);
        assertEquals("multiple-spaces-here", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с URL")
    void testUrlInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Visit https://example.com for details", config, headers);
        assertEquals("visit-for-details", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с зачеркнутым текстом")
    void testStrikethroughText() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Old ~~deprecated~~ method", config, headers);
        assertEquals("old-deprecated-method", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с подчеркиваниями")
    void testUnderscores() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Method_name_with_underscores", config, headers);
        assertEquals("method-name-with-underscores", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок только из специальных символов")
    void testSpecialCharactersOnly() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# !!!@@@###", config, headers);
        assertEquals("", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Пустой заголовок")
    void testEmptyTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("#   ", config, headers);
        assertEquals("", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с дефисами")
    void testDashesInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Pre-formatted--title---here", config, headers);
        assertEquals("pre-formatted-title-here", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с цифрами")
    void testNumbersInTitle() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("## Chapter 123 Section 4.5.6", config, headers);
        assertEquals("chapter-123-section-456", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Сложный заголовок с различными элементами")
    void testComplexTitle() {
        String complexTitle = "### **Step 2**: Configure [API Keys](https://docs.example.com) & `Environment` Variables!";
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header(complexTitle, config, headers);
        assertEquals("step-2-configure-api-keys-environment-variables", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок с Unicode символами")
    void testUnicodeCharacters() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# Título con acentós and émojis 🎉", config, headers);
        assertEquals("titulo-con-acentos-and-emojis", getAnchorFromHeader(header));
    }

    @Test
    @DisplayName("Заголовок начинающийся и заканчивающийся дефисами")
    void testLeadingTrailingDashes() {
        TableOfContentsWidget.Header header = new TableOfContentsWidget.Header("# -start and end-", config, headers);
        assertEquals("start-and-end", getAnchorFromHeader(header));
    }

    /**
     * Вспомогательный метод для извлечения якоря из Header объекта
     * Использует рефлексию для доступа к приватному полю anchor
     */
    private String getAnchorFromHeader(TableOfContentsWidget.Header header) {
        try {
            java.lang.reflect.Field anchorField = TableOfContentsWidget.Header.class.getDeclaredField("anchor");
            anchorField.setAccessible(true);
            return (String) anchorField.get(header);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access anchor field", e);
        }
    }
}