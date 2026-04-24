package com.nanolaba.nrg.core;

import com.nanolaba.nrg.DefaultNRGTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IgnoreBlockStripperTest extends DefaultNRGTest {

    @Test
    public void testNullAndEmpty() {
        assertNull(IgnoreBlockStripper.strip(null));
        assertEquals("", IgnoreBlockStripper.strip(""));
    }

    @Test
    public void testLeavesNormalContentUntouched() {
        assertEquals("a\nb\nc", IgnoreBlockStripper.strip("a\nb\nc"));
    }

    @Test
    public void testRemovesInlineIgnoreLine() {
        assertEquals("a\nc",
                IgnoreBlockStripper.strip("a\nb<!--nrg.ignore-->\nc"));
    }

    @Test
    public void testRemovesInlineIgnoreLineWithOnlyMarker() {
        assertEquals("a\nc",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore-->\nc"));
    }

    @Test
    public void testRemovesInlineIgnoreWithTrailingText() {
        assertEquals("a\nc",
                IgnoreBlockStripper.strip("a\nb<!--nrg.ignore-->trailing\nc"));
    }

    @Test
    public void testRemovesSimpleBlock() {
        assertEquals("a\nd",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore.begin-->\nb\nc\n<!--nrg.ignore.end-->\nd"));
    }

    @Test
    public void testRemovesMultipleBlocks() {
        String src = "a\n<!--nrg.ignore.begin-->\nx\n<!--nrg.ignore.end-->\nb\n<!--nrg.ignore.begin-->\ny\n<!--nrg.ignore.end-->\nc";
        assertEquals("a\nb\nc", IgnoreBlockStripper.strip(src));
    }

    @Test
    public void testEmptyBlock() {
        assertEquals("a\nb",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore.begin-->\n<!--nrg.ignore.end-->\nb"));
    }

    @Test
    public void testBlockOnSameLineAsOtherContent() {
        assertEquals("a\nd",
                IgnoreBlockStripper.strip("a\nstart<!--nrg.ignore.begin-->\nb\nc<!--nrg.ignore.end-->end\nd"));
    }

    @Test
    public void testUnclosedBeginDropsToEofAndLogsError() {
        assertEquals("a",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore.begin-->\nb\nc"));
        assertEquals(true, getErrAndClear().contains("Unclosed nrg.ignore.begin"));
    }

    @Test
    public void testExtraEndLogsErrorAndDropsMarker() {
        assertEquals("a\nb",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore.end-->\nb"));
        assertEquals(true, getErrAndClear().contains("nrg.ignore.end without matching"));
    }

    @Test
    public void testEscapedInlineMarkerIsPreserved() {
        assertEquals("a\nb<\\!--nrg.ignore-->\nc",
                IgnoreBlockStripper.strip("a\nb<\\!--nrg.ignore-->\nc"));
    }

    @Test
    public void testEscapedBackslashBeforeAngle() {
        assertEquals("a\n\\<!--nrg.ignore-->\nc",
                IgnoreBlockStripper.strip("a\n\\<!--nrg.ignore-->\nc"));
    }

    @Test
    public void testEscapedBeginMarkerIsPreserved() {
        String src = "a\n<\\!--nrg.ignore.begin-->\nb\n<\\!--nrg.ignore.end-->\nc";
        assertEquals(src, IgnoreBlockStripper.strip(src));
    }

    @Test
    public void testWhitespaceInsideMarker() {
        assertEquals("a\nc",
                IgnoreBlockStripper.strip("a\n<!--  nrg.ignore  -->\nc"));
        assertEquals("a\nd",
                IgnoreBlockStripper.strip("a\n<!--  nrg.ignore.begin  -->\nb\n<!--  nrg.ignore.end  -->\nd"));
    }

    @Test
    public void testPreservesTrailingNewline() {
        assertEquals("a\n", IgnoreBlockStripper.strip("a\n"));
        assertEquals("a\n",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore-->\n"));
    }

    @Test
    public void testWindowsLineEndingsNormalisedToLf() {
        assertEquals("a\nc",
                IgnoreBlockStripper.strip("a\r\nb<!--nrg.ignore-->\r\nc"));
    }

    @Test
    public void testNestedBeginInsideBlockIsIgnored() {
        assertEquals("a\nd",
                IgnoreBlockStripper.strip("a\n<!--nrg.ignore.begin-->\n<!--nrg.ignore.begin-->\nb\n<!--nrg.ignore.end-->\nd"));
    }
}
