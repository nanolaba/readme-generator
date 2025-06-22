package com.nanolaba.nrg.core;

public class GenerationResult {

    private final String language;
    private final StringBuilder content = new StringBuilder();

    public GenerationResult(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public StringBuilder getContent() {
        return content;
    }

}
