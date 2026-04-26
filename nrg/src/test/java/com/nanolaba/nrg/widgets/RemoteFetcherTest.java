package com.nanolaba.nrg.widgets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

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
        f.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 3_600_000L, null), tmp);
        assertEquals(1, calls.get());
    }

    @Test
    void networkFailureWithStaleCacheReturnsStale(@TempDir Path tmp) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        RemoteFetcher seed = new RemoteFetcher(stubOpener("hello".getBytes(), calls), fixedClock(1_000L));
        seed.fetch(new RemoteFetchSpec("https://example.com/x", 10_000L, 60_000L, null), tmp);
        RemoteFetcher offline = new RemoteFetcher(throwingOpener(), fixedClock(999_999L));
        byte[] bytes = offline.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, 60_000L, null), tmp);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
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
        assertThrows(IOException.class, () -> f.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, 3_600_000L,
                "1111111111111111111111111111111111111111111111111111111111111111"), tmp));
        assertEquals(2, calls.get(), "cache should have been evicted and opener called again");
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
        IOException ex = assertThrows(IOException.class, () -> offline.fetch(new RemoteFetchSpec(
                "https://example.com/x", 10_000L, 60_000L,
                "1111111111111111111111111111111111111111111111111111111111111111"), tmp));
        // The rethrown exception is the original network failure, not a sha-mismatch.
        assertEquals("network down", ex.getMessage());
    }
}
