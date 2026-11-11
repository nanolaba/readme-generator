package com.nanolaba.nrg.core;

import java.util.List;

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
    private List<SourceOrigin> sourceMap;

    public GenerationResult(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public StringBuilder getContent() {
        return content;
    }

    /**
     * One origin per line of {@link #getContent()} (the empty string after the final line
     * separator is not a line), or {@code null} unless the generator was asked to collect it
     * with {@link GeneratorConfig#setCollectSourceMap(boolean)}.
     */
    public List<SourceOrigin> getSourceMap() {
        return sourceMap;
    }

    public void setSourceMap(List<SourceOrigin> sourceMap) {
        this.sourceMap = sourceMap;
    }

}
