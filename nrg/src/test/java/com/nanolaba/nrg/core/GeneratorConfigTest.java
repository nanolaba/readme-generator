package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.logging.MemoryLogger;
import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.NRG;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.DEBUG;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_DEFAULT_LANGUAGE;
import static com.nanolaba.nrg.core.NRGConstants.PROPERTY_LANGUAGES;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorConfigTest extends DefaultNRGTest {

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
}