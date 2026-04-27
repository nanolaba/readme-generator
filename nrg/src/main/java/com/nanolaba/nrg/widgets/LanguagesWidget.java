package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.OutputFileNameResolver;

import java.io.File;
import java.nio.file.Path;

/**
 * {@code ${widget:languages}} — renders the language-switcher line {@code [ en | **ru** | de ]}.
 *
 * <p>The current language is bolded; every other declared language becomes a relative
 * Markdown link to its sibling output file (resolved via {@link OutputFileNameResolver}).
 * Path separators are normalised to forward slashes so links work on any platform.
 */
public class LanguagesWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "languages";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        StringBuilder builder = new StringBuilder();
        File currentFile = OutputFileNameResolver.resolve(
                config.getRootSourceFile(), config.getDefaultLanguage(), language, config.getProperties());
        Path currentDir = currentFile.getAbsoluteFile().toPath().getParent();

        for (String lang : config.getLanguages()) {
            if (builder.length() != 0) {
                builder.append(" | ");
            }
            if (lang.equals(language)) {
                builder.append("**").append(lang).append("**");
            } else {
                File targetFile = OutputFileNameResolver.resolve(
                        config.getRootSourceFile(), config.getDefaultLanguage(), lang, config.getProperties());
                String href;
                if (currentDir == null) {
                    href = targetFile.getName();
                } else {
                    Path target = targetFile.getAbsoluteFile().toPath();
                    Path rel = currentDir.relativize(target);
                    href = rel.toString().replace('\\', '/');
                }
                builder.append("[").append(lang).append("](").append(href).append(")");
            }
        }
        return "[ " + builder + " ]";
    }
}
