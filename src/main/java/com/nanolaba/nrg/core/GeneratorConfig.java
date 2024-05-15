package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.LanguagesWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.sugar.Code;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;

public class GeneratorConfig {

    private final List<NRGWidget> widgets = new ArrayList<>();

    private List<String> languages = new ArrayList<>();
    private String defaultLanguage;

    public GeneratorConfig(String templateText) {
        templateText.lines()
                .map(s -> new TemplateLine(this, s))
                .forEachOrdered(line -> {
                    String languages = line.getProperty(PROPERTY_LANGUAGES);
                    if (languages != null && !languages.isEmpty()) {
                        GeneratorConfig.this.languages = Arrays.stream(languages.split(",")).map(String::trim).toList();
                    }
                    String defaultLang = line.getProperty(PROPERTY_DEFAULT_LANGUAGE);
                    if (defaultLang != null && !defaultLang.isEmpty()) {
                        defaultLanguage = defaultLang;
                    }
                });

        if (defaultLanguage == null && !languages.isEmpty()) {
            defaultLanguage = languages.get(0);
        }

        if (defaultLanguage != null && !languages.contains(defaultLanguage)) {
            throw new IllegalStateException("The default language \"" + defaultLanguage +
                                            "\" is not defined within the \"" + PROPERTY_LANGUAGES +
                                            "\" property: " + languages);
        }

        initWidgets();
        printConfiguration();
    }

    protected void initWidgets() {
        widgets.add(new LanguagesWidget());
    }

    private void printConfiguration() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generator configuration:");
            for (Field field : getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    LOG.debug("{}: {}", field.getName(), Code.run(() -> field.get(this)));
                }
            }
        }
    }

    public NRGWidget getWidget(String widgetName) {
        return getWidgets().stream()
                .filter(e -> e.getName().equals(widgetName))
                .findFirst().orElse(null);
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public List<NRGWidget> getWidgets() {
        return widgets;
    }
}
