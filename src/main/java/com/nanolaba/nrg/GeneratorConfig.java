package com.nanolaba.nrg;

import com.nanolaba.logging.LOG;
import com.nanolaba.sugar.Code;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class GeneratorConfig {
    public static final String PROPERTY_LANGUAGES = "nrg.languages";
    public static final String PROPERTY_DEFAULT_LANGUAGE = "nrg.defaultLanguage";

    private List<String> languages = new ArrayList<>();
    private String defaultLanguage;

    public List<String> getLanguages() {
        return languages;
    }

    public GeneratorConfig setLanguages(List<String> languages) {
        this.languages = languages;
        return this;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public GeneratorConfig setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        return this;
    }

    protected void init() {
        if (defaultLanguage == null && !languages.isEmpty()) {
            defaultLanguage = languages.get(0);
        }

        if (defaultLanguage != null && !languages.contains(defaultLanguage)) {
            throw new IllegalStateException("The default language \"" + defaultLanguage + "\" is not defined within the \"" + PROPERTY_LANGUAGES + "\" property: " + languages);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generator configuration:");
            for (Field field : getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    LOG.debug("{}: {}", field.getName(), Code.run(() -> field.get(this)));
                }
            }
        }
    }
}
