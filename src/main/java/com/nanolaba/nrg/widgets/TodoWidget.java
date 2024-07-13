package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.Map;

public class TodoWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "todo";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config widgetConfig = getConfig(widgetTag.getParameters());

        return "<div style=\"" + widgetConfig.getStyle() + "\">" + widgetConfig.getText() + "</div>";
    }

    private Config getConfig(String parameters) {
        Config config = new Config();
        Map<String, String> map = NRGUtil.parseParametersLine(parameters);

        if (map.containsKey("text")) {
            config.setText(map.get("text"));
        }
        if (map.containsKey("style")) {
            config.setStyle(map.get("style"));
        }

        return config;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {

        private String text = "Not done yet...";
        private String style = "color:red; padding: 1em; border: 2px solid red;";

        public String getText() {
            return text;
        }

        public Config setText(String text) {
            this.text = text;
            return this;
        }


        public String getStyle() {
            return style;
        }

        public Config setStyle(String style) {
            this.style = style;
            return this;
        }
    }
}
