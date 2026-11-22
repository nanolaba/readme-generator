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

class JsonNpmReaderTest {

    private static File fixture() {
        return new File("src/test/resources/fixtures/npm-basic/package.json");
    }

    @Test
    void readsTopLevelString() {
        JsonNpmReader r = new JsonNpmReader(fixture());
        assertEquals(Optional.of("fixture-pkg"), r.read("name"));
        assertEquals(Optional.of("1.2.3"), r.read("version"));
    }

    @Test
    void readsNestedDependency() {
        JsonNpmReader r = new JsonNpmReader(fixture());
        assertEquals(Optional.of("^4.17.21"), r.read("dependencies.lodash"));
        assertEquals(Optional.of("1.3.0"), r.read("dependencies.left-pad"));
    }

    @Test
    void missingPathReturnsEmpty() {
        JsonNpmReader r = new JsonNpmReader(fixture());
        assertFalse(r.read("dependencies.missing").isPresent());
        assertFalse(r.read("nonexistent").isPresent());
    }

    @Test
    void objectLeafReturnsEmpty() {
        JsonNpmReader r = new JsonNpmReader(fixture());
        assertFalse(r.read("dependencies").isPresent());
    }

    @Test
    void missingFileReturnsEmpty() {
        JsonNpmReader r = new JsonNpmReader(new File("does-not-exist.json"));
        assertFalse(r.read("version").isPresent());
    }

    private static File write(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    @Test
    void numberAndBooleanLeavesAreRenderedAsText(@TempDir Path tmp) throws IOException {
        JsonNpmReader r = new JsonNpmReader(write(tmp.resolve("package.json"),
                "{\"n\": 42, \"d\": 1.5, \"b\": true, \"z\": null, \"xs\": [1], \"o\": {\"k\": \"v\"}}"));
        assertEquals(Optional.of("42"), r.read("n"));
        assertEquals(Optional.of("1.5"), r.read("d"));
        assertEquals(Optional.of("true"), r.read("b"));
        assertEquals(Optional.empty(), r.read("z"));
        assertEquals(Optional.empty(), r.read("xs"));
        assertEquals(Optional.empty(), r.read("n.deeper"));
        assertEquals(Optional.of("v"), r.read("o.k"));
    }

    @Test
    void missingFileIsLoggedOnceWithItsPath() {
        File missing = new File("does-not-exist.json");
        try (MemoryLogger log = MemoryLogger.capture()) {
            JsonNpmReader r = new JsonNpmReader(missing);
            assertFalse(r.read("version").isPresent());
            assertFalse(r.read("name").isPresent());
            assertEquals(Collections.singletonList("npm reader: file not found: " + missing.getAbsolutePath()),
                    log.getMessages());
        }
    }

    @Test
    void nullFileIsLoggedAsNull() {
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertFalse(new JsonNpmReader(null).read("version").isPresent());
            assertEquals(Collections.singletonList("npm reader: file not found: <null>"), log.getMessages());
        }
    }

    @Test
    void malformedJsonIsLoggedAndReadsAsEmpty(@TempDir Path tmp) throws IOException {
        File broken = write(tmp.resolve("package.json"), "{\"version\": \"1.0\",}");
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertFalse(new JsonNpmReader(broken).read("version").isPresent());
            log.assertLogged(ERROR, "npm reader: failed to parse " + broken.getAbsolutePath());
        }
    }
}
