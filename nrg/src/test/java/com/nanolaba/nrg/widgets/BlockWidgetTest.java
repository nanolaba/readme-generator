package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.GeneratorConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockWidgetTest extends DefaultNRGTest {

    /**
     * Minimal test-only subclass. Emits {@code <<<echo>>>} for openers and {@code <<</echo>>>}
     * for closers. Recognises {@code drop='true'} (returns active=false from onBlockOpen)
     * and {@code inline='true'} (treated as inline form, opener line passed through).
     */
    private static final class EchoBlockWidget extends BlockWidget {
        @Override
        public String getName() {
            return "echo";
        }

        @Override
        protected BlockOpening onBlockOpen(Map<String, String> params, GeneratorConfig config,
                                           String language, boolean parentActive, int lineNo) {
            boolean active = !"true".equals(params.get("drop"));
            return BlockOpening.of(Collections.singletonList("<<<echo>>>"), active);
        }

        @Override
        protected List<String> onBlockClose(int lineNo) {
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

    private static String process(String body) {
        GeneratorConfig cfg = new GeneratorConfig(new File("README.src.md"), body, null);
        EchoBlockWidget echo = new EchoBlockWidget();
        return echo.processBlocks(body, cfg, "en");
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
}
