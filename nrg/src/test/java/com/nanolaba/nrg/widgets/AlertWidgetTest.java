package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class AlertWidgetTest extends DefaultNRGTest {

    @Test
    public void testSimpleNote() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:alert(type='note', text='Hello')}\n");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("> [!NOTE]" + RN + "> Hello"), body);
    }

    @Test
    public void testAllValidTypes() {
        for (String type : new String[]{"note", "tip", "important", "warning", "caution"}) {
            Generator generator = new Generator(new File("README.src.md"),
                    "${widget:alert(type='" + type + "', text='Body')}");
            String body = generator.getResult("en").getContent().toString();
            assertTrue(body.contains("> [!" + type.toUpperCase() + "]" + RN + "> Body"),
                    "type=" + type + " produced: " + body);
        }
    }

    @Test
    public void testMixedCaseTypeIsAccepted() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:alert(type='Note', text='X')}");
        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("> [!NOTE]" + RN + "> X"), body);
    }

    @Test
    public void testInvalidTypeProducesEmptyOutputAndLogsError() {
        Generator generator = new Generator(new File("README.src.md"),
                "before\n${widget:alert(type='info', text='x')}\nafter");

        String body = generator.getResult("en").getContent().toString();
        assertFalse(body.contains("> [!"), body);
        assertTrue(body.contains("before"));
        assertTrue(body.contains("after"));
        assertTrue(getErrAndClear().contains("alert widget: invalid type 'info'"));
    }

    @Test
    public void testMissingTypeLogsError() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:alert(text='no type')}");

        generator.getResult("en").getContent().toString();
        assertTrue(getErrAndClear().contains("alert widget: invalid type"));
    }

    @Test
    public void testMultilineTextViaEscapedNewline() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:alert(type='warning', text='Line 1\\nLine 2\\nLine 3')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains(
                "> [!WARNING]" + RN +
                        "> Line 1" + RN +
                        "> Line 2" + RN +
                        "> Line 3"), body);
    }

    @Test
    public void testLiteralBackslashNViaDoubleEscape() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:alert(type='tip', text='a\\\\nb')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("> [!TIP]" + RN + "> a\\nb"), body);
    }

    @Test
    public void testLanguageSubstitutionInText() {
        Generator generator = new Generator(new File("README.src.md"),
                "<!--@nrg.languages=en,ru-->\n" +
                        "${widget:alert(type='important', text=\"${en:'Read me', ru:'Прочти'}\")}");

        String en = generator.getResult("en").getContent().toString();
        String ru = generator.getResult("ru").getContent().toString();

        assertTrue(en.contains("> [!IMPORTANT]" + RN + "> Read me"), en);
        assertTrue(ru.contains("> [!IMPORTANT]" + RN + "> Прочти"), ru);
    }

    @Test
    public void testEmptyTextEmitsOnlyTypeLine() {
        Generator generator = new Generator(new File("README.src.md"),
                "${widget:alert(type='note', text='')}");

        String body = generator.getResult("en").getContent().toString();
        assertTrue(body.contains("> [!NOTE]"));
        assertFalse(body.contains("> [!NOTE]" + RN + "> "), "no body line expected");
    }

    @Test
    public void testProcessEscapesUnit() {
        assertEquals("a\nb", AlertWidget.processEscapes("a\\nb"));
        assertEquals("a\\nb", AlertWidget.processEscapes("a\\\\nb"));
        assertEquals("\\", AlertWidget.processEscapes("\\\\"));
        assertEquals("no escapes", AlertWidget.processEscapes("no escapes"));
        assertEquals("trail\\", AlertWidget.processEscapes("trail\\"));
    }
}
