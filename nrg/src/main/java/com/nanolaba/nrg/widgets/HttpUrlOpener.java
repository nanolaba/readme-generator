package com.nanolaba.nrg.widgets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Production {@link UrlOpener} implementation backed by {@link HttpURLConnection}.
 *
 * <p>Redirects are followed manually (the JDK's built-in follower silently drops on
 * cross-protocol hops, e.g. http→https), capped at {@code maxRedirects}. A redirect may only
 * lead to another {@code http(s)} URL and never from {@code https} down to {@code http};
 * non-2xx responses translate to {@link IOException}.
 *
 * <p>The server is not trusted to be reasonable: {@code timeoutMillis} bounds the whole
 * exchange (every hop plus the download — not just each individual read, which a slow drip
 * would keep satisfying forever), and a body larger than {@link #MAX_BODY_BYTES} is refused
 * instead of being buffered until the heap runs out.
 */
final class HttpUrlOpener implements UrlOpener {

    /** Upper bound for an imported document; far above any README fragment. */
    static final int MAX_BODY_BYTES = 16 * 1024 * 1024;

    private final int maxBodyBytes;

    HttpUrlOpener() {
        this(MAX_BODY_BYTES);
    }

    HttpUrlOpener(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    public Response open(URL url, int timeoutMillis, int maxRedirects) throws IOException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        URL current = url;
        int hops = 0;
        while (true) {
            HttpURLConnection conn = (HttpURLConnection) current.openConnection();
            int remaining = remainingMillis(deadline, url);
            conn.setConnectTimeout(remaining);
            conn.setReadTimeout(remaining);
            conn.setInstanceFollowRedirects(false); // we handle redirects manually for cross-protocol support
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) {
                    throw new IOException("import url=" + url + ": redirect without Location header");
                }
                if (++hops > maxRedirects) {
                    throw new IOException("import url=" + url + ": too many redirects (>" + maxRedirects + ")");
                }
                current = redirectTarget(url, current, location);
                continue;
            }
            if (code < 200 || code >= 300) {
                conn.disconnect();
                throw new IOException("import url=" + url + ": HTTP " + code);
            }
            try (InputStream in = conn.getInputStream()) {
                return new Response(readBody(in, deadline, url));
            } finally {
                conn.disconnect();
            }
        }
    }

    /**
     * Resolves a {@code Location} header against the URL that produced it. Anything but
     * {@code http(s)} is refused — the connection below is cast to {@link HttpURLConnection},
     * and a server has no business sending a client to {@code file:} or {@code jar:} — and so
     * is a downgrade from {@code https} to {@code http}, which would silently drop the
     * transport protection the template author asked for.
     */
    static URL redirectTarget(URL original, URL from, String location) throws IOException {
        URL target;
        try {
            target = new URL(from, location);
        } catch (IOException e) {
            throw new IOException("import url=" + original + ": malformed redirect target '" + location + "'", e);
        }
        String protocol = target.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IOException("import url=" + original + ": redirect to a non-http(s) URL refused (" + target + ")");
        }
        if ("https".equalsIgnoreCase(from.getProtocol()) && "http".equalsIgnoreCase(protocol)) {
            throw new IOException("import url=" + original + ": redirect from https to http refused (" + target + ")");
        }
        return target;
    }

    private byte[] readBody(InputStream in, long deadline, URL url) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out.size() + read > maxBodyBytes) {
                throw new IOException("import url=" + url + ": response is larger than " + maxBodyBytes + " bytes");
            }
            out.write(buffer, 0, read);
            // Each read is individually within the socket timeout; this bounds their sum, so a
            // server that drips a byte at a time cannot hold the build forever.
            remainingMillis(deadline, url);
        }
        return out.toByteArray();
    }

    private static int remainingMillis(long deadline, URL url) throws IOException {
        long remaining = (deadline - System.nanoTime()) / 1_000_000L;
        if (remaining <= 0) {
            throw new IOException("import url=" + url + ": timed out");
        }
        return (int) Math.min(remaining, Integer.MAX_VALUE);
    }
}
