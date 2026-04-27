package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import org.apache.commons.text.TextStringBuilder;

/**
 * Plug-in contract for the {@code ${widget:name(params)}} extension point.
 *
 * <p>An NRG widget owns a globally unique {@link #getName() name} and produces a string
 * replacement for each tag occurrence via {@link #getBody(WidgetTag, GeneratorConfig, String)}.
 * Implementations may also intercept the surrounding line through the optional
 * {@link #beforeRenderLine} / {@link #afterRenderLine} hooks, e.g. for cross-tag bookkeeping
 * or output post-processing (see {@code TableOfContentsWidget} for the canonical example).
 *
 * <p>Custom widgets are registered programmatically via {@code NRG.addWidget(...)}, declared
 * via the {@code <!--@nrg.widgets=fqcn,fqcn-->} property, supplied through {@code --widgets},
 * or listed in the Maven plugin's {@code <widgets>} configuration. They must declare a public
 * no-argument constructor to be loadable reflectively.
 */
public interface NRGWidget {

    String getName();

    String getBody(WidgetTag widgetTag, GeneratorConfig config, String language);

    default void beforeRenderLine(TextStringBuilder line) {
    }

    default void afterRenderLine(TextStringBuilder line, GeneratorConfig config) {
    }

    boolean isEnabled();

    void setEnabled(boolean enabled);

}
