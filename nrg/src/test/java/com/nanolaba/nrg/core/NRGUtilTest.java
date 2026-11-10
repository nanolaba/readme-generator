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

    private static Map<String, String> mapOf(String... keysAndValues) {
        Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    @Test
    public void testParseParametersLineGrammar() {
        assertEquals(mapOf(), NRGUtil.parseParametersLine(null));
        assertEquals(mapOf(), NRGUtil.parseParametersLine(""));
        assertEquals(mapOf(), NRGUtil.parseParametersLine("  "));
        assertEquals(mapOf("a", "1"), NRGUtil.parseParametersLine("a='1'"));
        assertEquals(mapOf("a", "1"), NRGUtil.parseParametersLine("a  =  \"1\""));
        assertEquals(mapOf("a", ""), NRGUtil.parseParametersLine("a=''"));
        assertEquals(mapOf("title", "Table of contents", "ordered", "true"),
                NRGUtil.parseParametersLine("title = \"Table of contents\", ordered = \"true\""));
        assertEquals(mapOf("min-depth", "2", "numbering.style", "dotted", "a_b", "c"),
                NRGUtil.parseParametersLine("min-depth='2' numbering.style='dotted' a_b='c'"));

        // A value keeps everything up to its own closing quote.
        assertEquals(mapOf("a", "it's (done), x=y"), NRGUtil.parseParametersLine("a=\"it's (done), x=y\""));
        assertEquals(mapOf("a", "say \"hi\""), NRGUtil.parseParametersLine("a='say \"hi\"'"));
        assertEquals(mapOf("text", "say a='b', c='d'"), NRGUtil.parseParametersLine("text=\"say a='b', c='d'\""));
        assertEquals(mapOf("cond", "endsWith(${v}, -SNAPSHOT)", "text", "x"),
                NRGUtil.parseParametersLine("cond='endsWith(${v}, -SNAPSHOT)', text='x'"));

        // A later parameter with the same name wins.
        assertEquals(mapOf("a", "2"), NRGUtil.parseParametersLine("a='1', a='2'"));

        // Not parameters: no quote after '=', no closing quote, a value broken by a line end.
        assertEquals(mapOf(), NRGUtil.parseParametersLine("a = "));
        assertEquals(mapOf(), NRGUtil.parseParametersLine("a=b"));
        assertEquals(mapOf(), NRGUtil.parseParametersLine("a='x"));
        assertEquals(mapOf(), NRGUtil.parseParametersLine("a='1\n2'"));
        assertEquals(mapOf(), NRGUtil.parseParametersLine("='x'"));
        assertEquals(mapOf("b", "y"), NRGUtil.parseParametersLine("a='x, b=\"y\""));
        assertEquals(mapOf("c", "x"), NRGUtil.parseParametersLine("a b c = 'x'"));
    }

    @Test
    public void testParseParametersLineDoesNotNeedASpaceAfterTheComma() {
        // The name used to be "the longest run of non-whitespace", so in the first line below
        // the whole of title="T",ordered was read as one name and both parameters were lost.
        assertEquals(mapOf("title", "T", "ordered", "true"),
                NRGUtil.parseParametersLine("title=\"T\",ordered=\"true\""));
        assertEquals(mapOf("a", "1", "b", "2", "c", "3"), NRGUtil.parseParametersLine("a='1',b='2',c=\"3\""));
        assertEquals(mapOf("a", "1", "b", "2"), NRGUtil.parseParametersLine("a='1' ,b='2'"));
        assertEquals(mapOf("a", "1", "b", "2"), NRGUtil.parseParametersLine("a='1',,  ,b='2',"));
        assertEquals(mapOf("a", "1", "b", "2"), NRGUtil.parseParametersLine(",a='1',\tb='2'"));
        // Nothing at all between two parameters is tolerated as well.
        assertEquals(mapOf("a", "1", "b", "2"), NRGUtil.parseParametersLine("a='1'b='2'"));
    }

    @Test
    public void testParseParametersLineOnRandomWellFormedLists() {
        char[] nameChars = "abcxyz019-_.".toCharArray();
        // Deliberately includes what must not end or split a value.
        char[] valueChars = "ab 12,=()[]{}<>-_.:/\\$#@!\t".toCharArray();
        String[] separators = {", ", ",", " ", " , ", ",  ", "\t", ""};
        String[] aroundEquals = {"", " ", "  "};
        java.util.Random random = new java.util.Random(20261111L);

        for (int round = 0; round < 50_000; round++) {
            Map<String, String> expected = new java.util.HashMap<>();
            Map<String, String> expectedByOldRegex = new java.util.HashMap<>();
            StringBuilder list = new StringBuilder();
            StringBuilder conventional = new StringBuilder(); // ", " between, plain values
            int count = random.nextInt(6);
            for (int p = 0; p < count; p++) {
                StringBuilder name = new StringBuilder();
                for (int j = 1 + random.nextInt(6); j > 0; j--) {
                    name.append(nameChars[random.nextInt(nameChars.length)]);
                }
                char quote = random.nextBoolean() ? '\'' : '"';
                StringBuilder value = new StringBuilder();
                for (int j = random.nextInt(12); j > 0; j--) {
                    int pick = random.nextInt(valueChars.length + 1);
                    value.append(pick == valueChars.length ? (quote == '\'' ? '"' : '\'') : valueChars[pick]);
                }
                String eq = aroundEquals[random.nextInt(aroundEquals.length)] + "="
                        + aroundEquals[random.nextInt(aroundEquals.length)];
                if (p > 0) {
                    list.append(separators[random.nextInt(separators.length)]);
                    conventional.append(", ");
                }
                list.append(name).append(eq).append(quote).append(value).append(quote);
                expected.put(name.toString(), value.toString());

                String plainValue = value.toString().replaceAll("[='\"]", "x");
                conventional.append(name).append(eq).append(quote).append(plainValue).append(quote);
                expectedByOldRegex.put(name.toString(), plainValue);
            }
            assertEquals(expected, NRGUtil.parseParametersLine(list.toString()), "input: [" + list + "]");

            // Written the conventional way, a list means exactly what it meant to the regex
            // this parser replaced — existing templates keep their meaning.
            String conventionalList = conventional.toString();
            assertEquals(expectedByOldRegex, parseParametersWithTheOldRegex(conventionalList),
                    "oracle disagrees with the generator on: [" + conventionalList + "]");
            assertEquals(expectedByOldRegex, NRGUtil.parseParametersLine(conventionalList),
                    "input: [" + conventionalList + "]");
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
            // '=' is not part of a name, so a====…='v' has no '=' directly followed by a value
            // after the name and is not a parameter.
            assertTrue(NRGUtil.parseParametersLine(manyEquals + "'v'").isEmpty());
            assertEquals(mapOf("k", "v"), NRGUtil.parseParametersLine(noEquals + " k='v'"));
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