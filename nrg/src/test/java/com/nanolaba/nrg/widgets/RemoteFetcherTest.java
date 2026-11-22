package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LogEntry;
import com.nanolaba.logging.MemoryLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RemoteFetcherTest {

    private static final String SHA_OF_HELLO =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    private static UrlOpener stubOpener(byte[] body, AtomicInteger calls) {
        return (url, timeout, redirects) -> {
            calls.incrementAndGet();
            return new UrlOpener.Response(body);
        };
    }

    private static UrlOpener throwingOpener() {
        return (url, timeout, redirects) -> { throw new IOException("network down"); };
    }

    private static Clock fixedClock(long epochMs) {
        return Clock.fixed(Instant.ofEpochMilli(epochMs), ZoneId.of("UTC"));
    }

    @Test
    void happyPathReturnsBytes(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1L));
        byte[] bytes = f.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, DurationParser.DISABLED, null), tmp);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
        assertEquals(1, calls.get());
    }

    @Test
    void sha256MatchSucceeds(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1L));
        byte[] bytes = f.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, DurationParser.DISABLED, SHA_OF_HELLO), tmp);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    void sha256MismatchOnFreshFetchThrows(@TempDir Path tmp) {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1L));
        IOException ex = assertThrows(IOException.class, () -> f.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, DurationParser.DISABLED,
                "0000000000000000000000000000000000000000000000000000000000000000"), tmp));
        assertTrue(ex.getMessage().contains("expected="));
        assertTrue(ex.getMessage().contains("actual=" + SHA_OF_HELLO));
    }

    @Test
    void cacheHitShortCircuitsOpener(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1_000L));
        f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 3_600_000L, null), tmp);
        assertEquals(1, calls.get());
        byte[] cached = f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 3_600_000L, null), tmp);
        assertEquals(1, calls.get());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), cached);
    }

    @Test
    void zeroTtlDisablesTheCache(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1_000L));
        f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 0L, null), tmp);
        f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 0L, null), tmp);

        assertEquals(2, calls.get());
        assertEquals(0, Files.list(tmp).count(), "nothing may be written to the cache");
    }

    @Test
    void openerIsGivenTheParsedUrlAndTheRedirectLimit(@TempDir Path tmp) throws IOException {
        AtomicReference<URL> seenUrl = new AtomicReference<>();
        AtomicInteger seenRedirects = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher((url, timeout, redirects) -> {
            seenUrl.set(url);
            seenRedirects.set(redirects);
            return new UrlOpener.Response("hello".getBytes(StandardCharsets.UTF_8));
        }, fixedClock(1L));

        f.fetch(new RemoteFetchSpec("https://example.com/a/b.md?x=1", 10_000L, DurationParser.DISABLED, null), tmp);

        // Compared as text: URL.equals would resolve the host name.
        assertEquals("https://example.com/a/b.md?x=1", seenUrl.get().toExternalForm());
        assertEquals(5, seenRedirects.get());
    }

    @Test
    void unpinnedFetchLogsTheHashToPinAndPinnedFetchDoesNot(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(StandardCharsets.UTF_8), calls), fixedClock(1L));

        try (MemoryLogger log = MemoryLogger.capture()) {
            f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, DurationParser.DISABLED, null), tmp);
            log.assertLogged(LogEntry.LogEntryLevel.INFO,
                    "import url=https://example.com/x sha256=" + SHA_OF_HELLO + " - pin this hash for reproducibility");
        }
        try (MemoryLogger log = MemoryLogger.capture()) {
            f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, DurationParser.DISABLED, SHA_OF_HELLO), tmp);
            assertFalse(log.contains(LogEntry.LogEntryLevel.INFO, "pin this hash"), String.valueOf(log.getMessages()));
        }
    }

    @Test
    void networkFailureWithStaleCacheReturnsStale(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher seed = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1_000L));
        seed.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 60_000L, null), tmp);
        RemoteFetcher offline = new RemoteFetcher(throwingOpener(), fixedClock(999_999L));
        try (MemoryLogger log = MemoryLogger.capture()) {
            byte[] bytes = offline.fetch(new RemoteFetchSpec(
                    "https://example.com/x", 10_000L, 60_000L, null), tmp);
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
            log.assertLogged(LogEntry.LogEntryLevel.WARN, "remote fetch failed, using stale cache for https://example.com/x");
        }
    }

    @Test
    void networkFailureWithNoCacheThrows(@TempDir Path tmp) {
        RemoteFetcher f = new RemoteFetcher(throwingOpener(), fixedClock(1L));
        assertThrows(IOException.class, () -> f.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, DurationParser.DISABLED, null), tmp));
    }

    @Test
    void cacheHitWithMismatchedShaEvictsAndRefetches(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1L));
        f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 3_600_000L, SHA_OF_HELLO), tmp);
        assertEquals(1, calls.get());
        try (MemoryLogger log = MemoryLogger.capture()) {
            assertThrows(IOException.class, () -> f.fetch(new RemoteFetchSpec(
                    "https://example.com/x", 10_000L, 3_600_000L,
                    "1111111111111111111111111111111111111111111111111111111111111111"), tmp));
            log.assertLogged(LogEntry.LogEntryLevel.WARN,
                    "cache: pinned sha256 differs from cached entry, evicting and refetching (https://example.com/x)");
        }
        assertEquals(2, calls.get(), "cache should have been evicted and opener called again");
        assertFalse(new CacheStore(tmp).lookup("https://example.com/x").isPresent(), "the mismatching entry is gone");
    }

    @Test
    void nonHttpSchemeThrows(@TempDir Path tmp) {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher(stubOpener(new byte[0], calls), fixedClock(1L));
        assertThrows(IOException.class, () -> f.fetch(new RemoteFetchSpec(
                "file:///etc/passwd", 10_000L, DurationParser.DISABLED, null), tmp));
        assertEquals(0, calls.get(), "scheme rejection should happen before opener is called");
    }

    @Test
    void networkFailureWithStaleCacheButShaMismatchRethrows(@TempDir Path tmp) throws IOException {
        // Seed cache with "hello" — at t=1000.
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher seed = new RemoteFetcher(stubOpener("hello".getBytes(StandardCharsets.UTF_8), calls), fixedClock(1_000L));
        seed.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, 60_000L, null), tmp);

        // Now go offline at t=999_999 (cache is stale — past 60s TTL) AND request a different pinned sha.
        RemoteFetcher offline = new RemoteFetcher(throwingOpener(), fixedClock(999_999L));
        try (MemoryLogger log = MemoryLogger.capture()) {
            IOException ex = assertThrows(IOException.class, () -> offline.fetch(new RemoteFetchSpec(
                    "https://example.com/x", 10_000L, 60_000L,
                    "1111111111111111111111111111111111111111111111111111111111111111"), tmp));
            // The rethrown exception is the original network failure, not a sha-mismatch.
            assertEquals("network down", ex.getMessage());
            log.assertLogged(LogEntry.LogEntryLevel.WARN,
                    "remote fetch failed and stale cache for https://example.com/x does not match pinned sha256, rethrowing");
        }
    }

    @Test
    void timeoutBeyondIntRangeIsClampedNotWrapped(@TempDir Path tmp) throws IOException {
        // timeout='30d' is 2_592_000_000 ms; cast to int it wrapped to a negative value and
        // HttpURLConnection rejected it with "timeouts can't be negative".
        AtomicInteger seenTimeout = new AtomicInteger();
        RemoteFetcher f = new RemoteFetcher((url, timeout, redirects) -> {
            seenTimeout.set(timeout);
            return new UrlOpener.Response("hello".getBytes(StandardCharsets.UTF_8));
        }, fixedClock(1L));

        f.fetch(new RemoteFetchSpec("https://example.com/x",
                DurationParser.parseMillis("30d"), DurationParser.DISABLED, null), tmp);

        assertEquals(Integer.MAX_VALUE, seenTimeout.get());
    }
}
