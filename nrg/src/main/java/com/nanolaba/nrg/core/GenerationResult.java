package com.nanolaba.nrg.core;

/**
 * Mutable accumulator for one language variant of a generated source template.
 *
 * <p>{@link Generator} appends rendered lines to {@link #getContent()} as the per-line
 * pipeline progresses. The {@code StringBuilder} is exposed directly so callers can write
 * into it without intermediate copies.
 */
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
