package com.nanolaba.nrg.widgets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link HttpUrlOpener} against a loopback server: the remote end is not trusted
 * to send sane redirects, a sane amount of data, or to finish at all.
 *
 * <p>How each connection is configured is checked against {@link StubServer} instead, which
 * answers from memory and keeps every connection the opener made; together with a fixed
 * clock it pins the timeouts down to the millisecond.
 */
class HttpUrlOpenerTest {

    /** A {@code System.nanoTime()} that never moves: the full timeout is always left. */
    private static final LongSupplier FROZEN_CLOCK = () -> 0L;

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
    void everyHopGetsTheRemainingTimeAndNoAutomaticRedirectsAndIsClosed() throws IOException {
        StubServer stub = new StubServer()
                .redirect("/start", "/ok")
                .ok("/ok", "hello");

        byte[] body = new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, FROZEN_CLOCK)
                .open(stub.url("/start"), 5000, 5).getBody();

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), body);
        assertEquals(2, stub.opened.size());
        for (StubConnection conn : stub.opened) {
            assertEquals(5000, conn.getConnectTimeout(), conn.getURL().toString());
            assertEquals(5000, conn.getReadTimeout(), conn.getURL().toString());
            assertFalse(conn.getInstanceFollowRedirects(), conn.getURL().toString());
            assertTrue(conn.disconnected, conn.getURL().toString());
        }
    }

    @Test
    void timeoutsShrinkAsTheDeadlineApproaches() throws IOException {
        AtomicLong now = new AtomicLong();
        StubServer stub = new StubServer()
                .redirect("/start", "/ok")
                .ok("/ok", "hello");
        // Every look at the clock takes 10 ms.
        HttpUrlOpener opener = new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, () -> now.getAndAdd(10_000_000L));

        opener.open(stub.url("/start"), 1000, 5);

        assertEquals(990, stub.opened.get(0).getReadTimeout());
        assertEquals(980, stub.opened.get(1).getReadTimeout());
    }

    @Test
    void lessThanAMillisecondLeftIsATimeoutNotAnUnlimitedWait() throws MalformedURLException {
        // HttpURLConnection reads a timeout of 0 as "wait forever".
        StubServer stub = new StubServer().ok("/ok", "hello");

        IOException e = assertThrows(IOException.class,
                () -> new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, FROZEN_CLOCK).open(stub.url("/ok"), 0, 5));
        assertEquals("import url=" + stub.url("/ok") + ": timed out", e.getMessage());
    }

    @Test
    void nonSuccessStatusIsAnErrorAndClosesTheConnection() throws MalformedURLException {
        StubServer stub = new StubServer().status("/missing", 404);

        IOException e = assertThrows(IOException.class,
                () -> new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, FROZEN_CLOCK).open(stub.url("/missing"), 5000, 5));
        assertEquals("import url=" + stub.url("/missing") + ": HTTP 404", e.getMessage());
        assertTrue(stub.opened.get(0).disconnected);
    }

    @Test
    void anyStatusOutside2xxAnd3xxIsAnError() throws MalformedURLException {
        StubServer stub = new StubServer()
                .status("/info", 100)
                .status("/client", 400)
                .status("/server", 503);
        HttpUrlOpener opener = new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, FROZEN_CLOCK);

        for (String path : new String[]{"/info", "/client", "/server"}) {
            IOException e = assertThrows(IOException.class, () -> opener.open(stub.url(path), 5000, 5));
            assertTrue(e.getMessage().contains(": HTTP "), e.getMessage());
        }
    }

    @Test
    void redirectLimitAllowsExactlyThatManyHops() throws IOException {
        StubServer stub = new StubServer()
                .redirect("/one", "/two")
                .redirect("/two", "/ok")
                .ok("/ok", "hello");
        HttpUrlOpener opener = new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, FROZEN_CLOCK);

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), opener.open(stub.url("/one"), 5000, 2).getBody());

        IOException e = assertThrows(IOException.class, () -> opener.open(stub.url("/one"), 5000, 1));
        assertTrue(e.getMessage().endsWith(": too many redirects (>1)"), e.getMessage());
    }

    @Test
    void redirectWithoutLocationIsAnErrorAndClosesTheConnection() throws MalformedURLException {
        StubServer stub = new StubServer().status("/nowhere", 302);

        IOException e = assertThrows(IOException.class,
                () -> new HttpUrlOpener(HttpUrlOpener.MAX_BODY_BYTES, FROZEN_CLOCK).open(stub.url("/nowhere"), 5000, 5));
        assertTrue(e.getMessage().endsWith(": redirect without Location header"), e.getMessage());
        assertTrue(stub.opened.get(0).disconnected);
    }

    @Test
    void bodyOfExactlyTheLimitIsAccepted() throws IOException {
        StubServer stub = new StubServer().ok("/ok", "hello");

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
                new HttpUrlOpener(5, FROZEN_CLOCK).open(stub.url("/ok"), 5000, 5).getBody());
        IOException e = assertThrows(IOException.class,
                () -> new HttpUrlOpener(4, FROZEN_CLOCK).open(stub.url("/ok"), 5000, 5));
        assertTrue(e.getMessage().endsWith(": response is larger than 4 bytes"), e.getMessage());
        assertTrue(stub.opened.get(1).disconnected, "a refused download still closes the connection");
    }

    @Test
    void timeoutBoundsTheWholeDownloadNotEachRead() {
        // Every read of the drip succeeds well within the timeout; only their sum exceeds it.
        IOException e = assertTimeoutPreemptively(Duration.ofSeconds(8), () -> assertThrows(IOException.class,
                () -> new HttpUrlOpener().open(new URL(base + "/drip"), 400, 5)));
        assertTrue(e.getMessage().contains("timed out"), e.getMessage());
    }

    /** An in-memory HTTP response that remembers how the opener configured and closed it. */
    private static final class StubConnection extends HttpURLConnection {

        private final int code;
        private final String location;
        private final byte[] body;
        boolean disconnected;

        StubConnection(URL url, int code, String location, byte[] body) {
            super(url);
            this.code = code;
            this.location = location;
            this.body = body;
        }

        @Override
        public int getResponseCode() {
            return code;
        }

        @Override
        public String getHeaderField(String name) {
            return "Location".equalsIgnoreCase(name) ? location : null;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }
    }

    /**
     * Serves {@link StubConnection}s by path. The URLs it hands out carry it as their handler,
     * and a relative redirect target inherits the handler of the URL it is resolved against.
     */
    private static final class StubServer extends URLStreamHandler {

        private final Map<String, Function<URL, StubConnection>> routes = new HashMap<>();
        final List<StubConnection> opened = new ArrayList<>();

        StubServer ok(String path, String body) {
            routes.put(path, u -> new StubConnection(u, 200, null, body.getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        StubServer redirect(String path, String location) {
            routes.put(path, u -> new StubConnection(u, 302, location, new byte[0]));
            return this;
        }

        StubServer status(String path, int code) {
            routes.put(path, u -> new StubConnection(u, code, null, new byte[0]));
            return this;
        }

        URL url(String path) throws MalformedURLException {
            return new URL("http", "stub.invalid", -1, path, this);
        }

        @Override
        protected URLConnection openConnection(URL u) {
            StubConnection conn = routes.get(u.getPath()).apply(u);
            opened.add(conn);
            return conn;
        }
    }
}
