package com.nanolaba.nrg;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --init-wrapper}: the files it writes, their line endings, how the checksum is pinned
 * (given, fetched from the published {@code .sha256}, or left out), and the version checks.
 * The network is replaced by a stub fetcher; the real fetcher is exercised against a loopback
 * server and a stub connection.
 */
public class WrapperInstallerTest extends DefaultNRGTest {

    private static final String SHA = "9d5d58490703be4b575b489811baf4788c1a187e26d563b26d393f3591f52c62";

    @TempDir
    Path tempDir;

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    public void testWritesScriptsAndPropertiesWithFetchedChecksum() throws IOException {
        List<String> requested = new ArrayList<>();
        WrapperInstaller installer = new WrapperInstaller(url -> {
            requested.add(url);
            return SHA.toUpperCase() + " *readme-generator-1.3.zip\n";
        });

        assertEquals(NRG.EXIT_OK, installer.install(tempDir.toFile(), "1.3", null));

        assertEquals("https://github.com/nanolaba/readme-generator/releases/download/v1.3/readme-generator-1.3.zip.sha256",
                requested.get(0));
        String props = read(tempDir.resolve(".nrg/wrapper.properties"));
        assertTrue(props.contains("\nversion=1.3\n"), props);
        assertTrue(props.contains("\nsha256=" + SHA + "\n"), props);

        String sh = read(tempDir.resolve("nrgw"));
        assertTrue(sh.startsWith("#!/bin/sh\n"), "shell script must keep LF endings");
        assertFalse(sh.contains("\r"), "shell script must not contain CR");
        String cmd = read(tempDir.resolve("nrgw.cmd"));
        assertTrue(cmd.startsWith("@echo off\r\n"), "batch file must use CRLF endings");
        assertFalse(cmd.replace("\r\n", "").contains("\n"), "batch file must not contain bare LF");
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            assertTrue(new File(tempDir.toFile(), "nrgw").canExecute(), "nrgw must be executable");
        }
        assertTrue(getOutAndClear().contains("Created nrgw, nrgw.cmd and .nrg/wrapper.properties for NRG 1.3"));
        assertFalse(getErrAndClear().contains("could not mark"), "no warning when nrgw is marked executable");
    }

    @Test
    public void testFailureToMarkTheScriptExecutableIsAWarning() throws IOException {
        List<Object> calls = new ArrayList<>();
        WrapperInstaller installer = new WrapperInstaller(url -> SHA, (file, executable, ownerOnly) -> {
            calls.addAll(Arrays.asList(file.getName(), executable, ownerOnly));
            return false;
        });

        assertEquals(NRG.EXIT_OK, installer.install(tempDir.toFile(), "1.3", null));
        assertEquals(Arrays.asList("nrgw", true, false), calls, "executable for everyone, like chmod +x");
        String err = getErrAndClear();
        assertTrue(err.contains("WARN: could not mark " + new File(tempDir.toFile(), "nrgw").getPath()
                + " executable; run: chmod +x nrgw"), err);
        assertTrue(Files.exists(tempDir.resolve(".nrg/wrapper.properties")), "the files are written all the same");
    }

    private static HttpURLConnection connection(int status, String body) throws IOException {
        return new HttpURLConnection(new URL("https://example.com/readme-generator-1.3.zip.sha256")) {
            @Override
            public int getResponseCode() {
                return status;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void connect() {
            }

            @Override
            public void disconnect() {
            }

            @Override
            public boolean usingProxy() {
                return false;
            }
        };
    }

    @Test
    public void testReadReturnsTheBodyOfAnOkAnswerWithTimeoutsSet() throws IOException {
        HttpURLConnection ok = connection(200, SHA + "  readme-generator-1.3.zip\n");

        assertEquals(SHA + "  readme-generator-1.3.zip\n", WrapperInstaller.read(ok));
        assertEquals(15_000, ok.getConnectTimeout());
        assertEquals(15_000, ok.getReadTimeout());
        assertNull(WrapperInstaller.read(connection(404, "Not Found")));
    }

    @Test
    public void testReadStopsAfterFourKilobytes() throws IOException {
        char[] big = new char[10_000];
        Arrays.fill(big, 'a');

        assertEquals(4096, WrapperInstaller.read(connection(200, new String(big))).length());
    }

    @Test
    public void testFetchOverHttpAndFailures() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sha", exchange -> {
            byte[] body = SHA.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String base = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort();
            assertEquals(SHA, WrapperInstaller.fetch(base + "/sha"));
            assertNull(WrapperInstaller.fetch(base + "/missing"), "a 404 has no checksum");
        } finally {
            server.stop(0);
        }
        assertNull(WrapperInstaller.fetch(tempDir.toUri().toString()), "not an http(s) URL");
        assertNull(WrapperInstaller.fetch("no url at all"));
    }

    @Test
    public void testGivenChecksumIsUsedWithoutFetching() throws IOException {
        WrapperInstaller installer = new WrapperInstaller(url -> {
            throw new AssertionError("must not fetch: " + url);
        });

        assertEquals(NRG.EXIT_OK, installer.install(tempDir.toFile(), "1.3", SHA));
        assertTrue(read(tempDir.resolve(".nrg/wrapper.properties")).contains("sha256=" + SHA));
    }

    @Test
    public void testUnavailableChecksumIsLeftOutWithAWarning() throws IOException {
        WrapperInstaller installer = new WrapperInstaller(url -> null);

        assertEquals(NRG.EXIT_OK, installer.install(tempDir.toFile(), "1.3", null));
        assertFalse(read(tempDir.resolve(".nrg/wrapper.properties")).contains("\nsha256="));
        assertTrue(getErrAndClear().contains("written without sha256="));
    }

    @Test
    public void testDevelopmentVersionAndBadChecksumAreRejected() throws IOException {
        WrapperInstaller installer = new WrapperInstaller(url -> SHA);

        assertEquals(NRG.EXIT_USAGE, installer.install(tempDir.toFile(), "1.4-SNAPSHOT", null));
        assertTrue(getErrAndClear().contains("is not a release version"));
        assertEquals(NRG.EXIT_USAGE, installer.install(tempDir.toFile(), "1.3", "abc"));
        assertTrue(getErrAndClear().contains("is not a sha256 checksum"));
        assertFalse(Files.exists(tempDir.resolve("nrgw")), "nothing is written on a usage error");
    }

    @Test
    public void testWrapperOptionsRequireInitWrapper() {
        assertEquals(NRG.EXIT_USAGE, NRG.run("--wrapper-version", "1.3"));
        assertTrue(getErrAndClear().contains("require --init-wrapper"));
    }
}
