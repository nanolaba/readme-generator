package com.nanolaba.nrg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --init-wrapper}: the files it writes, their line endings, how the checksum is pinned
 * (given, fetched from the published {@code .sha256}, or left out), and the version checks.
 * The network is replaced by a stub fetcher.
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
