package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NRGUtilTest extends DefaultNRGTest {

    @Test
    public void testMergeProperties() {
        Properties properties = new Properties();
        NRGUtil.mergeProperty("a", "b", properties);
        assertEquals("b", properties.getProperty("a"));

        NRGUtil.mergeProperty("a", "c", properties);
        assertEquals("c", properties.getProperty("a"));
    }

    @Test
    public void testUnwrapParameters() {
        assertEquals("", NRGUtil.unwrapParameterValue(""));
        assertEquals("", NRGUtil.unwrapParameterValue("''"));
        assertEquals("", NRGUtil.unwrapParameterValue("\"\""));
        assertEquals("1", NRGUtil.unwrapParameterValue("\"1\""));
        assertEquals("1", NRGUtil.unwrapParameterValue("'1'"));
        assertEquals("'1'", NRGUtil.unwrapParameterValue("\"'1'\""));
        assertEquals("'", NRGUtil.unwrapParameterValue("\"'\""));
        assertEquals("\"", NRGUtil.unwrapParameterValue("\"\"\""));
        assertEquals("\"\"", NRGUtil.unwrapParameterValue("\"\"\"\""));
        assertEquals("\"123\"", NRGUtil.unwrapParameterValue("\"\"123\"\""));
    }

    @Test
    public void testParseParametersLine() {
        Map<String, String> map = NRGUtil.parseParametersLine("");
        assertTrue(map.isEmpty());

        map = NRGUtil.parseParametersLine("  ");
        assertTrue(map.isEmpty());

        map = NRGUtil.parseParametersLine("a=\"\"");
        assertEquals(1, map.size());
        assertEquals("", map.get("a"));

        map = NRGUtil.parseParametersLine("a=''");
        assertEquals(1, map.size());
        assertEquals("", map.get("a"));

        map = NRGUtil.parseParametersLine("a=\"b\"");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));

        map = NRGUtil.parseParametersLine("a='b'");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = \"b\" ");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = 'b' ");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = \"b b\" ");
        assertEquals(1, map.size());
        assertEquals("b b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = 'b b' ");
        assertEquals(1, map.size());
        assertEquals("b b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = \"b b\", c = \"d\" ");
        assertEquals(2, map.size());
        assertEquals("b b", map.get("a"));
        assertEquals("d", map.get("c"));

        map = NRGUtil.parseParametersLine(" a = 'b b', c = 'd' ");
        assertEquals(2, map.size());
        assertEquals("b b", map.get("a"));
        assertEquals("d", map.get("c"));

        map = NRGUtil.parseParametersLine("a=\"'b'\"");
        assertEquals(1, map.size());
        assertEquals("'b'", map.get("a"));

        map = NRGUtil.parseParametersLine("a='\"b\"'");
        assertEquals(1, map.size());
        assertEquals("\"b\"", map.get("a"));
    }

    @Test
    void testFindFirstUnescapedOccurrenceLine() {
        String text1 = "Line 1\nLine 2\n${widget:tableOfContents\nLine 4";
        assertEquals(2, NRGUtil.findFirstUnescapedOccurrenceLine(text1, "${widget:tableOfContents"));

        String text2 = "Line 1\n\\${widget:tableOfContents\n${widget:tableOfContents";
        assertEquals(2, NRGUtil.findFirstUnescapedOccurrenceLine(text2, "${widget:tableOfContents"));

        String text3 = "Line 1\nLine 2";
        assertEquals(-1, NRGUtil.findFirstUnescapedOccurrenceLine(text3, "${widget:tableOfContents"));

        String text4 = "\\${widget:tableOfContents\n\\${widget:tableOfContents";
        assertEquals(-1, NRGUtil.findFirstUnescapedOccurrenceLine(text4, "${widget:tableOfContents"));

        String text5 = "${widget:tableOfContents\nLine 2";
        assertEquals(0, NRGUtil.findFirstUnescapedOccurrenceLine(text5, "${widget:tableOfContents"));
    }

    @Test
    public void testGetLanguageScopedProperty_returnsLangScopedWhenDefined() {
        Properties p = new Properties();
        p.setProperty("greeting", "Hi");
        p.setProperty("greeting.ru", "Привет");
        assertEquals("Привет", NRGUtil.getLanguageScopedProperty(p, "greeting", "ru"));
    }

    @Test
    public void testGetLanguageScopedProperty_fallsBackToBareWhenLangScopedMissing() {
        Properties p = new Properties();
        p.setProperty("greeting", "Hi");
        p.setProperty("greeting.ru", "Привет");
        assertEquals("Hi", NRGUtil.getLanguageScopedProperty(p, "greeting", "en"));
    }

    @Test
    public void testGetLanguageScopedProperty_returnsNullWhenNeitherDefined() {
        Properties p = new Properties();
        assertNull(NRGUtil.getLanguageScopedProperty(p, "missing", "en"));
    }

    @Test
    public void testGetLanguageScopedProperty_returnsBareWhenLanguageNull() {
        Properties p = new Properties();
        p.setProperty("greeting", "Hi");
        assertEquals("Hi", NRGUtil.getLanguageScopedProperty(p, "greeting", null));
    }

    @Test
    public void testGetLanguageScopedProperty_emptyLangScopedValueIsHonored() {
        Properties p = new Properties();
        p.setProperty("foo", "bar");
        p.setProperty("foo.en", "");
        assertEquals("", NRGUtil.getLanguageScopedProperty(p, "foo", "en"));
    }

    @Test
    public void testGetLanguageScopedProperty_emptyBareValueIsHonored() {
        Properties p = new Properties();
        p.setProperty("foo", "");
        assertEquals("", NRGUtil.getLanguageScopedProperty(p, "foo", "en"));
    }

    @Test
    public void testProcessWidgetEscapes() {
        assertEquals("a\nb", NRGUtil.processWidgetEscapes("a\\nb"));
        assertEquals("a\\nb", NRGUtil.processWidgetEscapes("a\\\\nb"));
        assertEquals("\\", NRGUtil.processWidgetEscapes("\\\\"));
        assertEquals("no escapes", NRGUtil.processWidgetEscapes("no escapes"));
        assertEquals("trail\\", NRGUtil.processWidgetEscapes("trail\\"));
        assertEquals("a\\rb", NRGUtil.processWidgetEscapes("a\\rb"));
        assertNull(NRGUtil.processWidgetEscapes(null));
    }

    /** The implementation {@link NRGUtil#parseParametersLine} replaced; kept as the oracle. */
    private static Map<String, String> parseParametersWithTheOldRegex(String parameters) {
        Map<String, String> map = new java.util.HashMap<>();
        if (parameters != null && !parameters.isEmpty()) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(\\S+) *= *([\"'])(((?!\\2).)*)(\\2)").matcher(parameters);
            while (matcher.find()) {
                map.put(matcher.group(1), matcher.group(3));
            }
        }
        return map;
    }

    @Test
    public void testParseParametersLineMatchesTheRegexItReplaced() {
        String[] handPicked = {
                "", " ", "a='1'", "a = '1'", "a  =  \"1\"", "a='1', b=\"2\"", "a='1',b='2'",
                "a=b='x'", "a='b'='c'", "a='it's'", "a=\"it's\"", "a='x\"y'", "a = ", "='x'", "a=''",
                "a='1' b", "a ='1'b= '2'", "a\t='1'", "a=\t'1'", "a='1\n2'", "a='1' \n b='2'",
                "title = \"Table of contents\", ordered = \"true\"", "====='x'", "a==='x'", "a='x'==='y'",
                "a b c = 'x'", "a='x", "a=\"x'", "'='='", "\"=\"=\"", "a='\u2028'", "a\u00a0='1'",
        };
        for (String input : handPicked) {
            assertEquals(parseParametersWithTheOldRegex(input), NRGUtil.parseParametersLine(input),
                    "input: [" + input + "]");
        }

        // Every string up to a few characters over a small alphabet, then longer random ones.
        char[] alphabet = {'a', 'b', '=', ' ', '\'', '"', ',', '\t', '\n'};
        java.util.Random random = new java.util.Random(20261111L);
        for (int round = 0; round < 300_000; round++) {
            int length = random.nextInt(round < 150_000 ? 9 : 40);
            StringBuilder sb = new StringBuilder(length);
            for (int j = 0; j < length; j++) {
                sb.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String input = sb.toString();
            Map<String, String> expected = parseParametersWithTheOldRegex(input);
            Map<String, String> actual = NRGUtil.parseParametersLine(input);
            if (!expected.equals(actual)) {
                assertEquals(expected, actual, "input: [" + input.replace("\n", "\\n").replace("\t", "\\t") + "]");
            }
        }
    }

    @Test
    public void testParseParametersLineIsLinearOnHostileInput() {
        // Each of these took the old regex from many seconds to minutes at this size.
        StringBuilder noEquals = new StringBuilder();
        StringBuilder manyEquals = new StringBuilder("a");
        StringBuilder unclosed = new StringBuilder();
        for (int i = 0; i < 300_000; i++) {
            noEquals.append('x');
            manyEquals.append('=');
            unclosed.append("a='");
        }
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            assertTrue(NRGUtil.parseParametersLine(noEquals.toString()).isEmpty());
            assertTrue(NRGUtil.parseParametersLine(manyEquals.toString()).isEmpty());
            assertEquals(1, NRGUtil.parseParametersLine(manyEquals + "'v'").size());
            NRGUtil.parseParametersLine(unclosed.toString());
        });
    }

    @Test
    public void testStripPropertyComments() {
        assertEquals("", NRGUtil.stripPropertyComments("<!--@a=1-->"));
        assertEquals("", NRGUtil.stripPropertyComments("<!-- @a=1 -->"));
        assertEquals("", NRGUtil.stripPropertyComments("<!--@-->"));
        assertEquals("x  y", NRGUtil.stripPropertyComments("x <!--@a=1--> y"));
        assertEquals("", NRGUtil.stripPropertyComments("<!--@a=1--><!--@b=2-->"));

        // Each comment ends at its own terminator: the text between two comments survives.
        assertEquals("A  KEEP <!-- plain --> tail",
                NRGUtil.stripPropertyComments("A <!--@p=1--> KEEP <!-- plain --> tail"));
        assertEquals(" KEEP ", NRGUtil.stripPropertyComments("<!--@p=1--> KEEP <!--@q=2-->"));

        // Not property comments.
        assertEquals("<!-- plain -->", NRGUtil.stripPropertyComments("<!-- plain -->"));
        assertEquals("<\\!--@a=1-->", NRGUtil.stripPropertyComments("<\\!--@a=1-->"));
        assertEquals("<!--\\@a=1-->", NRGUtil.stripPropertyComments("<!--\\@a=1-->"));
        assertEquals("<!--\t@a=1-->", NRGUtil.stripPropertyComments("<!--\t@a=1-->"));

        // A comment is confined to its line; an unterminated one is left alone.
        assertEquals("<!--@a\nb-->", NRGUtil.stripPropertyComments("<!--@a\nb-->"));
        assertEquals("<!--@a\r\nx\r\ny", NRGUtil.stripPropertyComments("<!--@a\r\nx<!--@b=1-->\r\ny"));
        assertEquals("<!--@never closed", NRGUtil.stripPropertyComments("<!--@never closed"));

        String untouched = "no comments here";
        assertTrue(untouched == NRGUtil.stripPropertyComments(untouched), "same instance when nothing is removed");
    }

    @Test
    public void testStripPropertyCommentsIsLinearOnHostileLines() {
        // 200 KB of unterminated openers: the regex this replaced rescanned the rest of the
        // line for each one. Also with a terminator far away, on a later line.
        StringBuilder openers = new StringBuilder();
        for (int i = 0; i < 40_000; i++) {
            openers.append("<!--@");
        }
        String hostile = openers.toString();

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            assertEquals(hostile, NRGUtil.stripPropertyComments(hostile));
            assertEquals(hostile + "\n-->", NRGUtil.stripPropertyComments(hostile + "\n-->"));
        });
    }
}