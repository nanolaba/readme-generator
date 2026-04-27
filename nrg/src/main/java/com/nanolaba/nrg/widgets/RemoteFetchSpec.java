package com.nanolaba.nrg.widgets;

/**
 * Immutable value object holding everything {@link RemoteFetcher} needs to resolve a
 * single remote import: target URL, request {@code timeoutMillis}, cache {@code ttlMillis}
 * (negative = caching disabled), and the optional pinned {@code expectedSha256} (null = unpinned).
 */
final class RemoteFetchSpec {

    private final String url;
    private final long timeoutMillis;
    private final long cacheTtlMillis;     // -1 = disabled
    private final String expectedSha256;   // null = unset

    RemoteFetchSpec(String url, long timeoutMillis, long cacheTtlMillis, String expectedSha256) {
        this.url = url;
        this.timeoutMillis = timeoutMillis;
        this.cacheTtlMillis = cacheTtlMillis;
        this.expectedSha256 = expectedSha256;
    }

    String getUrl() { return url; }
    long getTimeoutMillis() { return timeoutMillis; }
    long getCacheTtlMillis() { return cacheTtlMillis; }
    String getExpectedSha256() { return expectedSha256; }
}
