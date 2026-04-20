package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImportRegionExtractorTest {

    @Test
    void simpleRegion() {
        List<String> input = Arrays.asList(
                "before",
                "// nrg:begin:foo",
                "inside1",
                "inside2",
                "// nrg:end:foo",
                "after");
        assertEquals(Arrays.asList("inside1", "inside2"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void regionNotFoundReturnsNull() {
        List<String> input = Arrays.asList("a", "b", "c");
        assertNull(ImportRegionExtractor.extract(input, "missing"));
    }

    @Test
    void unclosedRegionReturnsNull() {
        List<String> input = Arrays.asList("// nrg:begin:foo", "inside", "no end");
        assertNull(ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void emptyRegion() {
        List<String> input = Arrays.asList(
                "// nrg:begin:foo",
                "// nrg:end:foo");
        assertEquals(Collections.emptyList(), ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void nestedRegionMarkersStripped() {
        List<String> input = Arrays.asList(
                "// nrg:begin:outer",
                "outer-line-1",
                "// nrg:begin:inner",
                "inner-line",
                "// nrg:end:inner",
                "outer-line-2",
                "// nrg:end:outer");
        assertEquals(Arrays.asList("outer-line-1", "inner-line", "outer-line-2"),
                ImportRegionExtractor.extract(input, "outer"));
    }

    @Test
    void canExtractInnerRegionDirectly() {
        List<String> input = Arrays.asList(
                "// nrg:begin:outer",
                "// nrg:begin:inner",
                "inner-line",
                "// nrg:end:inner",
                "// nrg:end:outer");
        assertEquals(Collections.singletonList("inner-line"),
                ImportRegionExtractor.extract(input, "inner"));
    }

    @Test
    void duplicateBeginUsesFirst() {
        List<String> input = Arrays.asList(
                "// nrg:begin:foo",
                "first",
                "// nrg:begin:foo",
                "second",
                "// nrg:end:foo");
        // First begin wins; nested same-name begin is just a stripped marker
        assertEquals(Arrays.asList("first", "second"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void multipleMarkersOnSameLineStripped() {
        List<String> input = Arrays.asList(
                "// nrg:begin:foo",
                "a",
                "// nrg:end:foo nrg:begin:bar",
                "b",
                "// nrg:end:bar");
        assertEquals(Collections.singletonList("a"),
                ImportRegionExtractor.extract(input, "foo"));
        assertEquals(Collections.singletonList("b"),
                ImportRegionExtractor.extract(input, "bar"));
    }

    @Test
    void worksWithPythonStyleComments() {
        List<String> input = Arrays.asList(
                "# nrg:begin:foo",
                "py-line",
                "# nrg:end:foo");
        assertEquals(Collections.singletonList("py-line"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void worksWithHtmlStyleComments() {
        List<String> input = Arrays.asList(
                "<!-- nrg:begin:foo -->",
                "html-line",
                "<!-- nrg:end:foo -->");
        assertEquals(Collections.singletonList("html-line"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void worksWithSqlStyleComments() {
        List<String> input = Arrays.asList(
                "-- nrg:begin:foo",
                "sql-line",
                "-- nrg:end:foo");
        assertEquals(Collections.singletonList("sql-line"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void worksWithCssBlockComments() {
        List<String> input = Arrays.asList(
                "/* nrg:begin:foo */",
                "css-line",
                "/* nrg:end:foo */");
        assertEquals(Collections.singletonList("css-line"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void whitespaceVariantsNotMatched() {
        // "nrg : begin : foo" is NOT a marker — only exact "nrg:begin:foo"
        List<String> input = Arrays.asList(
                "// nrg : begin : foo",
                "line",
                "// nrg : end : foo");
        assertNull(ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void regionAtFileStart() {
        List<String> input = Arrays.asList(
                "// nrg:begin:foo",
                "first",
                "// nrg:end:foo",
                "after");
        assertEquals(Collections.singletonList("first"),
                ImportRegionExtractor.extract(input, "foo"));
    }

    @Test
    void regionAtFileEnd() {
        List<String> input = Arrays.asList(
                "before",
                "// nrg:begin:foo",
                "last",
                "// nrg:end:foo");
        assertEquals(Collections.singletonList("last"),
                ImportRegionExtractor.extract(input, "foo"));
    }
}
