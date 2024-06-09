package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.TemplateLine;

public class WidgetTag {

    private final TemplateLine line;
    private final String name;
    private final String parameters;
    private final int start;
    private final int end;

    public WidgetTag(TemplateLine line, String name, String parameters, int start, int end) {
        this.line = line;
        this.name = name;
        this.parameters = parameters;
        this.start = start;
        this.end = end;
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

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
