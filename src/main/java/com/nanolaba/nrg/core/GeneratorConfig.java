package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.LanguagesWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.TableOfContentsWidget;
import com.nanolaba.sugar.Code;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;

public class GeneratorConfig {

    private final List<NRGWidget> widgets = new ArrayList<>();

    private final File sourceFile;
    private final String sourceFileBody;
    private List<String> languages = new ArrayList<>();
    private String defaultLanguage;
    private final Properties properties = new Properties();


    public GeneratorConfig(File sourceFile, String templateText) {
        this.sourceFile = sourceFile;
        this.sourceFileBody = templateText;

        getSourceLinesStream().forEach(this::readPropertiesFromLine);

        if (defaultLanguage == null && !languages.isEmpty()) {
            defaultLanguage = languages.get(0);
        }

        if (defaultLanguage != null && !languages.contains(defaultLanguage)) {
            throw new IllegalStateException("The default language \"" + defaultLanguage +
                                            "\" is not defined within the \"" + PROPERTY_LANGUAGES +
                                            "\" property: " + languages);
        }

        initWidgets();
        printConfiguration();
    }

    public Stream<TemplateLine> getSourceLinesStream() {
        return sourceFileBody.lines().map(s -> new TemplateLine(this, s));
    }

    private void readPropertiesFromLine(TemplateLine line) {

        line.getProperties().forEach((key, value) -> NRGUtil.mergeProperty(key, value, properties));

        String lang = line.getProperty(PROPERTY_LANGUAGES);
        if (lang != null && !lang.isEmpty()) {
            languages = Arrays.stream(lang.split(",")).map(String::trim).toList();
        }
        String defaultLang = line.getProperty(PROPERTY_DEFAULT_LANGUAGE);
        if (defaultLang != null && !defaultLang.isEmpty()) {
            defaultLanguage = defaultLang;
        }
    }

    protected void initWidgets() {
        widgets.add(new LanguagesWidget());
        widgets.add(new TableOfContentsWidget());
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
