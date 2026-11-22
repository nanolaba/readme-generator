package com.nanolaba.nrg.core;

import com.nanolaba.logging.MemoryLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.ERROR;
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

    private static final String NOTHING_FOUND =
            "gradle reader: no gradle.properties or build.gradle(.kts) found at configured location";

    @Test
    void missingFilesAreLoggedOnce(@TempDir Path tmp) {
        RegexGradleReader r = new RegexGradleReader(
                tmp.resolve("gradle.properties").toFile(), tmp.resolve("build.gradle").toFile());
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertFalse(r.read("version").isPresent());
            assertFalse(r.read("group").isPresent());
            assertEquals(Collections.singletonList(NOTHING_FOUND), log.getMessages());
        }
    }

    @Test
    void anyReadableFileSilencesTheNotFoundError() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            File script = new File("src/test/resources/fixtures/gradle-kotlin/build.gradle.kts");
            assertEquals(Optional.of("7.8.9"), new RegexGradleReader(null, script).read("version"));
            File props = new File("src/test/resources/fixtures/gradle-groovy/gradle.properties");
            assertEquals(Optional.of("1.9.22"), new RegexGradleReader(props, null).read("kotlin.version"));
            assertEquals(Collections.emptyList(), log.getMessages());
        }
    }

    @Test
    void malformedPropertiesAreLoggedAndTheBuildScriptStillAnswers(@TempDir Path tmp) throws IOException {
        Path props = tmp.resolve("gradle.properties");
        // A malformed unicode escape makes Properties.load throw.
        Files.write(props, "version=\\u12G4\n".getBytes(StandardCharsets.ISO_8859_1));
        Path script = tmp.resolve("build.gradle");
        Files.write(script, "version = '2.0'\n".getBytes(StandardCharsets.UTF_8));
        try (MemoryLogger log = MemoryLogger.capture()) {
            RegexGradleReader r = new RegexGradleReader(props.toFile(), script.toFile());
            assertEquals(Optional.of("2.0"), r.read("version"));
            assertEquals(Optional.empty(), r.read("other"));
            log.assertLogged(ERROR, "gradle reader: failed to parse " + props.toFile().getAbsolutePath());
            assertFalse(log.contains(ERROR, NOTHING_FOUND), log.getMessages().toString());
        }
    }

    @Test
    void unreadableBuildScriptIsLogged(@TempDir Path tmp) {
        // The script vanishes between the isFile() check and the read.
        File vanished = new File(tmp.toFile(), "build.gradle") {
            @Override
            public boolean isFile() {
                return true;
            }
        };
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertEquals(Optional.empty(), new RegexGradleReader(null, vanished).read("version"));
            log.assertLogged(ERROR, "gradle reader: failed to read " + vanished.getAbsolutePath());
            assertFalse(log.contains(ERROR, NOTHING_FOUND), log.getMessages().toString());
        }
    }
}
