package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import org.apache.commons.text.TextStringBuilder;

public interface NRGWidget {

    String getName();

    String getBody(WidgetTag widgetTag, GeneratorConfig config, String language);

    default void beforeRenderLine(TextStringBuilder line) {
    }

    default void afterRenderLine(TextStringBuilder line, GeneratorConfig config) {
    }
}
