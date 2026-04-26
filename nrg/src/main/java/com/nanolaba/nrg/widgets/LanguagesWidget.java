package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;

public class LanguagesWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "languages";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        StringBuilder builder = new StringBuilder();
        java.io.File currentFile = com.nanolaba.nrg.core.OutputFileNameResolver.resolve(
                config.getRootSourceFile(), config.getDefaultLanguage(), language, config.getProperties());
        java.nio.file.Path currentDir = currentFile.getAbsoluteFile().toPath().getParent();

        for (String lang : config.getLanguages()) {
            if (builder.length() != 0) {
                builder.append(" | ");
            }
            if (lang.equals(language)) {
                builder.append("**").append(lang).append("**");
            } else {
                java.io.File targetFile = com.nanolaba.nrg.core.OutputFileNameResolver.resolve(
                        config.getRootSourceFile(), config.getDefaultLanguage(), lang, config.getProperties());
                String href;
                if (currentDir == null) {
                    href = targetFile.getName();
                } else {
                    java.nio.file.Path target = targetFile.getAbsoluteFile().toPath();
                    java.nio.file.Path rel = currentDir.relativize(target);
                    href = rel.toString().replace('\\', '/');
                }
                builder.append("[").append(lang).append("](").append(href).append(")");
            }
        }
        return "[ " + builder + " ]";
    }
}
