package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.Map;

public class AssetWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "asset";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> p = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String name = p.get("name");
        if (name == null || name.isEmpty()) {
            LOG.error("asset widget: missing required 'name' parameter");
            return "";
        }

        String langScoped = config.getProperties().getProperty("asset." + name + "." + language);
        if (langScoped != null) {
            return langScoped;
        }

        String fallback = config.getProperties().getProperty("asset." + name);
        if (fallback != null) {
            return fallback;
        }

        LOG.error("asset widget: no value for asset '{}' (define <!--@asset.{}=...--> or <!--@asset.{}.{}=...-->)",
                name, name, name, language);
        return "";
    }
}
