package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.*;
import com.nanolaba.sugar.Code;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;

public class GeneratorConfig {

    private final List<NRGWidget> widgets = new ArrayList<>();

    private final File sourceFile;
    private final String sourceFileBody;
    private List<String> languages = List.of("en");
    private String defaultLanguage;
    private final Properties properties = new Properties();


    public GeneratorConfig(File sourceFile, String templateText) {
        this.sourceFile = sourceFile;
        this.sourceFileBody = templateText;

        getSourceLinesStream().forEach(this::readLanguagesPropertiesFromLine);

        if (defaultLanguage == null && !languages.isEmpty()) {
            defaultLanguage = languages.get(0);
        }

        if (defaultLanguage != null && !languages.contains(defaultLanguage)) {
            throw new IllegalStateException("The default language \"" + defaultLanguage +
                                            "\" is not defined within the \"" + PROPERTY_LANGUAGES +
                                            "\" property: " + languages);
        }

        getSourceLinesStream().forEach(line -> readPropertiesFromLine(line, defaultLanguage));

        initWidgets(widgets);
        printConfiguration();
    }

    public Stream<TemplateLine> getSourceLinesStream() {
        AtomicInteger counter = new AtomicInteger(0);
        return sourceFileBody.lines().map(s -> new TemplateLine(this, s, counter.getAndIncrement()));
    }

    private void readLanguagesPropertiesFromLine(TemplateLine line) {

        String lang = line.getProperty(PROPERTY_LANGUAGES, null);
        if (lang != null && !lang.isEmpty()) {
            languages = Arrays.stream(lang.split(",")).map(String::trim).toList();
        }
        String defaultLang = line.getProperty(PROPERTY_DEFAULT_LANGUAGE, null);
        if (defaultLang != null && !defaultLang.isEmpty()) {
            defaultLanguage = defaultLang;
        }
    }

    private void readPropertiesFromLine(TemplateLine line, String language) {
        line.readProperties(language).forEach((key, value) -> NRGUtil.mergeProperty(key, value, properties));
    }

    protected void initWidgets(List<NRGWidget> widgets) {
        widgets.add(new LanguagesWidget());
        widgets.add(new TableOfContentsWidget());
        widgets.add(new DateWidget());
        widgets.add(new TodoWidget());
    }

    private void printConfiguration() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generator configuration:");
            for (Field field : getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    if (!field.getName().equals("sourceFileBody")) {
                        LOG.debug("{}: {}", field.getName(), Code.run(() -> field.get(this)));
                    }
                }
            }
        }
    }

    public NRGWidget getWidget(String widgetName) {
        return getWidgets().stream()
                .filter(e -> e.getName().equals(widgetName))
                .findFirst().orElse(null);
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getSourceFileBody() {
        return sourceFileBody;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public List<NRGWidget> getWidgets() {
        return widgets;
    }

    public Properties getProperties() {
        return properties;
    }
}
