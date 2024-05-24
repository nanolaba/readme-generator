package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.TemplateLine;

public class WidgetTag {
    private final TemplateLine line;
    private final String name;
    private final String parameters;

    public WidgetTag(TemplateLine line, String name, String parameters) {
        this.line = line;
        this.name = name;
        this.parameters = parameters;
    }

    public TemplateLine getLine() {
        return line;
    }

    public String getName() {
        return name;
    }

    public String getParameters() {
        return parameters;
    }
}
