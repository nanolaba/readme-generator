package com.nanolaba.nrg.examples;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.widgets.DefaultWidget;
import com.nanolaba.nrg.widgets.WidgetTag;

import java.util.Map;

public class ExampleWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "exampleWidget";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        String parameters = widgetTag.getParameters();
        Map<String, String> map = NRGUtil.parseParametersLine(parameters);

        return "Hello, " + map.get("name") + "!";
    }
}
