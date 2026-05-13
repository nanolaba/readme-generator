package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IfWidgetTest extends DefaultNRGTest {

    private static String render(String body, Map<String, String> env) {
        Generator g = new Generator(new File("README.src.md"), body, null,
                env == null ? null : env::get);
        return g.getResult("en").getContent().toString();
    }

    @Test
    public void testTrueConditionKeepsContent() {
        String body = "before\n" +
                "${widget:if(cond='yes')}\n" +
                "INSIDE\n" +
                "${widget:endIf}\n" +
                "after\n";

        String out = render(body, null);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("INSIDE"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:if"), out);
        assertFalse(out.contains("widget:endIf"), out);
    }

    @Test
    public void testFalseConditionDropsContent() {
        String body = "before\n" +
                "${widget:if(cond='\"\"')}\n" +
                "INSIDE\n" +
                "${widget:endIf}\n" +
                "after\n";

        String out = render(body, null);
        assertTrue(out.contains("before"));
        assertFalse(out.contains("INSIDE"), out);
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:if"));
    }

    @Test
    public void testNestedConditionsBothTrue() {
        String body = "${widget:if(cond='yes')}\n" +
                "OUTER\n" +
                "${widget:if(cond='yes')}\n" +
                "INNER\n" +
                "${widget:endIf}\n" +
                "${widget:endIf}\n";

        String out = render(body, null);
        assertTrue(out.contains("OUTER"), out);
        assertTrue(out.contains("INNER"), out);
    }

    @Test
    public void testNestedOuterFalseDropsAll() {
        String body = "${widget:if(cond='\"\"')}\n" +
                "OUTER\n" +
                "${widget:if(cond='yes')}\n" +
                "INNER\n" +
                "${widget:endIf}\n" +
                "${widget:endIf}\n";

        String out = render(body, null);
        assertFalse(out.contains("OUTER"), out);
        assertFalse(out.contains("INNER"), out);
    }

    @Test
    public void testNestedInnerFalseKeepsOuter() {
        String body = "${widget:if(cond='yes')}\n" +
                "OUTER\n" +
                "${widget:if(cond='\"\"')}\n" +
                "INNER\n" +
                "${widget:endIf}\n" +
                "TAIL\n" +
                "${widget:endIf}\n";

        String out = render(body, null);
        assertTrue(out.contains("OUTER"), out);
        assertFalse(out.contains("INNER"), out);
        assertTrue(out.contains("TAIL"), out);
    }

    @Test
    public void testFalseBranchDoesNotInvokeInnerWidgets() {
        // Inner badge widget would otherwise log an error for missing 'value' parameter; if the
        // false branch correctly skips inner widgets, no error should fire.
        String body = "${widget:if(cond='\"\"')}\n" +
                "${widget:badge(type='license')}\n" +
                "${widget:endIf}\n";

        render(body, null);
        String err = getErrAndClear();
        assertFalse(err.contains("badge widget"), "inner widget must not run on false branch: " + err);
    }

    @Test
    public void testConditionUsesEnvProvider() {
        Map<String, String> env = new HashMap<>();
        env.put("CI", "true");
        String body = "${widget:if(cond='${env.CI}==true')}\n" +
                "CI MODE\n" +
                "${widget:endIf}\n";

        String out = render(body, env);
        assertTrue(out.contains("CI MODE"), out);
    }

    @Test
    public void testConditionUsesPropertyDeclaredEarlier() {
        String body = "<!--@flag=on-->\n" +
                "${widget:if(cond='${flag}==on')}\n" +
                "FLAGGED\n" +
                "${widget:endIf}\n";

        String out = render(body, null);
        assertTrue(out.contains("FLAGGED"), out);
    }

    @Test
    public void testUnclosedBlockLogsErrorAndDropsToEnd() {
        String body = "before\n" +
                "${widget:if(cond='yes')}\n" +
                "INSIDE\n" +
                "tail-without-endif\n";

        String out = render(body, null);
        assertTrue(out.contains("before"));
        assertFalse(out.contains("INSIDE"), out);
        assertFalse(out.contains("tail-without-endif"), out);
        String err = getErrAndClear();
        assertTrue(err.contains("unclosed"), "expected unclosed-block error: " + err);
    }

    @Test
    public void testStrayEndIfLogsError() {
        String body = "before\n" +
                "${widget:endIf}\n" +
                "after\n";

        String out = render(body, null);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        String err = getErrAndClear();
        assertTrue(err.contains("unmatched") || err.contains("stray"),
                "expected unmatched/stray endIf error: " + err);
    }

    @Test
    public void testStartsWithFunctionInRealCondition() {
        String body = "<!--@url=https://github.com/foo-->\n" +
                "${widget:if(cond='startsWith(${url}, https://)')}\n" +
                "GITHUB\n" +
                "${widget:endIf}\n";

        String out = render(body, null);
        assertTrue(out.contains("GITHUB"), out);
    }

    @Test
    public void testInvalidConditionLogsErrorAndDropsBlock() {
        String body = "before\n" +
                "${widget:if(cond='(unclosed')}\n" +
                "INSIDE\n" +
                "${widget:endIf}\n" +
                "after\n";

        String out = render(body, null);
        assertTrue(out.contains("before"));
        assertFalse(out.contains("INSIDE"), out);
        assertTrue(out.contains("after"));
        String err = getErrAndClear();
        assertTrue(err.contains("if widget") || err.contains("condition"),
                "expected condition-parse error: " + err);
    }

    @Test
    public void testInlineFormTrueConditionEmitsText() {
        String body = "before\n${widget:if(cond='yes', text='HELLO')}\nafter\n";

        String out = render(body, null);
        assertTrue(out.contains("HELLO"), out);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:if"), out);
    }

    @Test
    public void testInlineFormFalseConditionEmitsEmpty() {
        String body = "before\n${widget:if(cond='\"\"', text='HELLO')}\nafter\n";

        String out = render(body, null);
        assertFalse(out.contains("HELLO"), out);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:if"), out);
    }

    @Test
    public void testInlineFormNewlineEscapeInText() {
        String body = "${widget:if(cond='yes', text='line1\\nline2')}\n";

        String out = render(body, null);
        assertTrue(out.contains("line1\nline2"), out);
    }

    @Test
    public void testInlineFormMissingTextFallsThroughToBlockForm() {
        // No text=, no endIf — pre-pass classifies as unclosed block, logs error, rolls back.
        String body = "before\n${widget:if(cond='yes')}\nafter\n";

        String out = render(body, null);
        assertTrue(out.contains("before"));
        assertFalse(out.contains("after"), out); // rolled back from the unclosed opener
        assertTrue(getErrAndClear().contains("if widget: unclosed"));
    }

    @Test
    public void testInlineFormMissingCondTreatsAsFalsy() {
        // null cond → empty string → IfCondition.evaluate("") returns false → text suppressed.
        String body = "${widget:if(text='SHOULD-NOT-APPEAR')}\n";

        String out = render(body, null);
        assertFalse(out.contains("SHOULD-NOT-APPEAR"), out);
    }
}
