package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class OutputFileNameResolverTest {

    private static File src(String path) {
        return new File(path);
    }

    private static String resolve(File source, String defaultLang, String lang, Properties props) {
        File f = OutputFileNameResolver.resolve(source, defaultLang, lang, props);
        return f.getPath().replace('\\', '/');
    }

    @Test
    void defaultBehaviour_defaultLanguage() {
        String r = resolve(src("repo/README.src.md"), "en", "en", new Properties());
        assertTrue(r.endsWith("repo/README.md"), r);
    }

    @Test
    void defaultBehaviour_otherLanguage() {
        String r = resolve(src("repo/README.src.md"), "en", "ru", new Properties());
        assertTrue(r.endsWith("repo/README.ru.md"), r);
    }

    @Test
    void globalPattern_appliesToAllLanguages() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "<base>_<LANG>.md");
        String defaultR = resolve(src("repo/README.src.md"), "en", "en", p);
        String otherR = resolve(src("repo/README.src.md"), "en", "ru", p);
        assertTrue(defaultR.endsWith("repo/README_EN.md"), defaultR);
        assertTrue(otherR.endsWith("repo/README_RU.md"), otherR);
    }

    @Test
    void defaultLanguagePattern_overridesGlobalForDefaultLangOnly() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "<base>.<lang>.md");
        p.setProperty("nrg.defaultLanguageFileNamePattern", "<base>.md");
        String defaultR = resolve(src("repo/README.src.md"), "en", "en", p);
        String otherR = resolve(src("repo/README.src.md"), "en", "ru", p);
        assertTrue(defaultR.endsWith("repo/README.md"), defaultR);
        assertTrue(otherR.endsWith("repo/README.ru.md"), otherR);
    }

    @Test
    void perLanguagePattern_overridesEverything() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "<base>.<lang>.md");
        p.setProperty("nrg.defaultLanguageFileNamePattern", "<base>.md");
        p.setProperty("nrg.fileNamePattern.zh-CN", "README_<LANG>.md");
        String r = resolve(src("repo/README.src.md"), "en", "zh-CN", p);
        assertTrue(r.endsWith("repo/README_ZH-CN.md"), r);
    }

    @Test
    void perLanguagePattern_beatsDefaultLanguagePatternForDefaultLang() {
        Properties p = new Properties();
        p.setProperty("nrg.defaultLanguageFileNamePattern", "<base>.md");
        p.setProperty("nrg.fileNamePattern.en", "docs/en/<base>.md");
        String r = resolve(src("repo/README.src.md"), "en", "en", p);
        assertTrue(r.endsWith("docs/en/README.md"), r);
    }

    @Test
    void pathBearingPattern_createsSubdirectoryLayout() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "docs/<lang>/<base>.md");
        String r = resolve(src("repo/README.src.md"), "en", "ru", p);
        assertTrue(r.endsWith("docs/ru/README.md"), r);
    }

    @Test
    void baseStripsOnlyDotSrcDotMd() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "<base>.<lang>.md");
        String r = resolve(src("repo/foo.bar.src.md"), "en", "ru", p);
        assertTrue(r.endsWith("repo/foo.bar.ru.md"), r);
    }

    @Test
    void langPlaceholderUppercase() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "<LANG>.md");
        String r = resolve(src("repo/README.src.md"), "en", "zh-CN", p);
        assertTrue(r.endsWith("ZH-CN.md"), r);
    }

    @Test
    void emptyPatternThrows() {
        Properties p = new Properties();
        p.setProperty("nrg.fileNamePattern", "");
        assertThrows(IllegalStateException.class,
                () -> OutputFileNameResolver.resolve(src("repo/README.src.md"), "en", "ru", p));
    }
}
