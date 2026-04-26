package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheStoreTest {

    @Test
    void saveThenLookupReturnsBytes(@TempDir Path tmp) throws IOException {
        CacheStore store = new CacheStore(tmp);
        byte[] bytes = "hello".getBytes();
        store.save("https://example.com/x", bytes, 1_000L);
        Optional<CacheStore.CachedEntry> e = store.lookup("https://example.com/x");
        assertTrue(e.isPresent());
        assertArrayEquals(bytes, e.get().getBytes());
        assertEquals(1_000L, e.get().getFetchedAtEpochMs());
    }

    @Test
    void lookupMissingReturnsEmpty(@TempDir Path tmp) {
        CacheStore store = new CacheStore(tmp);
        assertFalse(store.lookup("https://example.com/missing").isPresent());
    }

    @Test
    void differentUrlsAreSegregated(@TempDir Path tmp) throws IOException {
        CacheStore store = new CacheStore(tmp);
        store.save("https://a.example/x", new byte[]{1, 2}, 1L);
        store.save("https://b.example/x", new byte[]{3, 4}, 2L);
        assertArrayEquals(new byte[]{1, 2}, store.lookup("https://a.example/x").get().getBytes());
        assertArrayEquals(new byte[]{3, 4}, store.lookup("https://b.example/x").get().getBytes());
    }

    @Test
    void corruptSidecarEvictsAndReturnsEmpty(@TempDir Path tmp) throws IOException {
        CacheStore store = new CacheStore(tmp);
        store.save("https://example.com/x", new byte[]{1}, 1L);
        Path[] sidecar = Files.list(tmp).filter(p -> p.toString().endsWith(".json")).toArray(Path[]::new);
        assertEquals(1, sidecar.length);
        Files.write(sidecar[0], "not json".getBytes());

        assertFalse(store.lookup("https://example.com/x").isPresent());
        assertEquals(0, Files.list(tmp).count());
    }

    @Test
    void unwritableDirSilentlySkipsSave(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("not-a-dir.txt");
        Files.write(file, new byte[]{0});
        CacheStore store = new CacheStore(file);
        store.save("https://example.com/x", new byte[]{1}, 1L);
        assertFalse(store.lookup("https://example.com/x").isPresent());
    }
}
