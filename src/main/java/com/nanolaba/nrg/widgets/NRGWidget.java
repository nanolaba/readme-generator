package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.GeneratorConfig;

public interface NRGWidget {

    String getName();

    String getBody(WidgetTag widgetTag, GeneratorConfig config, String language);
}
