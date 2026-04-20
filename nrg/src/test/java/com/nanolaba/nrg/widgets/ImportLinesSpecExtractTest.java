package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportLinesSpecExtractTest {

    private final List<String> tenLines = Arrays.asList(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    @Test
    void midRange() {
        List<String> result = ImportLinesSpec.parse("3-5").apply(tenLines);
        assertEquals(Arrays.asList("3", "4", "5"), result);
    }

    @Test
    void openEndClampedToFile() {
        List<String> result = ImportLinesSpec.parse("8-").apply(tenLines);
        assertEquals(Arrays.asList("8", "9", "10"), result);
    }

    @Test
    void openStart() {
        List<String> result = ImportLinesSpec.parse("-3").apply(tenLines);
        assertEquals(Arrays.asList("1", "2", "3"), result);
    }

    @Test
    void singleLine() {
        List<String> result = ImportLinesSpec.parse("5").apply(tenLines);
        assertEquals(Collections.singletonList("5"), result);
    }

    @Test
    void multipleRangesConcatenatedNoSeparator() {
        List<String> result = ImportLinesSpec.parse("2-3,7-8").apply(tenLines);
        assertEquals(Arrays.asList("2", "3", "7", "8"), result);
    }

    @Test
    void overlappingRangesDuplicateContent() {
        List<String> result = ImportLinesSpec.parse("3-5,4-6").apply(tenLines);
        assertEquals(Arrays.asList("3", "4", "5", "4", "5", "6"), result);
    }

    @Test
    void rangePartiallyPastEndIsClampedSilently() {
        List<String> result = ImportLinesSpec.parse("8-100").apply(tenLines);
        assertEquals(Arrays.asList("8", "9", "10"), result);
    }

    @Test
    void rangeFullyPastEndYieldsEmpty() {
        List<String> result = ImportLinesSpec.parse("100-200").apply(tenLines);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void rangeFullyPastEndInMultipleStillProcessesOthers() {
        List<String> result = ImportLinesSpec.parse("2-3,100-200,7-8").apply(tenLines);
        assertEquals(Arrays.asList("2", "3", "7", "8"), result);
    }

    @Test
    void emptyFile() {
        List<String> result = ImportLinesSpec.parse("1-5").apply(Collections.emptyList());
        assertEquals(Collections.emptyList(), result);
    }
}
