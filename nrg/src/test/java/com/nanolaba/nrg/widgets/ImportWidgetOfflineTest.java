package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.DefaultNRGTest;
import com.nanolaba.nrg.core.Generator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --offline}: a remote import never opens a connection — a sha256-pinned URL is served
 * from the cache when the cached bytes match the pin, anything else fails, and the template's
 * own {@code nrg.allowRemoteImports} cannot lift it.
 */
class ImportWidgetOfflineTest extends DefaultNRGTest {

    @TempDir
    Path tempDir;

    private static final String URL = "https://example.com/snippet.md";

    private Generator generator(String importParams) {
        String body = "<!--@nrg.allowRemoteImports=true-->\n"
                + "<!--@nrg.cacheDir=" + tempDir.resolve("cache").toString().replace('\\', '/') + "-->\n"
                + "${widget:import(url='" + URL + "', run-generator='false'" + importParams + ")}\n";
        Generator generator = new Generator(new File(tempDir.toFile(), "README.src.md"), body);
        generator.getConfig().setOffline(true);
        return generator;
    }

    @Test
    public void testUnpinnedRemoteImportIsRefused() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> generator("").getResult("en"));
        assertTrue(e.getMessage().contains("disabled by --offline"), e.getMessage());
    }

    @Test
    public void testPinnedImportIsServedFromTheCache() {
        byte[] bytes = "cached text".getBytes(StandardCharsets.UTF_8);
        new CacheStore(tempDir.resolve("cache")).save(URL, bytes, System.currentTimeMillis());

        String out = generator(", sha256='" + Sha256Hex.hexOf(bytes) + "'").getResult("en").getContent().toString();
        assertTrue(out.contains("cached text"), out);
    }

    @Test
    public void testPinnedImportMissingFromTheCacheIsRefused() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> generator(", sha256='" + Sha256Hex.hexOf(new byte[]{1}) + "'").getResult("en"));
        assertTrue(e.getMessage().contains("is not in the cache"), e.getMessage());
    }

    @Test
    public void testCachedBytesThatDoNotMatchThePinAreRefused() {
        new CacheStore(tempDir.resolve("cache")).save(URL, "tampered".getBytes(StandardCharsets.UTF_8), System.currentTimeMillis());

        assertThrows(RuntimeException.class,
                () -> generator(", sha256='" + Sha256Hex.hexOf(new byte[]{1}) + "'").getResult("en"));
    }
}
