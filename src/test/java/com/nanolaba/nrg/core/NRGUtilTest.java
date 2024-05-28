package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class NRGUtilTest extends DefaultNRGTest {

    @Test
    public void testMergeProperties() {
        Properties properties = new Properties();
        NRGUtil.mergeProperty("a", "b", properties);
        assertEquals("b", properties.getProperty("a"));
        assertFalse(getOutAndClear().endsWith("A duplicate property 'a' declaration was detected" + RN));

        NRGUtil.mergeProperty("a", "c", properties);
        assertEquals("c", properties.getProperty("a"));
        assertTrue(getOutAndClear().endsWith("A duplicate property 'a' declaration was detected" + RN));
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

        map = NRGUtil.parseParametersLine("a=\"b\"");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = \"b\" ");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = \"b b\" ");
        assertEquals(1, map.size());
        assertEquals("b b", map.get("a"));

        map = NRGUtil.parseParametersLine(" a = \"b b\", c = \"d\" ");
        assertEquals(2, map.size());
        assertEquals("b b", map.get("a"));
        assertEquals("d", map.get("c"));
    }

}