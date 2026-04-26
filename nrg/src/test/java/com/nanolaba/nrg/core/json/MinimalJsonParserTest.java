package com.nanolaba.nrg.core.json;

import org.junit.jupiter.api.Test;

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
}
