package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceFileResolverTest {

    @TempDir
    Path tempDir;

    private Path touch(String relative) throws IOException {
        Path p = tempDir.resolve(relative);
        Files.createDirectories(p.getParent() == null ? tempDir : p.getParent());
        Files.write(p, new byte[0]);
        return p;
    }

    @Test
    void testLiteralPathReturnedAsIs() throws IOException {
        Path a = touch("README.src.md");
        SourceFileResolver.Result r = SourceFileResolver.resolve(Collections.singletonList(a.toString()));
        assertEquals(1, r.getFiles().size());
        assertEquals(a.toFile().getCanonicalFile(), r.getFiles().get(0).getCanonicalFile());
        assertTrue(r.getEmptyPatterns().isEmpty());
    }

    @Test
    void testRecursiveGlobMatchesNestedFiles() throws IOException {
        touch("docs/a.src.md");
        touch("docs/sub/b.src.md");
        touch("docs/c.txt");
        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.toString() + "/docs/**/*.src.md"));
        assertEquals(2, r.getFiles().size());
        // sorted lexicographically by absolute path
        assertTrue(r.getFiles().get(0).getName().equals("a.src.md"));
        assertTrue(r.getFiles().get(1).getName().equals("b.src.md"));
    }

    @Test
    void testSingleStarDoesNotMatchNestedDirs() throws IOException {
        touch("docs/a.src.md");
        touch("docs/sub/b.src.md");
        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.toString() + "/docs/*.src.md"));
        assertEquals(1, r.getFiles().size());
        assertEquals("a.src.md", r.getFiles().get(0).getName());
    }

    @Test
    void testDeduplicatesAcrossPatterns() throws IOException {
        Path a = touch("README.src.md");
        SourceFileResolver.Result r = SourceFileResolver.resolve(Arrays.asList(
                a.toString(),
                tempDir.toString() + "/*.src.md"));
        assertEquals(1, r.getFiles().size());
    }

    @Test
    void testFirstOccurrenceOrderPreservedAcrossPatterns() throws IOException {
        Path a = touch("a.src.md");
        Path b = touch("b.src.md");
        SourceFileResolver.Result r = SourceFileResolver.resolve(Arrays.asList(
                b.toString(),
                a.toString()));
        assertEquals(b.toFile().getCanonicalFile(), r.getFiles().get(0).getCanonicalFile());
        assertEquals(a.toFile().getCanonicalFile(), r.getFiles().get(1).getCanonicalFile());
    }

    @Test
    void testNoMatchRecordsEmptyPattern() throws IOException {
        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.toString() + "/missing/**/*.src.md"));
        assertTrue(r.getFiles().isEmpty());
        assertEquals(1, r.getEmptyPatterns().size());
    }

    @Test
    void testLiteralMissingFileRecordedAsEmpty() throws IOException {
        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.resolve("does-not-exist.src.md").toString()));
        assertTrue(r.getFiles().isEmpty());
        assertEquals(1, r.getEmptyPatterns().size());
    }

    @Test
    void testDirectoriesAreSkipped() throws IOException {
        Files.createDirectories(tempDir.resolve("docs"));
        touch("docs/a.src.md");
        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.toString() + "/docs/**"));
        for (File f : r.getFiles()) {
            assertTrue(f.isFile(), "expected only files, got " + f);
        }
        assertEquals(1, r.getFiles().size());
    }
}
