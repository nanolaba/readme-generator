package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;

public class LanguagesWidget implements NRGWidget {

    @Override
    public String getName() {
        return "languages";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        return getClass() + ": TODO";
    }
}
