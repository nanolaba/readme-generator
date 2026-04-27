package com.nanolaba.nrg.widgets;

import java.io.IOException;
import java.net.URL;

/**
 * Test seam over the network call used by {@link RemoteFetcher}: production runs use
 * {@link HttpUrlOpener} with {@code java.net.HttpURLConnection}; tests substitute their
 * own implementation to avoid hitting the network.
 *
 * <p>Implementations must follow up to {@code maxRedirects} HTTP redirects, surface
 * non-2xx responses as {@link IOException}, and apply {@code timeoutMillis} to both
 * connect and read timeouts.
 */
interface UrlOpener {

    Response open(URL url, int timeoutMillis, int maxRedirects) throws IOException;

    final class Response {

        private final byte[] body;

        Response(byte[] body) {
            this.body = body;
        }

        byte[] getBody() {
            return body;
        }
    }
}
