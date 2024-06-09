package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

public class TemplateLine {

    public static final Pattern WIDGET_TAG_PATTERN = Pattern.compile("\\$\\{ *widget:(\\w*)(\\(([^}]*)\\))? *}");
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

    public Properties getProperties(String language) {
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
                        value = renderLanguageProperties(value, language);
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

    public String getProperty(String property, String language) {
        return getProperties(language).getProperty(property);
    }

    protected String renderWidgets(String line, String language) {
        StringBuilder result = new StringBuilder(line);
        int shift = 0;
        for (WidgetTag tag : getWidgetTags(line)) {
            NRGWidget widget = config.getWidget(tag.getName());
            if (widget != null) {
                try {
                    String body = widget.getBody(tag, config, language);
                    result.replace(shift + tag.getStart(), shift + tag.getEnd(), body);
                    shift += body.length() - (tag.getEnd() - tag.getStart());
                } catch (Throwable e) {
                    LOG.error(e, "Can't render widget '" + tag.getName() + "' at the line #" + lineNumber + " (" + this.line + ")");
                }
            } else {
                LOG.warn("Unknown widget name: {}", tag.getName());
            }
        }
        return result.toString();
    }

    protected List<WidgetTag> getWidgetTags(String line) {
        ArrayList<WidgetTag> res = new ArrayList<>();
        Matcher m = WIDGET_TAG_PATTERN.matcher(line);

        while (m.find()) {
            res.add(new WidgetTag(this, m.group(1), m.group(3), m.start(), m.end()));
        }

        return res;
    }

    protected String renderProperties(String line) {
        for (Object key : config.getProperties().keySet()) {
            String keyString = key.toString();
            line = line.replaceAll("\\$\\{ *" + Pattern.quote(keyString) + "[^}]*}", config.getProperties().getProperty(keyString));
        }

        return line;
    }

    protected String renderLanguageProperties(String line, String language) {
        String pairs = config.getLanguages().stream().map(s -> "( *" + Pattern.quote(s) + ": *['\"][^}]*['\"][, ]*)").collect(Collectors.joining("|"));
        String regex = "\\$\\{ *(" + pairs + ")}";

        StringBuilder result = new StringBuilder(line);
        int shift = 0;
        Matcher matcher = Pattern.compile(regex).matcher(line);
        while (matcher.find()) {
            String params = matcher.group(1);
            Map<String, String> vals = Arrays.stream(params.split(","))
                    .map(String::trim)
                    .map(s -> StringUtils.split(s, ":", 2))
                    .collect(Collectors.toMap(s -> s[0], s -> s.length > 1 ? NRGUtil.unwrapParameterValue(s[1]) : ""));

            String body = StringUtils.trimToEmpty(vals.get(language));
            result.replace(shift + matcher.start(), shift + matcher.end(), body);
            shift += body.length() - (matcher.end() - matcher.start());
        }
        return result.toString();
    }


    public String fillLineWithProperties(String language) {
        if (isLineVisible(language)) {
            String result = line;
            result = renderLanguageProperties(result, language);
            result = renderProperties(result);
            return result;
        } else {
            return null;
        }
    }

    public String generateLine(String language) {
        if (isLineVisible(language)) {
            String result = fillLineWithProperties(language);
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
