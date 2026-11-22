package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LogEntry;
import com.nanolaba.logging.MemoryLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        try (MemoryLogger log = MemoryLogger.capture()) {
            assertFalse(store.lookup("https://example.com/x").isPresent());
            log.assertLogged(LogEntry.LogEntryLevel.WARN, "cache: corrupt or unreadable entry, evicting (https://example.com/x)");
        }
        assertEquals(0, Files.list(tmp).count());
    }

    @Test
    void unwritableDirSilentlySkipsSave(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("not-a-dir.txt");
        Files.write(file, new byte[]{0});
        CacheStore store = new CacheStore(file);
        try (MemoryLogger log = MemoryLogger.capture()) {
            store.save("https://example.com/x", new byte[]{1}, 1L);
            log.assertLogged(LogEntry.LogEntryLevel.WARN,
                    "cache: unable to write entry for https://example.com/x - continuing without cache");
        }
        assertFalse(store.lookup("https://example.com/x").isPresent());
    }

    @Test
    void entryIsStoredUnderTheSha1OfTheUrlWithTheUrlInItsSidecar(@TempDir Path tmp) throws IOException {
        // The key is a fixed format on disk: a cache written by one NRG version is read by the next.
        new CacheStore(tmp).save("https://example.com/419", new byte[]{7}, 42L);

        assertArrayEquals(new byte[]{7}, Files.readAllBytes(tmp.resolve("00a36860a6efcd8805aba612db4b2eea2a32d0ac.bin")));
        assertEquals("{\"url\":\"https://example.com/419\",\"fetchedAtEpochMs\":42}",
                new String(Files.readAllBytes(tmp.resolve("00a36860a6efcd8805aba612db4b2eea2a32d0ac.json")),
                        StandardCharsets.UTF_8));
    }

    @Test
    void sidecarEscapesQuotesAndBackslashesOfTheUrl(@TempDir Path tmp) throws IOException {
        String url = "https://example.com/a\"b\\c";
        CacheStore store = new CacheStore(tmp);
        store.save(url, new byte[]{1}, 5L);

        Path sidecar = Files.list(tmp).filter(p -> p.toString().endsWith(".json")).findFirst().get();
        assertEquals("{\"url\":\"https://example.com/a\\\"b\\\\c\",\"fetchedAtEpochMs\":5}",
                new String(Files.readAllBytes(sidecar), StandardCharsets.UTF_8));
        assertEquals(5L, store.lookup(url).get().getFetchedAtEpochMs());
    }

    @Test
    void evictRemovesBothFilesOfTheEntry(@TempDir Path tmp) throws IOException {
        CacheStore store = new CacheStore(tmp);
        store.save("https://example.com/x", new byte[]{1}, 1L);
        store.save("https://example.com/other", new byte[]{2}, 1L);

        store.evict("https://example.com/x");

        assertFalse(store.lookup("https://example.com/x").isPresent());
        assertTrue(store.lookup("https://example.com/other").isPresent());
        assertEquals(2, Files.list(tmp).count());
    }

    @Test
    void entryIsFreshUpToAndIncludingTheTtl() {
        CacheStore.CachedEntry entry = new CacheStore.CachedEntry(new byte[0], 1_000L);

        assertTrue(entry.isFreshAt(1_000L, 500L));
        assertTrue(entry.isFreshAt(1_400L, 500L));
        assertTrue(entry.isFreshAt(1_500L, 500L));
        assertFalse(entry.isFreshAt(1_501L, 500L));
    }

    @Test
    void zeroOrNegativeTtlIsNeverFresh() {
        CacheStore.CachedEntry entry = new CacheStore.CachedEntry(new byte[0], 1_000L);

        assertFalse(entry.isFreshAt(1_000L, 0L));
        assertFalse(entry.isFreshAt(900L, 0L));
        assertFalse(entry.isFreshAt(1_000L, -1L));
    }
}
