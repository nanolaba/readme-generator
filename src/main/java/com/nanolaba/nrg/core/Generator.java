package com.nanolaba.nrg.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Generator {


    private final String source;
    private final GeneratorConfig config;
    private final Map<String, GenerationResult> results = new HashMap<>();

    public Generator(String source) {
        this.source = source;
        config = new GeneratorConfig(source);
        generateContents();
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
                .map(s -> new TemplateLine(config, s))
                .peek(line -> line.renderWidgets(language))
                .filter(line -> line.isLineVisible(language))
                .peek(TemplateLine::removeNrgDataFromText)
                .map(TemplateLine::getLine)
                .forEachOrdered(line -> result.getContent().append(line).append(System.lineSeparator()));

        return result;
    }

    public GenerationResult getResult(String language) {
        return results.get(language);
    }

    public Collection<GenerationResult> getResults() {
        return results.values();
    }

    public GeneratorConfig getConfig() {
        return config;
    }
}
