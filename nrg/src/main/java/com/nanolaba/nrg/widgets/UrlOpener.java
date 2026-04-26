package com.nanolaba.nrg.widgets;

import java.io.IOException;
import java.net.URL;

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
