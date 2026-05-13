package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailsBlockProcessorTest extends DefaultNRGTest {

    private static String render(String body) {
        Generator g = new Generator(new File("README.src.md"), body);
        return g.getResult("en").getContent().toString();
    }

    @Test
    public void testSimpleBlock() {
        String body = "before\n" +
                "${widget:details(summary='Advanced')}\n" +
                "inner text\n" +
                "${widget:endDetails}\n" +
                "after\n";

        String out = render(body);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("<details>"), out);
        assertTrue(out.contains("<summary>Advanced</summary>"), out);
        assertTrue(out.contains("inner text"));
        assertTrue(out.contains("</details>"), out);
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:details"), out);
        assertFalse(out.contains("widget:endDetails"), out);
    }

    @Test
    public void testBlockOpenTrueEmitsOpenAttribute() {
        String body = "${widget:details(summary='S', open='true')}\n" +
                "x\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<details open>"), out);
    }

    @Test
    public void testBlockOpenFalseEmitsPlainDetails() {
        String body = "${widget:details(summary='S', open='false')}\n" +
                "x\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<details>"), out);
        assertFalse(out.contains("<details open>"), out);
    }

    @Test
    public void testBlankLinesAroundInnerContent() {
        String body = "${widget:details(summary='S')}\n" +
                "INNER\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<summary>S</summary>"), out);
        String summaryToInner = out.substring(out.indexOf("<summary>S</summary>"), out.indexOf("INNER"));
        assertTrue(summaryToInner.contains(System.lineSeparator() + System.lineSeparator()),
                "expected blank line after <summary>, got: " + summaryToInner.replace("\n", "\\n"));
        String innerToClose = out.substring(out.indexOf("INNER"), out.indexOf("</details>"));
        assertTrue(innerToClose.contains(System.lineSeparator() + System.lineSeparator()),
                "expected blank line before </details>, got: " + innerToClose.replace("\n", "\\n"));
    }

    @Test
    public void testNestedBlocks() {
        String body = "${widget:details(summary='Outer')}\n" +
                "OUTERTEXT\n" +
                "${widget:details(summary='Inner')}\n" +
                "INNERTEXT\n" +
                "${widget:endDetails}\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<summary>Outer</summary>"), out);
        assertTrue(out.contains("<summary>Inner</summary>"), out);
        assertTrue(out.contains("OUTERTEXT"));
        assertTrue(out.contains("INNERTEXT"));
        int openCount = (out.length() - out.replace("<details>", "").length()) / "<details>".length();
        int closeCount = (out.length() - out.replace("</details>", "").length()) / "</details>".length();
        assertEquals(2, openCount, "two opening tags expected; got: " + out);
        assertEquals(2, closeCount, "two closing tags expected; got: " + out);
    }

    @Test
    public void testMultipleSiblingBlocks() {
        String body = "${widget:details(summary='A')}\nax\n${widget:endDetails}\n" +
                "between\n" +
                "${widget:details(summary='B')}\nbx\n${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<summary>A</summary>"), out);
        assertTrue(out.contains("<summary>B</summary>"), out);
        assertTrue(out.contains("ax"));
        assertTrue(out.contains("bx"));
        assertTrue(out.contains("between"));
    }

    @Test
    public void testInnerVariableStillSubstitutes() {
        String body = "<!--@x=VALUE-->\n" +
                "${widget:details(summary='S')}\n" +
                "${x}\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("VALUE"), out);
        assertFalse(out.contains("${x}"), out);
    }

    @Test
    public void testInnerWidgetStillRenders() {
        String body = "<!--@nrg.languages=en-->\n" +
                "${widget:details(summary='S')}\n" +
                "Last build: ${widget:date(pattern='yyyy')}\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<summary>S</summary>"), out);
        assertTrue(out.matches("(?s).*Last build: \\d{4}.*"), out);
    }

    @Test
    public void testSummarySubstitutesProperty() {
        String body = "<!--@title=Hello-->\n" +
                "${widget:details(summary='${title}')}\n" +
                "x\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<summary>Hello</summary>"), out);
    }

    @Test
    public void testMissingSummaryLogsError() {
        String body = "${widget:details()}\nx\n${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<summary></summary>"), out);
        assertTrue(getErrAndClear().contains("details widget: missing required 'summary'"));
    }

    @Test
    public void testStrayEndDetailsLogsAndDrops() {
        String body = "before\n${widget:endDetails}\nafter\n";

        String out = render(body);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:endDetails"), out);
        assertTrue(getErrAndClear().contains("details widget: unmatched stray"));
    }

    @Test
    public void testUnclosedBlockRollsBack() {
        String body = "before\n" +
                "${widget:details(summary='S')}\n" +
                "INNER\n";

        String out = render(body);
        assertTrue(out.contains("before"));
        assertFalse(out.contains("<details"), out);
        assertFalse(out.contains("<summary>"), out);
        assertFalse(out.contains("INNER"), out);
        assertTrue(getErrAndClear().contains("details widget: unclosed"));
    }

    @Test
    public void testSingleTagOpenerWithContentParamIsLeftAlone() {
        String body = "before\n" +
                "${widget:details(summary='S', content='HIDDEN')}\n" +
                "after\n";

        String out = render(body);
        assertTrue(out.contains("<details><summary>S</summary>HIDDEN</details>"), out);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
    }

    @Test
    public void testMixedContentAndEndDetailsLogsStray() {
        String body = "${widget:details(summary='S', content='X')}\n" +
                "${widget:endDetails}\n";

        String out = render(body);
        assertTrue(out.contains("<details><summary>S</summary>X</details>"), out);
        assertFalse(out.contains("widget:endDetails"), out);
        assertTrue(getErrAndClear().contains("details widget: unmatched stray"));
    }
}
