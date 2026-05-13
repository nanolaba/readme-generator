package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import org.apache.commons.text.TextStringBuilder;

import java.util.Collections;
import java.util.Set;

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

    /**
     * Alternative names that also resolve to this widget. Used by per-line dispatch
     * (so {@code ${widget:toc(...)}} reaches {@code TableOfContentsWidget}), by the
     * validator (so closer pseudo-names like {@code endIf} aren't flagged as unknown),
     * and by conflict detection at config-init time.
     *
     * <p>Returning an empty set (the default) means the widget is only addressable by its
     * primary {@link #getName()}.
     *
     * <p>Aliases must be globally unique across all registered widgets, including primary
     * names — {@link GeneratorConfig} throws {@link IllegalStateException} on collision.
     */
    default Set<String> getAliases() {
        return Collections.emptySet();
    }

    String getBody(WidgetTag widgetTag, GeneratorConfig config, String language);

    default void beforeRenderLine(TextStringBuilder line) {
    }

    default void afterRenderLine(TextStringBuilder line, GeneratorConfig config) {
    }

    boolean isEnabled();

    void setEnabled(boolean enabled);

}
