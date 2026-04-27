package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.freeze.DiskFreezeIndex;
import com.nanolaba.nrg.widgets.*;
import com.nanolaba.sugar.Code;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nanolaba.nrg.core.NRGConstants.*;

/**
 * Holds the parsed configuration for one source template: declared languages,
 * properties, widgets, and lazily-resolved external descriptors (POM, npm, Gradle).
 *
 * <p>Construction parses the source body in two passes:
 * <ol>
 *   <li>First pass extracts the bootstrap properties {@code nrg.languages} and
 *       {@code nrg.defaultLanguage} so subsequent rendering knows which language variants exist.</li>
 *   <li>Second pass collects every other {@code <!--@key=value-->} marker into {@link #properties}.</li>
 * </ol>
 *
 * <p>Default widgets are always registered; {@code nrg.widgets} (if defined) and any
 * caller-supplied widgets are added on top. {@link #rootGenerator} distinguishes the
 * top-level generator from sub-generators created by {@code ImportWidget} — only the root
 * strips NRG metadata and unescapes characters during the final pass.
 */
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
    private PomReader pomReader;
    private boolean pomReaderInitialised = false;
    private final Set<String> warnedMissingPomPaths = new HashSet<>();

    private NpmReader npmReader;
    private boolean npmReaderInitialised = false;
    private final Set<String> warnedMissingNpmPaths = new HashSet<>();

    private GradleReader gradleReader;
    private boolean gradleReaderInitialised = false;
    private final Set<String> warnedMissingGradlePaths = new HashSet<>();

    private final Map<File, DiskFreezeIndex> diskFreezeIndexCache = new HashMap<>();
    private final Set<String> warnedMissingFreezeIds = new HashSet<>();


    public GeneratorConfig(File sourceFile, String templateText, List<NRGWidget> widgets) {
        this(sourceFile, templateText, widgets, null, null, null, null);
    }

    public GeneratorConfig(File sourceFile, String templateText, List<NRGWidget> widgets,
                           Function<String, String> envProvider) {
        this(sourceFile, templateText, widgets, envProvider, null, null, null);
    }

    public GeneratorConfig(File sourceFile, String templateText, List<NRGWidget> widgets,
                           Function<String, String> envProvider, PomReader pomReader) {
        this(sourceFile, templateText, widgets, envProvider, pomReader, null, null);
    }

    public GeneratorConfig(File sourceFile, String templateText, List<NRGWidget> widgets,
                           Function<String, String> envProvider, PomReader pomReader,
                           NpmReader npmReader, GradleReader gradleReader) {
        this.sourceFile = sourceFile;
        this.sourceFileBody = IgnoreBlockStripper.strip(templateText);
        this.rootSourceFile = sourceFile;
        if (envProvider != null) {
            this.envProvider = envProvider;
        }
        if (pomReader != null) {
            this.pomReader = pomReader;
            this.pomReaderInitialised = true;
        }
        if (npmReader != null) {
            this.npmReader = npmReader;
            this.npmReaderInitialised = true;
        }
        if (gradleReader != null) {
            this.gradleReader = gradleReader;
            this.gradleReaderInitialised = true;
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
        widgets.add(new FileTreeWidget());
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

    public boolean isAllowRemoteImports() {
        return Boolean.parseBoolean(properties.getProperty(PROPERTY_ALLOW_REMOTE_IMPORTS, "false"));
    }

    public Path getCacheDir() {
        String configured = properties.getProperty(PROPERTY_CACHE_DIR);
        if (configured == null || configured.isEmpty()) {
            return Paths.get(System.getProperty("user.home"), ".nrg", "cache");
        }
        return Paths.get(configured);
    }

    public boolean isRequireSha256ForRemote() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY_REQUIRE_SHA256_FOR_REMOTE, "false"));
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

    public PomReader getPomReader() {
        if (!pomReaderInitialised) {
            pomReaderInitialised = true;
            if (pomReader == null) {
                pomReader = new DomPomReader(resolvePomFile());
            }
        }
        return pomReader;
    }

    public void setPomReader(PomReader pomReader) {
        this.pomReader = pomReader;
        this.pomReaderInitialised = true;
    }

    public Set<String> getWarnedMissingPomPaths() {
        return warnedMissingPomPaths;
    }

    private File resolvePomFile() {
        String configured = getProperties().getProperty(NRGConstants.PROPERTY_POM_PATH);
        File base = sourceFile == null ? new File(".").getAbsoluteFile() : sourceFile.getAbsoluteFile().getParentFile();
        if (configured == null || configured.isEmpty()) {
            return new File(base, "pom.xml");
        }
        File requested = new File(configured);
        return requested.isAbsolute() ? requested : new File(base, configured);
    }

    public NpmReader getNpmReader() {
        if (!npmReaderInitialised) {
            npmReaderInitialised = true;
            if (npmReader == null) {
                npmReader = new JsonNpmReader(resolveNpmFile());
            }
        }
        return npmReader;
    }

    public void setNpmReader(NpmReader npmReader) {
        this.npmReader = npmReader;
        this.npmReaderInitialised = true;
    }

    public Set<String> getWarnedMissingNpmPaths() {
        return warnedMissingNpmPaths;
    }

    private File resolveNpmFile() {
        String configured = getProperties().getProperty(NRGConstants.PROPERTY_NPM_PATH);
        File base = sourceFile == null
                ? new File(".").getAbsoluteFile()
                : sourceFile.getAbsoluteFile().getParentFile();
        if (configured == null || configured.isEmpty()) {
            return new File(base, "package.json");
        }
        File requested = new File(configured);
        return requested.isAbsolute() ? requested : new File(base, configured);
    }

    public GradleReader getGradleReader() {
        if (!gradleReaderInitialised) {
            gradleReaderInitialised = true;
            if (gradleReader == null) {
                File[] files = resolveGradleLocation();
                gradleReader = new RegexGradleReader(files[0], files[1]);
            }
        }
        return gradleReader;
    }

    public void setGradleReader(GradleReader gradleReader) {
        this.gradleReader = gradleReader;
        this.gradleReaderInitialised = true;
    }

    public Set<String> getWarnedMissingGradlePaths() {
        return warnedMissingGradlePaths;
    }

    /**
     * Resolves the configured gradle location into a [propertiesFile, buildScriptFile] pair.
     * The configured value may be a directory (resolves both inside it) or an explicit file
     * (used as build script; sibling gradle.properties is included automatically).
     */
    private File[] resolveGradleLocation() {
        String configured = getProperties().getProperty(NRGConstants.PROPERTY_GRADLE_PATH);
        File base = sourceFile == null
                ? new File(".").getAbsoluteFile()
                : sourceFile.getAbsoluteFile().getParentFile();
        File anchor;
        if (configured == null || configured.isEmpty()) {
            anchor = base;
        } else {
            File requested = new File(configured);
            anchor = requested.isAbsolute() ? requested : new File(base, configured);
        }
        if (anchor.isDirectory()) {
            File props = new File(anchor, "gradle.properties");
            File groovy = new File(anchor, "build.gradle");
            File kotlin = new File(anchor, "build.gradle.kts");
            File buildScript = groovy.isFile() ? groovy : (kotlin.isFile() ? kotlin : null);
            return new File[] { props.isFile() ? props : null, buildScript };
        }
        File parent = anchor.getParentFile();
        File props = parent == null ? null : new File(parent, "gradle.properties");
        return new File[] { props != null && props.isFile() ? props : null, anchor.isFile() ? anchor : null };
    }

    /**
     * Returns the {@link DiskFreezeIndex} for the given on-disk file, caching the result so
     * subsequent lookups for the same file (e.g. when regenerating multiple language variants)
     * reuse the parsed index instead of re-reading and re-parsing the file.
     *
     * <p>A {@code null} file is treated as "no on-disk source": the call delegates to
     * {@link DiskFreezeIndex#read(File)} and is not cached, so callers always get a fresh
     * empty index for the null case.
     */
    public DiskFreezeIndex getDiskFreezeIndex(File file) {
        if (file == null) {
            return DiskFreezeIndex.read(null);
        }
        DiskFreezeIndex existing = diskFreezeIndexCache.get(file);
        if (existing != null) {
            return existing;
        }
        DiskFreezeIndex fresh = DiskFreezeIndex.read(file);
        diskFreezeIndexCache.put(file, fresh);
        return fresh;
    }

    public Set<String> getWarnedMissingFreezeIds() {
        return warnedMissingFreezeIds;
    }
}
