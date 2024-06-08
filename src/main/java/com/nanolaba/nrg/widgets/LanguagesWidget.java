package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.NRG;
import com.nanolaba.nrg.core.GeneratorConfig;

public class LanguagesWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "languages";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        StringBuilder builder = new StringBuilder();
        for (String lang : config.getLanguages()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            if (lang.equals(language)) {
                builder.append("**").append(lang).append("**");
            } else {
                builder.append("[").append(lang).append("](").append(NRG.getReadmeFile(lang, config).getName()).append(")");
            }
        }
        return "[ " + builder + " ]";
    }
}
