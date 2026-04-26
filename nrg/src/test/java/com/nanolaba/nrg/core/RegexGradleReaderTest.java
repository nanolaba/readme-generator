package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegexGradleReaderTest {

    @Test
    void readsVersionAndGroupFromGroovyDsl() {
        File props = new File("src/test/resources/fixtures/gradle-groovy/gradle.properties");
        File script = new File("src/test/resources/fixtures/gradle-groovy/build.gradle");
        RegexGradleReader r = new RegexGradleReader(props, script);

        assertEquals(Optional.of("4.5.6"), r.read("version"));
        assertEquals(Optional.of("com.example.groovy"), r.read("group"));
    }

    @Test
    void readsArbitraryKeyFromGradleProperties() {
        File props = new File("src/test/resources/fixtures/gradle-groovy/gradle.properties");
        File script = new File("src/test/resources/fixtures/gradle-groovy/build.gradle");
        RegexGradleReader r = new RegexGradleReader(props, script);

        assertEquals(Optional.of("1.9.22"), r.read("kotlin.version"));
        assertEquals(Optional.of("on"), r.read("extra.flag"));
    }

    @Test
    void readsVersionFromKotlinDsl() {
        File script = new File("src/test/resources/fixtures/gradle-kotlin/build.gradle.kts");
        RegexGradleReader r = new RegexGradleReader(null, script);

        assertEquals(Optional.of("7.8.9"), r.read("version"));
        assertEquals(Optional.of("com.example.kotlin"), r.read("group"));
    }

    @Test
    void missingFilesReturnEmpty() {
        RegexGradleReader r = new RegexGradleReader(null, null);
        assertFalse(r.read("version").isPresent());
        assertFalse(r.read("anything").isPresent());
    }
}
