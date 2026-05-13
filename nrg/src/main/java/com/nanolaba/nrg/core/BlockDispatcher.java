package com.nanolaba.nrg.core;

import com.nanolaba.nrg.widgets.BlockWidget;
import com.nanolaba.nrg.widgets.NRGWidget;

/**
 * Runs every registered {@link BlockWidget}'s pre-pass over the source body, in widget
 * registration order, before per-line rendering. Replaces the historical if / details
 * block pre-pass pair in {@link Generator#generateContent}.
 *
 * <p>Each widget's pre-pass operates on the output of the previous one. Disabled widgets
 * ({@link NRGWidget#isEnabled()} returns {@code false}) are skipped — the toc widget uses
 * this same convention when running its secondary render pass.
 */
final class BlockDispatcher {

    private BlockDispatcher() {/* utility */}

    static String process(String body, GeneratorConfig config, String language) {
        for (NRGWidget w : config.getWidgets()) {
            if (w instanceof BlockWidget && w.isEnabled()) {
                body = ((BlockWidget) w).processBlocks(body, config, language);
            }
        }
        return body;
    }
}
