package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.Map;

public class TodoWidget extends DefaultWidget {

    public static final String HOURGLASS = "⌛";

    @Override
    public String getName() {
        return "todo";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config widgetConfig = getConfig(widgetTag.getParameters());

        return "<pre>" + HOURGLASS + " " + widgetConfig.getText() + "</pre>";
    }

    private Config getConfig(String parameters) {
        Config config = new Config();
        Map<String, String> map = NRGUtil.parseParametersLine(parameters);

        if (map.containsKey("text")) {
            config.setText(map.get("text"));
        }

        return config;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {

        private String text = "Not done yet...";

        public String getText() {
            return text;
        }

        public Config setText(String text) {
            this.text = text;
            return this;
        }
    }
}
