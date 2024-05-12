package com.nanolaba.nrg;

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
}
