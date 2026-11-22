package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.logging.MemoryLogger;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.NRG;
import com.nanolaba.nrg.core.freeze.DiskFreezeIndex;
import com.nanolaba.nrg.widgets.DefaultWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.TodoWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.DEBUG;
import static com.nanolaba.logging.LogEntry.LogEntryLevel.WARN;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_GRADLE_PATH;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_NPM_PATH;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorConfigTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    // The entries are captured rather than read from the console: under a coding agent the
    // default console of nanolog shows WARN and above only.
    @Test
    public void testPrintEmptyProperties() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            new GeneratorConfig(new File("README.src.md"), "", null);
            log.assertLogged(DEBUG, "Generator configuration:");
            log.assertLogged(DEBUG, "widgets: [");
            log.assertLogged(DEBUG, "sourceFile: README.src.md");
            log.assertLogged(DEBUG, "languages: [en]");
            log.assertLogged(DEBUG, "defaultLanguage: en");
            log.assertLogged(DEBUG, "properties (names only): []");

            assertFalse(log.contains(DEBUG, "sourceFileBody: "), log.getMessages().toString());
        }
    }

    @Test
    public void testPrintProperties() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            new GeneratorConfig(new File("README.src.md"), "<!--@nrg.languages=en,ru-->", null);
            // Names only: values may carry secrets taken from the environment.
            log.assertLogged(DEBUG, "properties (names only): [nrg.languages]");
        }
    }

    @Test
    public void testGetWidget() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "", null);

        assertNull(config.getWidget(null));
        assertNull(config.getWidget(""));
        assertNull(config.getWidget("123"));

        assertNotNull(config.getWidget("languages"));
    }

    @Test
    public void testConfigLanguages1() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--@" + PROPERTY_LANGUAGES + "=ru-->", null);
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("ru", langs.get(0));
        assertEquals("ru", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages2() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text<!-- @" + PROPERTY_LANGUAGES + " = zz -->some text", null);
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("zz", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguages3() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text<!-- @" + PROPERTY_LANGUAGES + " = aa ,bb, cc , dd-->some text", null);
        List<String> langs = config.getLanguages();
        assertEquals(4, langs.size());
        assertEquals("aa", langs.get(0));
        assertEquals("bb", langs.get(1));
        assertEquals("cc", langs.get(2));
        assertEquals("dd", langs.get(3));
        assertEquals("aa", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguagesWithDefaultLanguage1() {
        GeneratorConfig config = new Generator(new File("README.src.md"),
                "<!-- @" + PROPERTY_LANGUAGES + "=xx-->" + RN +
                "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->").getConfig();
        List<String> langs = config.getLanguages();
        assertEquals(1, langs.size());
        assertEquals("xx", langs.get(0));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testConfigLanguagesWithDefaultLanguage2() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"),
                "<!--comment-->some text" +
                "<!-- @" + PROPERTY_LANGUAGES + " = zz, xx -->some text" +
                        "<!--@" + PROPERTY_DEFAULT_LANGUAGE + "=xx-->", null);
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("zz", langs.get(0));
        assertEquals("xx", langs.get(1));
        assertEquals("xx", config.getDefaultLanguage());
    }

    @Test
    public void testParseConfigWithContent() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "<!--@nrg.languages=en,ru-->\n<!--@nrg.defaultLanguage=en-->\n# Nanolaba Readme Generator (NRG)", null);
        List<String> langs = config.getLanguages();
        assertEquals(2, langs.size());
        assertEquals("en", langs.get(0));
        assertEquals("ru", langs.get(1));
        assertEquals("en", config.getDefaultLanguage());
    }

    @Test
    public void testDebugDumpShowsPropertyNamesButNotValues() {
        // A property value may come from ${env.…}; the debug dump must not put it into a log.
        NRG.applyLogLevel(NRG.LogLevel.DEBUG);
        try {
            new GeneratorConfig(new File("README.src.md"),
                    "<!--@apiToken=${env.API_TOKEN}-->\n<!--@title=Visible Name-->", null,
                    name -> "API_TOKEN".equals(name) ? "s3cr3t-value" : null);
        } finally {
            LOG.init();
        }

        String log = getOutAndClear() + getErrAndClear();
        assertTrue(log.contains("apiToken"), log);
        assertTrue(log.contains("title"), log);
        assertFalse(log.contains("s3cr3t-value"), log);
        assertFalse(log.contains("Visible Name"), log);
    }

    @Test
    public void testPrimaryNameCollisionLogsWarning() {
        NRGWidget shadow = new DefaultWidget() {
            @Override
            public String getName() {
                return "todo";
            }

            @Override
            public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
                return "";
            }
        };
        try (MemoryLogger log = MemoryLogger.capture()) {
            GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "", Collections.singletonList(shadow));

            log.assertLogged(WARN, "Widget name 'todo' is declared by " + TodoWidget.class.getName() + " and ");
            assertSame(shadow, config.getWidget("todo"), "last registered wins");
        }
    }

    @Test
    public void testVariantsSkipEmptyEntries() {
        GeneratorConfig config = new GeneratorConfig(new File("README.src.md"), "<!--@nrg.variants= pro, ,lite,-->", null);
        assertEquals(Arrays.asList("pro", "lite"), config.getVariants());
    }

    @Test
    public void testRunContextIsPassedToAChildConfig() {
        File rootSource = tempDir.resolve("README.src.md").toFile();
        // Not what detection would find for the root source, so a child that detects its own shows.
        File root = tempDir.resolve("project").toFile();
        Function<String, String> env = name -> "from the caller";
        GeneratorConfig parent = new GeneratorConfig(rootSource, "", null);
        parent.setVariant("pro");
        parent.setCollectSourceMap(true);
        parent.setEnvRestricted(true);
        parent.setOffline(true);
        parent.setProjectRoot(root);
        parent.setEnvProvider(env);
        parent.setExecAllowed(true);
        parent.setStructureScan(true);

        GeneratorConfig child = new GeneratorConfig(tempDir.resolve("docs/part.src.md").toFile(), "", null);
        parent.passRunContextTo(child);

        assertEquals("pro", child.getVariant());
        assertTrue(child.isCollectSourceMap());
        assertTrue(child.isEnvRestricted());
        assertTrue(child.isOffline());
        assertSame(rootSource, child.getRootSourceFile());
        assertSame(root, child.getProjectRoot());
        assertSame(env, child.getEnvProvider());
        assertTrue(child.isExecAllowed());
        assertTrue(child.isStructureScan());
    }

    @Test
    public void testImportedTemplateConfigTakesTheRunContextAndTheMissingProperties() {
        GeneratorConfig parent = new GeneratorConfig(new File("README.src.md"),
                "<!--@shared=from parent-->\n<!--@own=from parent-->", null);
        parent.setOffline(true);
        File part = new File("docs/part.src.md");

        GeneratorConfig child = parent.forImportedTemplate(part, "<!--@own=from part-->", null);

        assertSame(part, child.getSourceFile());
        assertTrue(child.isOffline());
        assertEquals("from parent", child.getProperties().getProperty("shared"));
        assertEquals("from part", child.getProperties().getProperty("own"), "the fragment's own declaration wins");
    }

    @Test
    public void testNpmReaderReadsTheConfiguredPackageJson() throws IOException {
        write("package.json", "{\"version\": \"1.0.0\"}");
        File nested = write("web/package.json", "{\"version\": \"2.0.0\"}");

        assertEquals("1.0.0", npmVersion(configIn("")));
        assertEquals("1.0.0", npmVersion(configIn("<!--@nrg.npm.path=-->")), "an empty path is the default");
        assertEquals("2.0.0", npmVersion(configIn("<!--@nrg.npm.path=web/package.json-->")));
        GeneratorConfig absolute = configIn("");
        absolute.getProperties().setProperty(PROPERTY_NPM_PATH, nested.getAbsolutePath());
        assertEquals("2.0.0", npmVersion(absolute));
    }

    @Test
    public void testGradleReaderPrefersTheGroovyBuildScriptNextToTheTemplate() throws IOException {
        write("gradle.properties", "kotlinVersion=1.9\n");
        write("build.gradle", "version = '1.0'\n");
        write("build.gradle.kts", "version = \"2.0\"\n");

        GradleReader reader = configIn("").getGradleReader();
        assertEquals("1.9", reader.read("kotlinVersion").orElse(null));
        assertEquals("1.0", reader.read("version").orElse(null));
    }

    @Test
    public void testGradleReaderFallsBackToTheKotlinBuildScript() throws IOException {
        write("build.gradle.kts", "version = \"2.0\"\n");

        assertEquals("2.0", configIn("").getGradleReader().read("version").orElse(null));
    }

    @Test
    public void testGradlePathMayNameADirectory() throws IOException {
        write("gradle.properties", "kotlinVersion=root\n");
        write("sub/gradle.properties", "kotlinVersion=sub\n");

        assertEquals("sub", configIn("<!--@nrg.gradle.path=sub-->").getGradleReader()
                .read("kotlinVersion").orElse(null));
    }

    @Test
    public void testGradlePathMayNameTheBuildScript() throws IOException {
        write("sub/gradle.properties", "kotlinVersion=sub\n");
        File script = write("sub/custom.gradle", "version = '3.0'\n");
        GeneratorConfig config = configIn("");
        config.getProperties().setProperty(PROPERTY_GRADLE_PATH, script.getAbsolutePath());

        GradleReader reader = config.getGradleReader();
        assertEquals("3.0", reader.read("version").orElse(null));
        assertEquals("sub", reader.read("kotlinVersion").orElse(null), "the gradle.properties beside it is read too");
    }

    @Test
    public void testDescriptorFilesListWhatExists() throws IOException {
        write("pom.xml", "<project/>");
        write("package.json", "{}");
        write("gradle.properties", "a=1\n");
        GeneratorConfig config = configIn("");

        assertEquals(Collections.singletonList("pom.xml"), names(config.getDescriptorFiles("pom")));
        assertEquals(Collections.singletonList("package.json"), names(config.getDescriptorFiles("npm")));
        assertEquals(Collections.singletonList("gradle.properties"), names(config.getDescriptorFiles("gradle")),
                "no build script: only the properties file");
        assertTrue(config.getDescriptorFiles("maven").isEmpty());
    }

    @Test
    public void testDiskFreezeIndexIsCachedPerFile() throws IOException {
        File output = write("README.md", "<!--nrg.freeze id=\"a\"-->\nkept\n<!--/nrg.freeze-->\n");
        GeneratorConfig config = configIn("");

        DiskFreezeIndex first = config.getDiskFreezeIndex(output);
        assertEquals("kept\n", first.lookup("a").orElse(null));
        assertSame(first, config.getDiskFreezeIndex(output));
        assertFalse(config.getDiskFreezeIndex(null).lookup("a").isPresent(), "no file, no frozen content");
    }

    private File write(String relative, String content) throws IOException {
        Path path = tempDir.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }

    /** A template in the temporary directory, confined to it, so descriptors are read from there. */
    private GeneratorConfig configIn(String template) {
        GeneratorConfig config = new GeneratorConfig(tempDir.resolve("README.src.md").toFile(), template, null);
        config.setProjectRoot(tempDir.toFile());
        return config;
    }

    private static String npmVersion(GeneratorConfig config) {
        return config.getNpmReader().read("version").orElse(null);
    }

    private static List<String> names(List<File> files) {
        return files.stream().map(File::getName).collect(Collectors.toList());
    }
}