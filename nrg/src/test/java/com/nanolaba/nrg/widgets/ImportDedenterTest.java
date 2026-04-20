package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportDedenterTest {

    @Test
    void stripsCommonSpacePrefix() {
        List<String> input = Arrays.asList("    void foo() {", "        bar();", "    }");
        List<String> expected = Arrays.asList("void foo() {", "    bar();", "}");
        assertEquals(expected, ImportDedenter.dedent(input));
    }

    @Test
    void stripsCommonTabPrefix() {
        List<String> input = Arrays.asList("\t\tline1", "\t\t\tline2", "\t\tline3");
        List<String> expected = Arrays.asList("line1", "\tline2", "line3");
        assertEquals(expected, ImportDedenter.dedent(input));
    }

    @Test
    void mixedTabsAndSpacesIsNoOp() {
        List<String> input = Arrays.asList("    line1", "\tline2");
        assertEquals(input, ImportDedenter.dedent(input));
    }

    @Test
    void emptyLinesDoNotAffectMinimum() {
        List<String> input = Arrays.asList("    line1", "", "    line2");
        List<String> expected = Arrays.asList("line1", "", "line2");
        assertEquals(expected, ImportDedenter.dedent(input));
    }

    @Test
    void whitespaceOnlyLinesDoNotAffectMinimum() {
        List<String> input = Arrays.asList("    line1", "  ", "    line2");
        List<String> result = ImportDedenter.dedent(input);
        assertEquals("line1", result.get(0));
        assertEquals("line2", result.get(2));
        // Whitespace-only line truncated to its length (no negative length)
        assertEquals("", result.get(1));
    }

    @Test
    void noCommonPrefixIsNoOp() {
        List<String> input = Arrays.asList("line1", "    line2");
        assertEquals(input, ImportDedenter.dedent(input));
    }

    @Test
    void singleLineWithIndentIsDedented() {
        List<String> input = Collections.singletonList("    line");
        assertEquals(Collections.singletonList("line"), ImportDedenter.dedent(input));
    }

    @Test
    void allEmptyLinesIsNoOp() {
        List<String> input = Arrays.asList("", "", "");
        assertEquals(input, ImportDedenter.dedent(input));
    }

    @Test
    void emptyListReturnsEmpty() {
        assertEquals(Collections.emptyList(), ImportDedenter.dedent(Collections.emptyList()));
    }
}
