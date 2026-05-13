package com.nanolaba.nrg.widgets;

import java.util.Collections;
import java.util.List;

/**
 * Return type of {@link BlockWidget#onBlockOpen}. Describes the lines a block widget wants
 * emitted in place of an opener marker, plus whether subsequent inner content should pass
 * through the output (active) or be dropped (inactive).
 *
 * <p>Widgets that never produce inactive blocks (e.g. {@code details} — it always renders
 * its inner content) always return {@code active=true}. Widgets like {@code if} use the
 * active flag to drop the false branch.
 *
 * <p><b>NRG internal API; subject to change in minor releases.</b>
 */
public final class BlockOpening {

    private final List<String> emittedLines;
    private final boolean contentActive;

    private BlockOpening(List<String> emittedLines, boolean contentActive) {
        this.emittedLines = emittedLines;
        this.contentActive = contentActive;
    }

    public static BlockOpening of(List<String> emittedLines, boolean contentActive) {
        return new BlockOpening(emittedLines, contentActive);
    }

    /** Convenience for widgets that emit no replacement lines (e.g. {@code if}). */
    public static BlockOpening empty(boolean contentActive) {
        return new BlockOpening(Collections.<String>emptyList(), contentActive);
    }

    public List<String> getEmittedLines() {
        return emittedLines;
    }

    public boolean isContentActive() {
        return contentActive;
    }
}
