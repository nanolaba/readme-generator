package com.nanolaba.nrg.widgets;

public class WidgetTag {
    private String name;
    private String parameters;

    public String getName() {
        return name;
    }

    public WidgetTag setName(String name) {
        this.name = name;
        return this;
    }

    public String getParameters() {
        return parameters;
    }

    public WidgetTag setParameters(String parameters) {
        this.parameters = parameters;
        return this;
    }
}
