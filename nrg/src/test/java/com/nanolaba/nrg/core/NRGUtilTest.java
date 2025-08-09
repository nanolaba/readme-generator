package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}