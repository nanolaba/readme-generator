package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LogEntry;
import com.nanolaba.logging.MemoryLogger;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.SourceOrigin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockWidgetTest extends DefaultNRGTest {

    /**
     * Minimal test-only subclass. Emits {@code <<<echo>>>} for openers and {@code <<</echo>>>}
     * for closers. Recognises {@code drop='true'} (returns active=false from onBlockOpen)
     * and {@code inline='true'} (treated as inline form, opener line passed through).
     */
    private static final class EchoBlockWidget extends BlockWidget {
        final List<Integer> openerLines = new ArrayList<>();
        final List<Integer> closerLines = new ArrayList<>();

        @Override
        public String getName() {
            return "echo";
        }

        @Override
        protected BlockOpening onBlockOpen(Map<String, String> params, GeneratorConfig config,
                                           String language, boolean parentActive, int lineNo) {
            openerLines.add(lineNo);
            boolean active = !"true".equals(params.get("drop"));
            return BlockOpening.of(Collections.singletonList("<<<echo>>>"), active);
        }

        @Override
        protected List<String> onBlockClose(int lineNo) {
            closerLines.add(lineNo);
            return Collections.singletonList("<<</echo>>>");
        }

        @Override
        protected boolean isInlineForm(Map<String, String> params) {
            return "true".equals(params.get("inline"));
        }

        @Override
        protected String renderInline(Map<String, String> params, GeneratorConfig config, String language) {
            return "INLINE";
        }
    }

    private static GeneratorConfig config(String body) {
        return new GeneratorConfig(new File("README.src.md"), body, null);
    }

    private static String process(String body) {
        return new EchoBlockWidget().processBlocks(body, config(body), "en");
    }

    private static List<SourceOrigin> originsFor(int lines) {
        List<SourceOrigin> origins = new ArrayList<>();
        for (int i = 1; i <= lines; i++) {
            origins.add(SourceOrigin.of(new File("README.src.md"), i));
        }
        return origins;
    }

    @Test
    public void testSimpleBlockRewritten() {
        String body = "before\n${widget:echo}\nINNER\n${widget:endEcho}\nafter\n";
        String out = process(body);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("<<<echo>>>"), out);
        assertTrue(out.contains("INNER"), out);
        assertTrue(out.contains("<<</echo>>>"), out);
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:echo"), out);
        assertFalse(out.contains("widget:endEcho"), out);
    }

    @Test
    public void testInactiveOpenerDropsInnerContent() {
        String body = "before\n${widget:echo(drop='true')}\nINNER\n${widget:endEcho}\nafter\n";
        String out = process(body);
        assertTrue(out.contains("before"));
        // Both opener and closer emit because the enclosing scope (no parent — outer level) is
        // active. The frame's own activity controls only whether INNER content lines pass through.
        assertTrue(out.contains("<<<echo>>>"), out);
        assertFalse(out.contains("INNER"), out);
        assertTrue(out.contains("<<</echo>>>"), out);
        assertTrue(out.contains("after"));
    }

    @Test
    public void testNestedBlocksTrackStackDepth() {
        String body = "${widget:echo}\nOUTER\n${widget:echo}\nINNER\n${widget:endEcho}\nTAIL\n${widget:endEcho}\n";
        String out = process(body);
        assertTrue(out.contains("OUTER"), out);
        assertTrue(out.contains("INNER"), out);
        assertTrue(out.contains("TAIL"), out);
        int openCount = (out.length() - out.replace("<<<echo>>>", "").length()) / "<<<echo>>>".length();
        int closeCount = (out.length() - out.replace("<<</echo>>>", "").length()) / "<<</echo>>>".length();
        assertEquals(2, openCount, out);
        assertEquals(2, closeCount, out);
    }

    @Test
    public void testParentInactivePropagates() {
        String body = "${widget:echo(drop='true')}\n" +
                "${widget:echo}\nINNER\n${widget:endEcho}\n" +
                "${widget:endEcho}\n";
        String out = process(body);
        int openCount = (out.length() - out.replace("<<<echo>>>", "").length()) / "<<<echo>>>".length();
        int closeCount = (out.length() - out.replace("<<</echo>>>", "").length()) / "<<</echo>>>".length();
        assertEquals(1, openCount, "outer opener emits; inner opener suppressed: " + out);
        assertEquals(1, closeCount, "outer closer emits; inner closer suppressed: " + out);
        assertFalse(out.contains("INNER"), out);
    }

    @Test
    public void testStrayCloserLogsAndDrops() {
        String body = "before\n${widget:endEcho}\nafter\n";
        String out = process(body);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("after"));
        assertFalse(out.contains("widget:endEcho"), out);
        assertTrue(getErrAndClear().contains("echo widget: unmatched stray"));
    }

    @Test
    public void testUnclosedBlockRollsBack() {
        String body = "before\n${widget:echo}\nINNER\n";
        String out = process(body);
        assertTrue(out.contains("before"));
        assertFalse(out.contains("<<<echo>>>"), out);
        assertFalse(out.contains("INNER"), out);
        assertTrue(getErrAndClear().contains("echo widget: unclosed"));
    }

    @Test
    public void testInlineFormOpenerPassedThrough() {
        String body = "before\n${widget:echo(inline='true')}\nafter\n";
        String out = process(body);
        assertTrue(out.contains("before"));
        assertTrue(out.contains("${widget:echo(inline='true')}"), out);
        assertTrue(out.contains("after"));
        assertFalse(out.contains("<<<echo>>>"), out);
    }

    @Test
    public void testBodyWithoutMarkersUnchanged() {
        String body = "line 1\nline 2\nline 3\n";
        String out = process(body);
        assertEquals(body, out);
    }

    @Test
    public void testNullBodyIsReturnedAsNull() {
        assertNull(new EchoBlockWidget().processBlocks(null, config(""), "en"));
    }

    @Test
    public void testBlockOpenerReachingPerLineDispatchLogsError() {
        EchoBlockWidget echo = new EchoBlockWidget();
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals("", echo.getBody(new WidgetTag(null, "echo", "", 0, 0), config(""), "en"));
            log.assertLogged(LogEntry.LogEntryLevel.ERROR,
                    "echo widget: opener without inline-form parameter must be closed by");
            log.assertLogged(LogEntry.LogEntryLevel.ERROR, "widget:endEcho");
        }
    }

    @Test
    public void testHooksReceiveOneBasedLineNumbers() {
        EchoBlockWidget echo = new EchoBlockWidget();
        String body = "before\n${widget:echo}\nINNER\n${widget:endEcho}\n";
        echo.processBlocks(body, config(body), "en");
        assertEquals(Collections.singletonList(2), echo.openerLines);
        assertEquals(Collections.singletonList(4), echo.closerLines);
    }

    @Test
    public void testStrayCloserReportsItsLineNumber() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            process("before\n${widget:endEcho}\nafter\n");
            log.assertLogged(LogEntry.LogEntryLevel.ERROR, "echo widget: unmatched stray");
            log.assertLogged(LogEntry.LogEntryLevel.ERROR, "at line 2");
        }
    }

    @Test
    public void testUnclosedOuterBlockRollsBackPastClosedInnerBlock() {
        String body = "before\n${widget:echo}\nX\n${widget:echo}\nY\n${widget:endEcho}\nZ";
        assertEquals("before", process(body));
    }

    @Test
    public void testUnclosedBlockOnFirstLineRollsBackEverything() {
        assertEquals("", process("${widget:echo}\nINNER"));
    }

    @Test
    public void testOriginsFollowRewrittenLines() {
        String body = "a\n${widget:echo(inline='true')}\n${widget:echo}\nb\n${widget:endEcho}";
        List<SourceOrigin> origins = originsFor(5);
        List<SourceOrigin> expected = new ArrayList<>(origins);

        String out = new EchoBlockWidget().processBlocks(body, config(body), "en", origins);

        assertEquals("a\n${widget:echo(inline='true')}\n<<<echo>>>\nb\n<<</echo>>>", out);
        assertEquals(expected, origins);
    }

    @Test
    public void testOriginsOfRolledBackLinesAreDropped() {
        String body = "a\n${widget:echo}\nb";
        List<SourceOrigin> origins = originsFor(3);
        SourceOrigin first = origins.get(0);

        String out = new EchoBlockWidget().processBlocks(body, config(body), "en", origins);

        assertEquals("a", out);
        assertEquals(Collections.singletonList(first), origins);
    }

    @Test
    public void testLinesBeyondTheOriginsListGetNoOrigin() {
        String body = "a\n${widget:echo}\nb\n${widget:endEcho}";
        List<SourceOrigin> origins = originsFor(1);
        SourceOrigin first = origins.get(0);

        new EchoBlockWidget().processBlocks(body, config(body), "en", origins);

        assertEquals(Arrays.asList(first, null, null, null), origins);
    }

    @Test
    public void testCloserNameIsTheOnlyAlias() {
        assertEquals(Collections.singleton("endEcho"), new EchoBlockWidget().getAliases());
    }
}
