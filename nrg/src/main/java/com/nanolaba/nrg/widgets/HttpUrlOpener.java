package com.nanolaba.nrg.widgets;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class HttpUrlOpener implements UrlOpener {

    @Override
    public Response open(URL url, int timeoutMillis, int maxRedirects) throws IOException {
        URL current = url;
        int hops = 0;
        while (true) {
            HttpURLConnection conn = (HttpURLConnection) current.openConnection();
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);
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
                current = new URL(current, location);
                continue;
            }
            if (code < 200 || code >= 300) {
                conn.disconnect();
                throw new IOException("import url=" + url + ": HTTP " + code);
            }
            try (InputStream in = conn.getInputStream()) {
                return new Response(IOUtils.toByteArray(in));
            } finally {
                conn.disconnect();
            }
        }
    }
}
