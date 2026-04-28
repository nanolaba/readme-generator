package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineEndingsTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDetectLfOnly() {
        assertEquals(LineEndings.LF, LineEndings.detect("a\nb\nc\n"));
    }

    @Test
    public void testDetectCrlfOnly() {
        assertEquals(LineEndings.CRLF, LineEndings.detect("a\r\nb\r\nc\r\n"));
    }

    @Test
    public void testDetectMixedReturnsLf() {
        // A file with both styles is not predictable to round-trip in CRLF;
        // LF is the deterministic, Git-friendly choice.
        assertEquals(LineEndings.LF, LineEndings.detect("a\r\nb\nc\r\n"));
    }

    @Test
    public void testDetectEmptyOrNoNewlineReturnsNull() {
        assertNull(LineEndings.detect(""));
        assertNull(LineEndings.detect(null));
        assertNull(LineEndings.detect("no newline here"));
    }

    @Test
    public void testApplyToConvertsBetweenStyles() {
        assertEquals("a\nb\nc\n", LineEndings.applyTo("a\r\nb\r\nc\r\n", LineEndings.LF));
        assertEquals("a\r\nb\r\nc\r\n", LineEndings.applyTo("a\nb\nc\n", LineEndings.CRLF));
        // Mixed input normalises through LF first, so the output is fully consistent.
        assertEquals("a\r\nb\r\nc\r\n", LineEndings.applyTo("a\r\nb\nc\r\n", LineEndings.CRLF));
    }

    @Test
    public void testApplyToHandlesEmptyAndNull() {
        assertNull(LineEndings.applyTo(null, LineEndings.LF));
        assertEquals("", LineEndings.applyTo("", LineEndings.LF));
        // Null target ending is a no-op (caller has nothing to enforce).
        assertEquals("a\r\nb\n", LineEndings.applyTo("a\r\nb\n", null));
    }

    @Test
    public void testModeParse() {
        assertEquals(LineEndings.Mode.AUTO, LineEndings.Mode.parse(null));
        assertEquals(LineEndings.Mode.AUTO, LineEndings.Mode.parse(""));
        assertEquals(LineEndings.Mode.AUTO, LineEndings.Mode.parse("auto"));
        assertEquals(LineEndings.Mode.AUTO, LineEndings.Mode.parse("AUTO"));
        assertEquals(LineEndings.Mode.LF_ONLY, LineEndings.Mode.parse("lf"));
        assertEquals(LineEndings.Mode.LF_ONLY, LineEndings.Mode.parse("LF"));
        assertEquals(LineEndings.Mode.CRLF_ONLY, LineEndings.Mode.parse("crlf"));
        assertEquals(LineEndings.Mode.CRLF_ONLY, LineEndings.Mode.parse("CRLF"));
        assertThrows(IllegalArgumentException.class, () -> LineEndings.Mode.parse("cr"));
    }

    @Test
    public void testResolveAutoUsesExistingFileEnding() throws IOException {
        Path crlfFile = tempDir.resolve("crlf.md");
        Files.write(crlfFile, "a\r\nb\r\n".getBytes(StandardCharsets.UTF_8));
        Path lfFile = tempDir.resolve("lf.md");
        Files.write(lfFile, "a\nb\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(LineEndings.CRLF, LineEndings.resolve(LineEndings.Mode.AUTO, crlfFile.toFile(), StandardCharsets.UTF_8));
        assertEquals(LineEndings.LF, LineEndings.resolve(LineEndings.Mode.AUTO, lfFile.toFile(), StandardCharsets.UTF_8));
    }

    @Test
    public void testResolveAutoFallsBackToPlatformDefault() throws IOException {
        Path missing = tempDir.resolve("does-not-exist.md");
        assertEquals(System.lineSeparator(),
                LineEndings.resolve(LineEndings.Mode.AUTO, missing.toFile(), StandardCharsets.UTF_8));
    }

    @Test
    public void testResolveExplicitModeIgnoresExistingFile() throws IOException {
        Path crlfFile = tempDir.resolve("crlf.md");
        Files.write(crlfFile, "a\r\nb\r\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(LineEndings.LF,
                LineEndings.resolve(LineEndings.Mode.LF_ONLY, crlfFile.toFile(), StandardCharsets.UTF_8));
        assertEquals(LineEndings.CRLF,
                LineEndings.resolve(LineEndings.Mode.CRLF_ONLY, crlfFile.toFile(), StandardCharsets.UTF_8));
    }
}
