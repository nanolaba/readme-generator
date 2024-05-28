package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NRGUtilTest extends DefaultNRGTest {

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