package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

/**
 * Coordinates remote-import payload retrieval: cache lookup, conditional refetch,
 * SHA-256 verification, and stale-cache fallback on network failure.
 *
 * <p>Cache hit logic — for a {@link RemoteFetchSpec} with a positive TTL, returns the
 * cached bytes when their {@code fetchedAt} timestamp is within the TTL. A pinned SHA-256
 * that disagrees with the cached entry forces eviction and refetch. On network failure,
 * an existing cached copy (even if stale) is used as a fallback unless its SHA-256
 * disagrees with the pin — in which case the original error is rethrown rather than
 * surfacing untrusted bytes.
 *
 * <p>Only HTTP and HTTPS schemes are accepted; everything else is rejected at parse time.
 */
final class RemoteFetcher {

    private final UrlOpener opener;
    private final Clock clock;
    private static final int MAX_REDIRECTS = 5;

    RemoteFetcher(UrlOpener opener, Clock clock) {
        this.opener = opener;
        this.clock = clock;
    }

    byte[] fetch(RemoteFetchSpec spec, Path cacheDir) throws IOException {
        URL url = parseHttpUrl(spec.getUrl());
        long now = clock.millis();
        boolean cacheEnabled = spec.getCacheTtlMillis() > 0;
        CacheStore cache = new CacheStore(cacheDir);

        Optional<CacheStore.CachedEntry> cached = cacheEnabled ? cache.lookup(spec.getUrl()) : Optional.empty();
        if (cached.isPresent() && cached.get().isFreshAt(now, spec.getCacheTtlMillis())) {
            byte[] bytes = cached.get().getBytes();
            if (spec.getExpectedSha256() != null && !Sha256Hex.hexOf(bytes).equals(spec.getExpectedSha256())) {
                LOG.warn("cache: pinned sha256 differs from cached entry, evicting and refetching ({})", spec.getUrl());
                cache.evict(spec.getUrl());
            } else {
                return bytes;
            }
        }

        byte[] bytes;
        try {
            bytes = opener.open(url, (int) spec.getTimeoutMillis(), MAX_REDIRECTS).getBody();
        } catch (IOException networkFailure) {
            if (cached.isPresent()) {
                byte[] stale = cached.get().getBytes();
                if (spec.getExpectedSha256() == null
                        || Sha256Hex.hexOf(stale).equals(spec.getExpectedSha256())) {
                    LOG.warn(networkFailure,
                            "remote fetch failed, using stale cache for " + spec.getUrl());
                    return stale;
                }
                LOG.warn("remote fetch failed and stale cache for {} does not match pinned sha256, rethrowing",
                        spec.getUrl());
            }
            throw networkFailure;
        }

        String actualSha = Sha256Hex.hexOf(bytes);
        if (spec.getExpectedSha256() != null && !actualSha.equals(spec.getExpectedSha256())) {
            throw new IOException("import url=" + spec.getUrl()
                    + ": sha256 mismatch (expected=" + spec.getExpectedSha256() + ", actual=" + actualSha + ")");
        }
        if (spec.getExpectedSha256() == null) {
            LOG.info("import url={} sha256={} - pin this hash for reproducibility", spec.getUrl(), actualSha);
        }

        if (cacheEnabled) {
            cache.save(spec.getUrl(), bytes, now);
        }
        return bytes;
    }

    private static URL parseHttpUrl(String s) throws IOException {
        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw new IOException("import: invalid URL '" + s + "'", e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IOException("import: only http(s) URLs are allowed, got '" + s + "'");
        }
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IOException("import: malformed URL '" + s + "'", e);
        }
    }
}
