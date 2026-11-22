package com.nanolaba.nrg.core.json;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MinimalJsonParserTest {

    @Test
    void parsesEmptyObject() {
        Object v = MinimalJsonParser.parse("{}");
        assertTrue(v instanceof Map);
        assertTrue(((Map<?, ?>) v).isEmpty());
    }

    @Test
    void parsesStringField() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"name\":\"nrg\"}");
        assertEquals("nrg", m.get("name"));
    }

    @Test
    void parsesNumberField() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"n\":42}");
        assertEquals(42L, m.get("n"));
    }

    @Test
    void parsesDecimalNumber() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"x\":1.5}");
        assertEquals(1.5d, m.get("x"));
    }

    @Test
    void parsesNegativeNumber() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"n\":-7}");
        assertEquals(-7L, m.get("n"));
    }

    @Test
    void parsesScientificNumber() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"n\":1e3}");
        assertEquals(1000.0d, m.get("n"));
    }

    @Test
    void parsesBooleansAndNull() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"a\":true,\"b\":false,\"c\":null}");
        assertEquals(Boolean.TRUE, m.get("a"));
        assertEquals(Boolean.FALSE, m.get("b"));
        assertNull(m.get("c"));
        assertTrue(m.containsKey("c"));
    }

    @Test
    void parsesNestedObject() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"a\":{\"b\":\"c\"}}");
        Map<?, ?> inner = (Map<?, ?>) m.get("a");
        assertEquals("c", inner.get("b"));
    }

    @Test
    void parsesArray() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"xs\":[1,2,3]}");
        List<?> xs = (List<?>) m.get("xs");
        assertEquals(3, xs.size());
        assertEquals(1L, xs.get(0));
    }

    @Test
    void parsesStringEscapes() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse(
                "{\"s\":\"a\\\"b\\\\c\\n\\td\"}");
        assertEquals("a\"b\\c\n\td", m.get("s"));
    }

    @Test
    void parsesUnicodeEscape() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse("{\"s\":\"\\u00e9\"}");
        assertEquals("é", m.get("s"));
    }

    @Test
    void ignoresWhitespace() {
        Map<?, ?> m = (Map<?, ?>) MinimalJsonParser.parse(
                "  {  \"a\"  :  1  ,  \"b\"  :  \"x\"  }  ");
        assertEquals(1L, m.get("a"));
        assertEquals("x", m.get("b"));
    }

    @Test
    void rejectsTrailingComma() {
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse("{\"a\":1,}"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse("{\"a\":\"oops"));
    }

    @Test
    void rejectsUnknownLiteral() {
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse("{\"a\":nope}"));
    }

    @Test
    void rejectsExtraTokensAfterValue() {
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse("{\"a\":1} junk"));
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            b.append(s);
        }
        return b.toString();
    }

    @Test
    void deepNestingIsAParseErrorNotAStackOverflow() {
        // StackOverflowError is an Error: the readers catch Exception, so it ended the run.
        IllegalArgumentException arrays = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse(repeat("[", 50_000) + repeat("]", 50_000)));
        assertTrue(arrays.getMessage().contains("nesting deeper than"), arrays.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse(repeat("{\"a\":", 50_000) + "1" + repeat("}", 50_000)));
    }

    @Test
    void nestingWithinTheLimitStillParses() {
        Object parsed = MinimalJsonParser.parse(repeat("[", 500) + repeat("]", 500));
        assertTrue(parsed instanceof java.util.List);

        // Siblings do not add depth: a long flat array is fine.
        assertEquals(20_000, ((java.util.List<?>) MinimalJsonParser.parse(
                "[" + repeat("[],", 19_999) + "[]]")).size());
    }

    @Test
    void nestingLimitIsExactly512Levels() {
        assertTrue(MinimalJsonParser.parse(repeat("[", 512) + repeat("]", 512)) instanceof List);
        assertTrue(MinimalJsonParser.parse(repeat("{\"a\":", 511) + "[]" + repeat("}", 511)) instanceof Map);
        assertParseError(repeat("[", 513) + repeat("]", 513), "nesting deeper than 512 levels");
    }

    private static void assertParseError(String json, String expectedMessagePart) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parse(json), json);
        assertTrue(e.getMessage().contains(expectedMessagePart), e.getMessage());
    }

    @Test
    void parsesEmptyContainersWithInnerWhitespace() {
        assertEquals(Collections.emptyMap(), MinimalJsonParser.parse("{ \t\r\n}"));
        assertEquals(Collections.emptyList(), MinimalJsonParser.parse("[ \t\r\n]"));
        assertEquals(Collections.singletonMap("a", Collections.emptyList()),
                MinimalJsonParser.parse("{\"a\":[ ]}"));
    }

    @Test
    void parsesWhitespaceAroundEveryToken() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("a", Arrays.asList(1L, "x", null));
        expected.put("b", Boolean.TRUE);
        assertEquals(expected, MinimalJsonParser.parse(
                "\n{\n \"a\" : [ 1 , \"x\" ,\tnull ] ,\r\n \"b\" : true \n}\n"));
    }

    @Test
    void parsesTopLevelScalars() {
        assertEquals("s", MinimalJsonParser.parse(" \"s\" "));
        assertNull(MinimalJsonParser.parse("null"));
        assertEquals(Boolean.FALSE, MinimalJsonParser.parse("false"));
        assertEquals(5L, MinimalJsonParser.parse("5"));
    }

    @Test
    void parsesNumbersWithEveryDigitAndExponentForm() {
        assertEquals(Arrays.asList(0L, 9L, 10L, 90L, 1234567890L, -0L, -9L),
                MinimalJsonParser.parse("[0,9,10,90,1234567890,-0,-9]"));
        assertEquals(Arrays.asList(1000.0d, 0.001d, 100000.0d, 2.5e10d, 0.5d),
                MinimalJsonParser.parse("[1e+3,1e-3,1E5,2.5e10,0.5]"));
    }

    @Test
    void rejectsMalformedNumbers() {
        assertParseError("1e", "bad number: 1e");
        assertParseError("[1e]", "bad number: 1e");
        assertParseError("-", "bad number: -");
        assertParseError("1.2.3", "bad number: 1.2.3");
    }

    @Test
    void rejectsTrailingCommasWithTheirOwnMessage() {
        assertParseError("{\"a\":1,}", "trailing comma in object");
        assertParseError("{\"a\":1, \n}", "trailing comma in object");
        assertParseError("[1,]", "trailing comma in array");
        assertParseError("[1, \t]", "trailing comma in array");
    }

    @Test
    void rejectsInputThatEndsAfterAComma() {
        assertParseError("{\"a\":1,", "expected '\"'");
        assertParseError("[1,", "unexpected end of input");
        assertParseError("[1 ", "expected ']'");
        assertParseError("{\"a\":1 ", "expected '}'");
    }

    @Test
    void rejectsMalformedObjects() {
        assertParseError("{\"a\" 1}", "expected ':'");
        assertParseError("{a:1}", "expected '\"'");
        assertParseError("{\"a\":1 \"b\":2}", "expected '}'");
        assertParseError("", "unexpected end of input");
        assertParseError("   ", "unexpected end of input");
        assertParseError("+1", "unexpected character '+'");
        assertParseError("tru", "expected true/false");
        assertParseError("nul", "expected null");
    }

    @Test
    void parsesEveryEscape() {
        assertEquals("A\u00e9\"\\/\b\f\n\r\t", MinimalJsonParser.parse(
                "\"\\u0041\\u00E9\\\"\\\\\\/\\b\\f\\n\\r\\t\""));
    }

    @Test
    void rejectsMalformedEscapes() {
        assertParseError("\"\\u12", "short unicode escape");
        assertParseError("\"\\u123", "short unicode escape");
        // Exactly four characters left: the escape itself is complete, the string is not.
        assertParseError("\"\\u0041", "unterminated string");
        assertParseError("\"\\u12G4\"", "bad unicode escape: 12G4");
        assertParseError("\"\\x\"", "unknown escape: \\x");
        assertParseError("\"\\", "dangling escape");
    }
}
