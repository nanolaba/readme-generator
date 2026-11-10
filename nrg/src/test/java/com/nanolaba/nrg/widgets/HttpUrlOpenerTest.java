package com.nanolaba.nrg.widgets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link HttpUrlOpener} against a loopback server: the remote end is not trusted
 * to send sane redirects, a sane amount of data, or to finish at all.
 */
class HttpUrlOpenerTest {

    private HttpServer server;
    private String base;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/to-ok", exchange -> {
            exchange.getResponseHeaders().add("Location", "/ok");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/to-file", exchange -> {
            exchange.getResponseHeaders().add("Location", "file:///etc/passwd");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/big", exchange -> {
            byte[] body = new byte[5000];
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/drip", exchange -> {
            exchange.sendResponseHeaders(200, 0); // chunked: no length announced
            try (OutputStream out = exchange.getResponseBody()) {
                for (int i = 0; i < 200; i++) {
                    out.write('x');
                    out.flush();
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException ignored) {
                // the client gave up, which is the point of the test
            }
        });
        server.start();
        base = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void readsBodyAndFollowsHttpRedirect() throws IOException {
        HttpUrlOpener opener = new HttpUrlOpener();

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
                opener.open(new URL(base + "/ok"), 5000, 5).getBody());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
                opener.open(new URL(base + "/to-ok"), 5000, 5).getBody());
    }

    @Test
    void redirectToANonHttpSchemeIsRefused() {
        // Used to blow up with a ClassCastException on the FileURLConnection.
        IOException e = assertThrows(IOException.class,
                () -> new HttpUrlOpener().open(new URL(base + "/to-file"), 5000, 5));
        assertTrue(e.getMessage().contains("non-http(s)"), e.getMessage());
    }

    @Test
    void downgradeFromHttpsToHttpIsRefused() throws IOException {
        URL original = new URL("https://example.com/a");

        IOException e = assertThrows(IOException.class,
                () -> HttpUrlOpener.redirectTarget(original, original, "http://example.com/a"));
        assertTrue(e.getMessage().contains("https to http"), e.getMessage());

        assertEquals("https://example.com/b",
                HttpUrlOpener.redirectTarget(original, original, "/b").toString());
        assertEquals("https://other.example/c",
                HttpUrlOpener.redirectTarget(new URL("http://example.com/a"), new URL("http://example.com/a"),
                        "https://other.example/c").toString());
    }

    @Test
    void bodyLargerThanTheLimitIsRefused() {
        IOException e = assertThrows(IOException.class,
                () -> new HttpUrlOpener(1000).open(new URL(base + "/big"), 5000, 5));
        assertTrue(e.getMessage().contains("larger than 1000 bytes"), e.getMessage());
    }

    @Test
    void timeoutBoundsTheWholeDownloadNotEachRead() {
        // Every read of the drip succeeds well within the timeout; only their sum exceeds it.
        IOException e = assertTimeoutPreemptively(Duration.ofSeconds(8), () -> assertThrows(IOException.class,
                () -> new HttpUrlOpener().open(new URL(base + "/drip"), 400, 5)));
        assertTrue(e.getMessage().contains("timed out"), e.getMessage());
    }
}
