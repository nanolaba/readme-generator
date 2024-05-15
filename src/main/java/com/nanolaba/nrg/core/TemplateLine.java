package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.*;

public class TemplateLine {

    public static final Pattern WIDGET_TAG_PATTERN = Pattern.compile(".*\\$\\{ *nrg\\.widget:(\\w*)(\\((.*)\\))? *}.*");

    private final GeneratorConfig config;
    private final String line;

    public TemplateLine(GeneratorConfig config, String line) {
        this.config = config;
        this.line = line;
    }

    public String removeNrgDataFromText(String line) {
        for (String language : config.getLanguages()) {
            line = line.replaceAll("<!-- *" + language + " *-->", "");
        }
        return line.replaceAll("<!-- *nrg\\..*-->", "");
    }

    public boolean isLineVisible(String language) {
        boolean hasLanguageMark = line.matches(".*<!-- *\\w* *-->.*");
        boolean hasCurrentLanguageMark = line.matches(".*<!-- *" + language + " *-->.*");
        boolean hasOnlyPropertyDefinition = line.matches("\\W*<!-- *nrg\\..*-->\\W*");

        return (!hasLanguageMark || hasCurrentLanguageMark) && !hasOnlyPropertyDefinition;
    }

    protected String getProperty(String property) {
        String[] strings = substringsBetween(line, "<!--", "-->");
        if (strings != null) {
            for (String comment : strings) {
                if (trimToEmpty(comment).contains(property)) {
                    String s = trimToEmpty(substringAfter(comment, property));
                    if (s.contains("=")) {
                        return trimToEmpty(substringAfter(s, "="));
                    }
                }
            }
        }

        return null;
    }

    protected String renderWidgets(String language) {
        WidgetTag widgetTag = getWidgetTag();
        if (widgetTag != null) {
            NRGWidget widget = config.getWidget(widgetTag.getName());
            if (widget != null) {
                String body = widget.getBody(widgetTag, config, language);
                return line.replaceAll("\\$\\{ *nrg\\.widget:" + widgetTag.getName() + ".*}", body);
            } else {
                LOG.warn("Unknown widget name: {}", widgetTag.getName());
            }
        }
        return line;
    }

    protected WidgetTag getWidgetTag() {
        Matcher m = WIDGET_TAG_PATTERN.matcher(line);

        if (m.find()) {
            WidgetTag result = new WidgetTag();
            result.setName(m.group(1));
            result.setParameters(m.group(3));
            return result;
        } else {
            return null;
        }
    }

    public String generateLine(String language) {
        if (isLineVisible(language)) {
            String result = renderWidgets(language);
            result = removeNrgDataFromText(result);
            return result;
        } else {
            return null;
        }
    }

    public GeneratorConfig getConfig() {
        return config;
    }
}
