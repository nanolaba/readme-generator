package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * On-disk cache backing the remote-import feature.
 *
 * <p>Each cached URL is stored as a pair of files keyed by the SHA-1 of the URL: a
 * {@code .bin} file with the raw bytes and a sibling {@code .json} sidecar carrying the
 * {@code fetchedAtEpochMs} timestamp (and the original URL, for debuggability). Corrupt
 * or unreadable entries are evicted on lookup; write failures are logged but never abort
 * the surrounding fetch — the cache is best-effort, not load-bearing.
 *
 * <p>SHA-1 is deliberate: it is used only as a filesystem-friendly key, never for
 * integrity. Integrity is enforced separately via {@link Sha256Hex} pinning.
 */
final class CacheStore {

    private final Path cacheDir;

    CacheStore(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    Optional<CachedEntry> lookup(String url) {
        String key = key(url);
        Path body = cacheDir.resolve(key + ".bin");
        Path sidecar = cacheDir.resolve(key + ".json");
        if (!Files.isRegularFile(body) || !Files.isRegularFile(sidecar)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(body);
            String json = new String(Files.readAllBytes(sidecar), StandardCharsets.UTF_8);
            long fetchedAt = parseFetchedAt(json);
            return Optional.of(new CachedEntry(bytes, fetchedAt));
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("cache: corrupt or unreadable entry, evicting ({})", url);
            evict(body, sidecar);
            return Optional.empty();
        }
    }

    void save(String url, byte[] bytes, long fetchedAtEpochMs) {
        try {
            if (!Files.isDirectory(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
            String key = key(url);
            Path body = cacheDir.resolve(key + ".bin");
            Path sidecar = cacheDir.resolve(key + ".json");
            Files.write(body, bytes);
            String json = "{\"url\":" + jsonString(url) + ",\"fetchedAtEpochMs\":" + fetchedAtEpochMs + "}";
            Files.write(sidecar, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.warn(e, "cache: unable to write entry for " + url + " - continuing without cache");
        }
    }

    void evict(String url) {
        String key = key(url);
        evict(cacheDir.resolve(key + ".bin"), cacheDir.resolve(key + ".json"));
    }

    private static void evict(Path body, Path sidecar) {
        try { Files.deleteIfExists(body); } catch (IOException ignored) {}
        try { Files.deleteIfExists(sidecar); } catch (IOException ignored) {}
    }

    private static String key(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private static final Pattern FETCHED_AT = Pattern.compile("\"fetchedAtEpochMs\"\\s*:\\s*(\\d+)");

    private static long parseFetchedAt(String json) {
        Matcher m = FETCHED_AT.matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("sidecar: missing fetchedAtEpochMs");
        }
        return Long.parseLong(m.group(1));
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static final class CachedEntry {

        private final byte[] bytes;
        private final long fetchedAtEpochMs;

        CachedEntry(byte[] bytes, long fetchedAtEpochMs) {
            this.bytes = bytes;
            this.fetchedAtEpochMs = fetchedAtEpochMs;
        }

        byte[] getBytes() {
            return bytes;
        }

        long getFetchedAtEpochMs() {
            return fetchedAtEpochMs;
        }

        boolean isFreshAt(long nowEpochMs, long ttlMs) {
            return ttlMs > 0 && (nowEpochMs - fetchedAtEpochMs) <= ttlMs;
        }
    }
}
