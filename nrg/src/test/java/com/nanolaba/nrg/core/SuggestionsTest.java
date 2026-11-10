package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionsTest {

    @Test
    public void testDistance() {
        assertEquals(0, Suggestions.distance("abc", "abc"));
        assertEquals(1, Suggestions.distance("toc", "tocc"));
        assertEquals(1, Suggestions.distance("maxdepth", "max-depth"));
        assertEquals(3, Suggestions.distance("", "abc"));
    }

    @Test
    public void testClosestWithinLimit() {
        assertEquals("toc", Suggestions.closest("tocc", Arrays.asList("toc", "todo", "date")));
        assertEquals("max-depth", Suggestions.closest("maxDepth", Arrays.asList("min-depth", "max-depth")));
        assertEquals("stableVersion", Suggestions.closest("StableVersion", Collections.singletonList("stableVersion")));
    }

    @Test
    public void testShortNamesAllowOnlyOneEdit() {
        assertNull(Suggestions.closest("doh", Arrays.asList("todo", "date")));
        assertEquals("ru", Suggestions.closest("rus", Arrays.asList("en", "ru")));
    }

    @Test
    public void testNoSuggestionForExactOrMissing() {
        assertNull(Suggestions.closest("toc", Collections.singletonList("toc")));
        assertNull(Suggestions.closest("zzzzzz", Arrays.asList("toc", "date")));
        assertNull(Suggestions.closest("x", null));
    }

    @Test
    public void testTiesResolveAlphabetically() {
        assertEquals("ab", Suggestions.closest("aa", Arrays.asList("ba", "ab")));
    }
}
