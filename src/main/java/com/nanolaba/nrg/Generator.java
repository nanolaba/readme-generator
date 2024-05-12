package com.nanolaba.nrg;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Generator {

    private final String source;
    private final GeneratorConfig config = new GeneratorConfig();
    private final Map<String, GenerationResult> results = new HashMap<>();

    public Generator(String source) {
        this.source = source;
        fillConfig();
    }

    private void fillConfig() {
        try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
            String line = reader.readLine();
            while (line != null) {
                String languages = getProperty(line, GeneratorConfig.PROPERTY_LANGUAGES);
                if (languages != null && !languages.isEmpty()) {
                    config.setLanguages(Arrays.stream(languages.split(",")).map(String::trim).toList());
                }
                line = reader.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

    public Collection<GenerationResult> getResult() {
        return results.values();
    }

    public GeneratorConfig getConfig() {
        return config;
    }
}
