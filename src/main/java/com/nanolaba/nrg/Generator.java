package com.nanolaba.nrg;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.nanolaba.nrg.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.NRGConstants.PROPERTY_LANGUAGES;

public class Generator {

    private final String source;
    private final GeneratorConfig config = new GeneratorConfig();
    private final Map<String, GenerationResult> results = new HashMap<>();

    public Generator(String source) {
        this.source = source;
        fillConfig();
        generateContents();
    }

    private void fillConfig() {
        source.lines().forEachOrdered(line -> {
            String languages = getProperty(line, PROPERTY_LANGUAGES);
            if (languages != null && !languages.isEmpty()) {
                config.setLanguages(Arrays.stream(languages.split(",")).map(String::trim).toList());
            }
            String defaultLang = getProperty(line, PROPERTY_DEFAULT_LANGUAGE);
            if (defaultLang != null && !defaultLang.isEmpty()) {
                config.setDefaultLanguage(defaultLang);
            }
        });

        config.init();
    }

    private void generateContents() {
        for (String language : config.getLanguages()) {
            results.put(language, generateContent(language));
        }
    }

    private GenerationResult generateContent(String language) {
        GenerationResult result = new GenerationResult();
        result.setLanguage(language);

        source.lines()
                .filter(line -> isLineVisible(line, language))
                .map(this::clearNrgComments)
                .forEachOrdered(line -> result.getContent().append(line).append(System.lineSeparator()));

        return result;
    }

    protected String clearNrgComments(String line) {
        for (String language : config.getLanguages()) {
            line = line.replaceAll("<!--[ ]*" + language + "[ ]*-->", "");
        }
        line = line.replaceAll("<!--[ ]*nrg\\..*-->", "");
        return line;
    }

    protected boolean isLineVisible(String line, String language) {
        boolean hasLanguageMark = line.matches(".*<!--[ ]*[\\w]*[ ]*-->.*");
        boolean hasCurrentLanguageMark = line.matches(".*<!--[ ]*" + language + "[ ]*-->.*");
        boolean hasOnlyPropertyDefinition = line.matches("\\W*<!--[ ]*nrg\\..*-->\\W*");

        return (!hasLanguageMark || hasCurrentLanguageMark) && !hasOnlyPropertyDefinition;
    }

    private String getProperty(String line, String property) {
        for (String comment : StringUtils.substringsBetween(line, "<!--", "-->")) {
            if (StringUtils.trimToEmpty(comment).contains(property)) {
                String s = StringUtils.trimToEmpty(StringUtils.substringAfter(comment, property));
                if (s.contains("=")) {
                    return StringUtils.trimToEmpty(StringUtils.substringAfter(s, "="));
                }
            }
        }

        return null;
    }

    public GenerationResult getResult(String language) {
        return results.get(language);
    }

    public Collection<GenerationResult> getResult() {
        return results.values();
    }

    public GeneratorConfig getConfig() {
        return config;
    }
}
