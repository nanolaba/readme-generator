package com.nanolaba.nrg;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.LanguagesWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nanolaba.nrg.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.NRGConstants.PROPERTY_LANGUAGES;
import static org.apache.commons.lang3.StringUtils.*;

public class Generator {

    public static final Pattern WIDGET_TAG_PATTERN = Pattern.compile(".*<!-- *nrg\\.widget\\.(\\w*)(\\((.*)\\))? *-->.*");

    private final String source;
    private final GeneratorConfig config = new GeneratorConfig();
    private final Map<String, GenerationResult> results = new HashMap<>();
    private final List<NRGWidget> widgets = new ArrayList<>();

    public Generator(String source) {
        this.source = source;

        initWidgets();
        initConfig();
        generateContents();
    }

    private void initWidgets() {
        widgets.add(new LanguagesWidget());
    }

    private void initConfig() {
        source.lines().forEachOrdered(line -> {
            String languages = getProperty(line, PROPERTY_LANGUAGES);
            if (languages != null && !languages.isEmpty()) {
                config.setLanguages(Arrays.stream(languages.split(",")).map(String::trim).toList());
            }
            String defaultLang = getProperty(line, PROPERTY_DEFAULT_LANGUAGE);
            if (defaultLang != null && !defaultLang.isEmpty()) {
                config.setDefaultLanguage(defaultLang);
            }
        });

        config.init();
    }

    private void generateContents() {
        for (String language : config.getLanguages()) {
            results.put(language, generateContent(language));
        }
    }

    private GenerationResult generateContent(String language) {
        GenerationResult result = new GenerationResult();
        result.setLanguage(language);

        source.lines()
                .map(line -> renderWidgets(line, language))
                .filter(line -> isLineVisible(line, language))
                .map(this::removeNrgDataFromText)
                .forEachOrdered(line -> result.getContent().append(line).append(System.lineSeparator()));

        return result;
    }

    protected String renderWidgets(String line, String language) {
        WidgetTag widgetTag = getWidgetTag(line);
        if (widgetTag != null) {
            NRGWidget widget = getWidget(widgetTag.getName());
            if (widget != null) {
                String body = widget.getBody(widgetTag, config, language);
                line = line.replaceAll("<!-- *nrg\\.widget\\." + widgetTag.getName() + ".*-->", body);
            } else {
                LOG.warn("Unknown widget name: {}", widgetTag.getName());
            }
        }
        return line;
    }

    protected NRGWidget getWidget(String widgetName) {
        return widgets.stream()
                .filter(e -> e.getName().equals(widgetName))
                .findFirst().orElse(null);
    }

    protected WidgetTag getWidgetTag(String line) {
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

    protected String removeNrgDataFromText(String line) {
        for (String language : config.getLanguages()) {
            line = line.replaceAll("<!-- *" + language + " *-->", "");
        }
        line = line.replaceAll("<!-- *nrg\\..*-->", "");
        return line;
    }

    protected boolean isLineVisible(String line, String language) {
        boolean hasLanguageMark = line.matches(".*<!-- *\\w* *-->.*");
        boolean hasCurrentLanguageMark = line.matches(".*<!-- *" + language + " *-->.*");
        boolean hasOnlyPropertyDefinition = line.matches("\\W*<!-- *nrg\\..*-->\\W*");

        return (!hasLanguageMark || hasCurrentLanguageMark) && !hasOnlyPropertyDefinition;
    }

    protected String getProperty(String line, String property) {
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

    public GenerationResult getResult(String language) {
        return results.get(language);
    }

    public Collection<GenerationResult> getResults() {
        return results.values();
    }

    public GeneratorConfig getConfig() {
        return config;
    }

    public List<NRGWidget> getWidgets() {
        return widgets;
    }
}
