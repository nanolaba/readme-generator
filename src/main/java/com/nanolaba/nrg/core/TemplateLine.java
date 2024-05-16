package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.*;

public class TemplateLine {

    public static final Pattern WIDGET_TAG_PATTERN = Pattern.compile(".*\\$\\{ *nrg\\.widget:(\\w*)(\\((.*)\\))? *}.*");
    public static final Properties EMPTY_PROPERTIES = new Properties(0);

    private final GeneratorConfig config;
    private final String line;

    public TemplateLine(GeneratorConfig config, String line) {
        this.config = config;
        this.line = line;
    }

    public String removeNrgDataFromText(String line) {
        for (String language : config.getLanguages()) {
            line = line.replaceAll("<!-- *" + Pattern.quote(language) + " *-->", "");
        }
        return line.replaceAll("<!-- *@.*-->", "");
    }

    public boolean isLineVisible(String language) {
        boolean hasLanguageMark = config.getLanguages().stream().anyMatch(s -> line.matches(".*<!-- *" + Pattern.quote(s) + " *-->.*"));
        boolean hasCurrentLanguageMark = line.matches(".*<!-- *" + Pattern.quote(language) + " *-->.*");
        boolean hasOnlyPropertyDefinition = line.matches("\\W*<!-- *@.*-->\\W*");

        return (!hasLanguageMark || hasCurrentLanguageMark) && !hasOnlyPropertyDefinition;
    }

    public Properties getProperties() {
        String[] strings = substringsBetween(line, "<!--", "-->");
        if (strings != null) {
            Properties properties = new Properties();
            for (String comment : strings) {
                String s = trimToEmpty(comment);
                if (s.contains("@")) {
                    s = substringAfter(s, "@");
                    if (s.contains("=")) {
                        String key = trimToEmpty(substringBefore(s, "="));
                        String value = trimToEmpty(substringAfter(s, "="));
                        NRGUtil.mergeProperty(key, value, properties);
                    } else {
                        String key = trimToEmpty(s);
                        NRGUtil.mergeProperty(key, "", properties);
                    }
                }
            }
            return properties;
        }
        return EMPTY_PROPERTIES;
    }

    public String getProperty(String property) {
        return getProperties().getProperty(property);
    }

    protected String renderWidgets(String line, String language) {
        WidgetTag widgetTag = getWidgetTag();
        if (widgetTag != null) {
            NRGWidget widget = config.getWidget(widgetTag.getName());
            if (widget != null) {
                String body = widget.getBody(widgetTag, config, language);
                return line.replaceAll("\\$\\{ *nrg\\.widget:" + Pattern.quote(widgetTag.getName()) + ".*}", body);
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

    protected String renderProperties(String line, String language) {
        for (Object key : config.getProperties().keySet()) {
            String keyString = key.toString();
            line = line.replaceAll("\\$\\{ *" + Pattern.quote(keyString) + ".*}", config.getProperties().getProperty(keyString));
        }

        return line;
    }

    public String generateLine(String language) {
        if (isLineVisible(language)) {
            String result = line;
            result = renderWidgets(result, language);
            result = renderProperties(result, language);
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
