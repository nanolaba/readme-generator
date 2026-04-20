package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportLinesSpecParserTest {

    @Test
    void simpleRange() {
        ImportLinesSpec s = ImportLinesSpec.parse("10-20");
        assertEquals(1, s.getRanges().size());
        assertEquals(10, s.getRanges().get(0)[0]);
        assertEquals(20, s.getRanges().get(0)[1]);
    }

    @Test
    void openEnd() {
        ImportLinesSpec s = ImportLinesSpec.parse("10-");
        assertEquals(10, s.getRanges().get(0)[0]);
        assertEquals(Integer.MAX_VALUE, s.getRanges().get(0)[1]);
    }

    @Test
    void openStart() {
        ImportLinesSpec s = ImportLinesSpec.parse("-20");
        assertEquals(1, s.getRanges().get(0)[0]);
        assertEquals(20, s.getRanges().get(0)[1]);
    }

    @Test
    void singleLine() {
        ImportLinesSpec s = ImportLinesSpec.parse("15");
        assertEquals(15, s.getRanges().get(0)[0]);
        assertEquals(15, s.getRanges().get(0)[1]);
    }

    @Test
    void multipleRanges() {
        ImportLinesSpec s = ImportLinesSpec.parse("10-20,30-35");
        assertEquals(2, s.getRanges().size());
        assertEquals(10, s.getRanges().get(0)[0]);
        assertEquals(20, s.getRanges().get(0)[1]);
        assertEquals(30, s.getRanges().get(1)[0]);
        assertEquals(35, s.getRanges().get(1)[1]);
    }

    @Test
    void multipleRangesWithSpaces() {
        ImportLinesSpec s = ImportLinesSpec.parse("10-20 , 30-35");
        assertEquals(2, s.getRanges().size());
    }

    @Test
    void zeroRangeIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse("0-5"));
    }

    @Test
    void negativeRangeIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse("-5-10"));
    }

    @Test
    void nonNumericIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse("abc"));
    }

    @Test
    void doubleDashIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse("5--7"));
    }

    @Test
    void reversedRangeIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse("20-10"));
    }

    @Test
    void emptyStringIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse(""));
    }

    @Test
    void onlyCommaIsError() {
        assertThrows(IllegalArgumentException.class, () -> ImportLinesSpec.parse(","));
    }

    @Test
    void overlappingRangesAccepted() {
        ImportLinesSpec s = ImportLinesSpec.parse("5-10,7-12");
        assertEquals(2, s.getRanges().size());
    }
}
