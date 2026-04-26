package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

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
}
