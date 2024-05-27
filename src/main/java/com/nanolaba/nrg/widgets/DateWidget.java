package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DateWidget implements NRGWidget {

    @Override
    public String getName() {
        return "date";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config widgetConfig = getConfig(widgetTag.getParameters());

        return new SimpleDateFormat(widgetConfig.getPattern()).format(new Date());
    }

    private Config getConfig(String parameters) {
        Config config = new Config();
        Map<String, String> map = NRGUtil.parseParametersLine(parameters);

        if (map.containsKey("pattern")) {
            config.setPattern(map.get("pattern"));
        }

        return config;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {

        private String pattern = "dd.MM.yyyy HH:mm:ss";

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }
}
