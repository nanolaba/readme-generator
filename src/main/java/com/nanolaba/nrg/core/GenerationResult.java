package com.nanolaba.nrg.core;

public class GenerationResult {

    private String language;
    private StringBuilder content = new StringBuilder();

    public String getLanguage() {
        return language;
    }

    public GenerationResult setLanguage(String language) {
        this.language = language;
        return this;
    }

    public StringBuilder getContent() {
        return content;
    }

    public GenerationResult setContent(StringBuilder content) {
        this.content = content;
        return this;
    }
}
