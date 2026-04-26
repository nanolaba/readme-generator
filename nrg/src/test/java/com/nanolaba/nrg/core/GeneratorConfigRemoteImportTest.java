package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorConfigRemoteImportTest {

    private static GeneratorConfig configFor(String body) {
        return new GeneratorConfig(new File("dummy.src.md"), body, null);
    }

    @Test
    void allowRemoteImportsDefaultsToFalse() {
        GeneratorConfig c = configFor("# title\n");
        assertFalse(c.isAllowRemoteImports());
    }

    @Test
    void allowRemoteImportsParsedFromTemplate() {
        GeneratorConfig c = configFor("<!--@nrg.allowRemoteImports=true-->\n# title\n");
        assertTrue(c.isAllowRemoteImports());
    }

    @Test
    void cacheDirDefaultsToUserHome() {
        GeneratorConfig c = configFor("# title\n");
        Path expected = Paths.get(System.getProperty("user.home"), ".nrg", "cache");
        assertEquals(expected, c.getCacheDir());
    }

    @Test
    void cacheDirOverriddenByTemplateProperty() {
        GeneratorConfig c = configFor("<!--@nrg.cacheDir=/tmp/custom-cache-->\n# title\n");
        assertEquals(Paths.get("/tmp/custom-cache"), c.getCacheDir());
    }

    @Test
    void requireSha256FromSystemProperty() {
        String prop = NRGConstants.PROPERTY_REQUIRE_SHA256_FOR_REMOTE;
        String old = System.getProperty(prop);
        try {
            System.setProperty(prop, "true");
            GeneratorConfig c = configFor("# title\n");
            assertTrue(c.isRequireSha256ForRemote());
        } finally {
            if (old == null) System.clearProperty(prop); else System.setProperty(prop, old);
        }
    }

    @Test
    void requireSha256DefaultsFalse() {
        String prop = NRGConstants.PROPERTY_REQUIRE_SHA256_FOR_REMOTE;
        String old = System.getProperty(prop);
        System.clearProperty(prop);
        try {
            GeneratorConfig c = configFor("# title\n");
            assertFalse(c.isRequireSha256ForRemote());
        } finally {
            if (old != null) System.setProperty(prop, old);
        }
    }
}
