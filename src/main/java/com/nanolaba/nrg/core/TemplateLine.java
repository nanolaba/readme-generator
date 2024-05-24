package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

public class TemplateLine {

    public static final Pattern WIDGET_TAG_PATTERN = Pattern.compile(".*\\$\\{ *nrg\\.widget:(\\w*)(\\((.*)\\))? *}.*");
    public static final Properties EMPTY_PROPERTIES = new Properties(0);

    private final GeneratorConfig config;
    private final String line;
    private final int lineNumber;

    public TemplateLine(GeneratorConfig config, String line, int lineNumber) {
        this.config = config;
        this.line = line;
        this.lineNumber = lineNumber;
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
        WidgetTag widgetTag = getWidgetTag(line);
        if (widgetTag != null) {
            NRGWidget widget = config.getWidget(widgetTag.getName());
            if (widget != null) {
                String body = widget.getBody(widgetTag, config, language);
                return line.replaceAll("\\$\\{ *nrg\\.widget:" + Pattern.quote(widgetTag.getName()) + "[^{]*}", body);
            } else {
                LOG.warn("Unknown widget name: {}", widgetTag.getName());
            }
        }
        return line;
    }

    protected WidgetTag getWidgetTag(String line) {
        Matcher m = WIDGET_TAG_PATTERN.matcher(line);

        return m.find() ? new WidgetTag(this, m.group(1), m.group(3)) : null;
    }

    protected String renderProperties(String line) {
        for (Object key : config.getProperties().keySet()) {
            String keyString = key.toString();
            line = line.replaceAll("\\$\\{ *" + Pattern.quote(keyString) + ".*}", config.getProperties().getProperty(keyString));
        }

        return line;
    }

    protected String renderLanguageProperties(String line, String language) {
        String pairs = config.getLanguages().stream().map(s -> "( *" + Pattern.quote(s) + ": *['\"].*['\"][, ]*)").collect(Collectors.joining("|"));
        String regex = "\\$\\{ *(" + pairs + ")}";

        Matcher matcher = Pattern.compile(regex).matcher(line);
        if (matcher.find()) {
            String params = matcher.group(1);
            Map<String, String> vals = Arrays.stream(params.split(","))
                    .map(String::trim)
                    .filter(s -> s.contains(":"))
                    .map(s -> s.split(":"))
                    .collect(Collectors.toMap(s -> s[0], s -> StringUtils.unwrap(StringUtils.unwrap(s[1], "'"), "\"")));
            line = line.replaceAll(regex, StringUtils.trimToEmpty(vals.get(language)));
        } else {
            line = line.replaceAll(regex, "");
        }

        return line;
    }

    public String generateLine(String language) {
        if (isLineVisible(language)) {
            String result = line;
            result = renderLanguageProperties(result, language);
            result = renderProperties(result);
            result = renderWidgets(result, language);
            result = removeNrgDataFromText(result);
            return result;
        } else {
            return null;
        }
    }

    public GeneratorConfig getConfig() {
        return config;
    }

    public String getLine() {
        return line;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
