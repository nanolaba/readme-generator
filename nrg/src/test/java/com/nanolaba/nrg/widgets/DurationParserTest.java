package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationParserTest {

    @Test
    void parsesSeconds() {
        assertEquals(30_000L, DurationParser.parseMillis("30s"));
    }

    @Test
    void parsesMinutes() {
        assertEquals(60_000L * 5, DurationParser.parseMillis("5m"));
    }

    @Test
    void parsesHours() {
        assertEquals(3_600_000L, DurationParser.parseMillis("1h"));
    }

    @Test
    void parsesDays() {
        assertEquals(7L * 24 * 3_600_000L, DurationParser.parseMillis("7d"));
    }

    @Test
    void noneReturnsNegativeOne() {
        assertEquals(-1L, DurationParser.parseMillis("none"));
    }

    @Test
    void rejectsFractional() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("1.5h"));
    }

    @Test
    void rejectsCompound() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("1h30m"));
    }

    @Test
    void rejectsBareNumber() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("90"));
    }

    @Test
    void rejectsUnknownUnit() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("1week"));
    }

    @Test
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis(""));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis(null));
    }

    @Test
    void rejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("0s"));
    }

    @Test
    void rejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("-1h"));
    }
}
