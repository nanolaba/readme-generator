package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailsWidgetTest extends DefaultNRGTest {

    @Test
    public void testSingleTagBasic() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='Click', content='Hidden')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("<details><summary>Click</summary>Hidden</details>"), body);
    }

    @Test
    public void testSingleTagOpenTrue() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='Click', content='Hidden', open='true')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("<details open><summary>Click</summary>Hidden</details>"), body);
    }

    @Test
    public void testSingleTagOpenFalseRendersPlain() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='Click', content='Hidden', open='false')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("<details><summary>Click</summary>Hidden</details>"), body);
        assertFalse(body.contains("<details open>"), body);
    }

    @Test
    public void testSingleTagOpenGarbageRendersPlain() {
        // Boolean.parseBoolean: anything that's not case-insensitive "true" → false.
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='Click', content='Hidden', open='yes')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertFalse(body.contains("<details open>"), body);
    }

    @Test
    public void testNewlineEscapeInContent() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='S', content='line1\\nline2')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("<details><summary>S</summary>line1\nline2</details>"), body);
    }

    @Test
    public void testBackslashEscapeInContent() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='S', content='a\\\\b')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("<details><summary>S</summary>a\\b</details>"), body);
    }

    @Test
    public void testMissingSummaryLogsErrorAndRendersEmpty() {
        Generator generator = new Generator(new File("README.src.md"),
                "before\n${widget:details(content='X')}\nafter\n");
        String body = generator.getResult("en").getContent().toString();
        // The widget itself renders empty when summary is missing. The block-pre-pass (Task 3)
        // leaves this line alone because content= is present; the per-line widget then renders ''.
        assertFalse(body.contains("<details"), body);
        assertTrue(body.contains("before"), body);
        assertTrue(body.contains("after"), body);
        assertTrue(getErrAndClear().contains("details widget: missing required 'summary'"));
    }

    @Test
    public void testMissingContentInSingleTagLooksLikeOpenerLogsError() {
        // No content=, no following endDetails → block-form pre-pass classifies as unclosed
        // and rolls back. The per-line widget never gets called.
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:details(summary='S')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertFalse(body.contains("<details><summary>S</summary>"), body);
        assertTrue(getErrAndClear().contains("details widget: unclosed"),
                "expected unclosed-block error in stderr");
    }

    @Test
    public void testSummaryWithPropertyAndLanguageConstructs() {
        // Per-line pipeline already applies property/language substitution before the widget
        // runs, so ${var} and ${en:'…', ru:'…'} inside summary= "just work".
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@title=Hello-->\n" +
                        "${widget:details(summary='${title}', content='X')}\n");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("<details><summary>Hello</summary>X</details>"), body);
    }
}
