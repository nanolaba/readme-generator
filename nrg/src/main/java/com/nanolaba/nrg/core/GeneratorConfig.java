package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.*;
import com.nanolaba.sugar.Code;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nanolaba.nrg.core.NRGConstants.*;

public class GeneratorConfig {

    private final List<NRGWidget> widgets = new ArrayList<>();

    private final File sourceFile;
    private final String sourceFileBody;
    private final Properties properties = new Properties();

    private File rootSourceFile;
    private List<String> languages = Collections.singletonList("en");
    private String defaultLanguage;
    private String widgetsProperty;
    private boolean rootGenerator = true;
    private boolean execAllowed = false;
    private Function<String, String> envProvider = System::getenv;
    private final Set<String> warnedMissingEnvVars = new HashSet<>();


    public GeneratorConfig(File sourceFile, String templateText, List<NRGWidget> widgets) {
        this(sourceFile, templateText, widgets, null);
    }

    public GeneratorConfig(File sourceFile, String templateText, List<NRGWidget> widgets,
                           Function<String, String> envProvider) {
        this.sourceFile = sourceFile;
        this.sourceFileBody = IgnoreBlockStripper.strip(templateText);
        this.rootSourceFile = sourceFile;
        if (envProvider != null) {
            this.envProvider = envProvider;
        }

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

        initDefaultWidgets(this.widgets);
        this.widgets.addAll(NRGUtil.loadWidgets(widgetsProperty, null));
        if (widgets != null) {
            this.widgets.addAll(widgets);
        }
        printConfiguration();
    }

    public Stream<TemplateLine> getSourceLinesStream() {
        AtomicInteger counter = new AtomicInteger(0);
        return new BufferedReader(new StringReader(sourceFileBody))
                .lines()
                .map(s -> new TemplateLine(this, s, counter.getAndIncrement()));

    }

    private void readLanguagesPropertiesFromLine(TemplateLine line) {
        Map<String, String> raw = NRGUtil.extractRawPropertyMarkers(line.getLine());

        String lang = raw.get(PROPERTY_LANGUAGES);
        if (lang != null && !lang.isEmpty()) {
            languages = Arrays.stream(lang.split(",")).map(String::trim).collect(Collectors.toList());
        }
        String defaultLang = raw.get(PROPERTY_DEFAULT_LANGUAGE);
        if (defaultLang != null && !defaultLang.isEmpty()) {
            defaultLanguage = defaultLang;
        }
        String widgetsProp = raw.get(PROPERTY_WIDGETS);
        if (widgetsProp != null && !widgetsProp.isEmpty()) {
            widgetsProperty = widgetsProp;
        }
    }

    private void readPropertiesFromLine(TemplateLine line, String language) {
        line.readProperties(language).forEach((key, value) -> NRGUtil.mergeProperty(key, value, properties));
    }

    protected void initDefaultWidgets(List<NRGWidget> widgets) {
        widgets.add(new ImportWidget());
        widgets.add(new LanguagesWidget());
        widgets.add(new TableOfContentsWidget());
        widgets.add(new DateWidget());
        widgets.add(new TodoWidget());
        widgets.add(new AlertWidget());
        widgets.add(new BadgeWidget());
        widgets.add(new MathWidget());
        widgets.add(new ExecWidget());
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
        NRGWidget match = null;
        for (NRGWidget widget : getWidgets()) {
            if (widget.getName().equals(widgetName)) {
                match = widget;
            }
        }
        return match;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public File getRootSourceFile() {
        return rootSourceFile;
    }

    public void setRootSourceFile(File rootSourceFile) {
        this.rootSourceFile = rootSourceFile;
    }

    public String getSourceFileBody() {
        return sourceFileBody;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public List<NRGWidget> getWidgets() {
        return widgets;
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean isRootGenerator() {
        return rootGenerator;
    }

    public void setRootGenerator(boolean rootGenerator) {
        this.rootGenerator = rootGenerator;
    }

    public boolean isExecAllowed() {
        return execAllowed;
    }

    public void setExecAllowed(boolean execAllowed) {
        this.execAllowed = execAllowed;
    }

    public Function<String, String> getEnvProvider() {
        return envProvider;
    }

    public void setEnvProvider(Function<String, String> envProvider) {
        this.envProvider = envProvider == null ? System::getenv : envProvider;
    }

    public Set<String> getWarnedMissingEnvVars() {
        return warnedMissingEnvVars;
    }
}
