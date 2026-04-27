package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImportHeadingShifterTest {

    @Test
    void offsetZeroReturnsContentUnchanged() {
        String input = "# A" + "\n" + "## B" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 0);
        assertEquals(input, r.content);
        assertEquals(0, r.clampedCount);
        assertNull(r.firstClampedLine);
    }

    @Test
    void positiveOffsetShiftsAllAtxHeadings() {
        String input = "# A" + "\n" + "## B" + "\n" + "### C" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("## A" + "\n" + "### B" + "\n" + "#### C" + "\n", r.content);
        assertEquals(0, r.clampedCount);
        assertNull(r.firstClampedLine);
    }

    @Test
    void positiveOffsetTwoShiftsCorrectly() {
        String input = "# A" + "\n" + "## B" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 2);
        assertEquals("### A" + "\n" + "#### B" + "\n", r.content);
    }

    @Test
    void preservesHeadingTextAndTrailingWhitespace() {
        String input = "# A heading with  spaces  " + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("## A heading with  spaces  " + "\n", r.content);
    }

    @Test
    void doesNotShiftLinesThatLookLikeHeadingsButHaveNoSpaceAfterHash() {
        String input = "#foo" + "\n" + "# bar" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("#foo" + "\n" + "## bar" + "\n", r.content);
    }

    @Test
    void doesNotShiftLinesContainingHashButNotAtStart() {
        String input = "Use the # operator" + "\n" + "[link](https://x.y/#anchor)" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(input, r.content);
    }

    @Test
    void shiftsLineWithOnlyHashes() {
        String input = "##" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("###" + "\n", r.content);
    }

    @Test
    void negativeOffsetShiftsLevelsDown() {
        String input = "## A" + "\n" + "### B" + "\n" + "#### C" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, -1);
        assertEquals("# A" + "\n" + "## B" + "\n" + "### C" + "\n", r.content);
        assertEquals(0, r.clampedCount);
    }

    @Test
    void positiveOffsetClampsHeadingsAtH6() {
        String input = "##### A" + "\n" + "###### B" + "\n" + "## C" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 2);
        assertEquals("###### A" + "\n" + "###### B" + "\n" + "#### C" + "\n", r.content);
        // Both A (target 7) and B (target 8) clamp to 6; C (target 4) is unaffected.
        assertEquals(2, r.clampedCount);
        assertEquals("##### A", r.firstClampedLine);
    }

    @Test
    void negativeOffsetClampsHeadingsAtH1() {
        String input = "# A" + "\n" + "## B" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, -2);
        assertEquals("# A" + "\n" + "# B" + "\n", r.content);
        assertEquals(2, r.clampedCount);
        assertEquals("# A", r.firstClampedLine);
    }

    @Test
    void firstClampedLineIsTheFirstNotTheLast() {
        String input = "###### X" + "\n" + "###### Y" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("###### X" + "\n" + "###### Y" + "\n", r.content);
        assertEquals(2, r.clampedCount);
        assertEquals("###### X", r.firstClampedLine);
    }

    @Test
    void hugePositiveOffsetClampsEveryHeading() {
        String input = "# A" + "\n" + "## B" + "\n" + "### C" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 100);
        assertEquals("###### A" + "\n" + "###### B" + "\n" + "###### C" + "\n", r.content);
        assertEquals(3, r.clampedCount);
    }

    @Test
    void backtickFencedCodeBlockContentIsNotShifted() {
        String input =
                "# Real heading" + "\n" +
                "```bash" + "\n" +
                "# this is a shell comment" + "\n" +
                "## still a comment" + "\n" +
                "```" + "\n" +
                "## Another heading" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(
                "## Real heading" + "\n" +
                "```bash" + "\n" +
                "# this is a shell comment" + "\n" +
                "## still a comment" + "\n" +
                "```" + "\n" +
                "### Another heading" + "\n",
                r.content);
        assertEquals(0, r.clampedCount);
    }

    @Test
    void unclosedBacktickFenceTreatsRestOfFileAsCode() {
        String input =
                "# Heading" + "\n" +
                "```" + "\n" +
                "# code line" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(
                "## Heading" + "\n" +
                "```" + "\n" +
                "# code line" + "\n",
                r.content);
    }

    @Test
    void multipleSeparateFencesAreTrackedIndependently() {
        String input =
                "```" + "\n" +
                "# in fence 1" + "\n" +
                "```" + "\n" +
                "# between" + "\n" +
                "```" + "\n" +
                "# in fence 2" + "\n" +
                "```" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(
                "```" + "\n" +
                "# in fence 1" + "\n" +
                "```" + "\n" +
                "## between" + "\n" +
                "```" + "\n" +
                "# in fence 2" + "\n" +
                "```" + "\n",
                r.content);
    }

    @Test
    void tildeFencedCodeBlockContentIsNotShifted() {
        String input =
                "# Real" + "\n" +
                "~~~bash" + "\n" +
                "# comment" + "\n" +
                "~~~" + "\n" +
                "## After" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(
                "## Real" + "\n" +
                "~~~bash" + "\n" +
                "# comment" + "\n" +
                "~~~" + "\n" +
                "### After" + "\n",
                r.content);
    }

    @Test
    void mixedFenceCharactersAreNotConfused() {
        String input =
                "~~~" + "\n" +
                "```" + "\n" +
                "# inside" + "\n" +
                "```" + "\n" +
                "~~~" + "\n" +
                "# after" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(
                "~~~" + "\n" +
                "```" + "\n" +
                "# inside" + "\n" +
                "```" + "\n" +
                "~~~" + "\n" +
                "## after" + "\n",
                r.content);
    }

    @Test
    void longerOpeningFenceRequiresAtLeastSameLengthClose() {
        String input =
                "````" + "\n" +
                "# inside" + "\n" +
                "```" + "\n" +
                "# still inside" + "\n" +
                "````" + "\n" +
                "# outside" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals(
                "````" + "\n" +
                "# inside" + "\n" +
                "```" + "\n" +
                "# still inside" + "\n" +
                "````" + "\n" +
                "## outside" + "\n",
                r.content);
    }

    @Test
    void preservesTrailingNewline() {
        String input = "# A" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("## A" + "\n", r.content);
    }

    @Test
    void preservesAbsenceOfTrailingNewline() {
        String input = "# A";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("## A", r.content);
    }

    @Test
    void preservesCrlfLineSeparators() {
        String input = "# A" + "\r\n" + "## B" + "\r\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("## A" + "\r\n" + "### B" + "\r\n", r.content);
    }

    @Test
    void shiftsHeadingWithTabSeparator() {
        String input = "#" + "\t" + "A" + "\n";
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift(input, 1);
        assertEquals("##" + "\t" + "A" + "\n", r.content);
    }

    @Test
    void emptyInputReturnsEmpty() {
        ImportHeadingShifter.Result r = ImportHeadingShifter.shift("", 1);
        assertEquals("", r.content);
        assertEquals(0, r.clampedCount);
        assertNull(r.firstClampedLine);
    }
}
