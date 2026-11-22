package com.nanolaba.nrg.core;

import com.nanolaba.logging.MemoryLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.nanolaba.logging.LogEntry.LogEntryLevel.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void testAnInvalidGlobIsRejectedRatherThanReportedAsMatchingNothing() throws IOException {
        touch("README.src.md");
        String pattern = tempDir + "/{README,x.src.md";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SourceFileResolver.resolve(Collections.singletonList(pattern)));
        assertEquals("pattern '" + pattern + "': Missing '}", e.getMessage());
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
    void testMatchesAreSortedByAbsolutePath() throws IOException {
        // Upper case sorts before lower case, and "a.src.md" before "a/z.src.md": an order no
        // common file system lists a directory in, so the result must have been sorted.
        List<String> sorted = Arrays.asList("B.src.md", "D.src.md", "F.src.md", "H.src.md",
                "a.src.md", "a/z.src.md", "c.src.md", "e.src.md", "g.src.md");
        List<String> creationOrder = new ArrayList<>(sorted);
        Collections.reverse(creationOrder);
        for (String name : creationOrder) {
            touch(name);
        }
        List<File> expected = new ArrayList<>();
        for (String name : sorted) {
            expected.add(tempDir.resolve(name).toFile().getCanonicalFile());
        }

        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.toString() + "/**/*.src.md"));

        assertEquals(expected, r.getFiles());
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
        Files.createDirectories(tempDir.resolve("docs/sub"));
        touch("docs/a.src.md");
        SourceFileResolver.Result r = SourceFileResolver.resolve(
                Collections.singletonList(tempDir.toString() + "/docs/**"));
        for (File f : r.getFiles()) {
            assertTrue(f.isFile(), "expected only files, got " + f);
        }
        assertEquals(1, r.getFiles().size());
    }

    @Test
    void testBaseDirectoryIsThePrefixBeforeTheFirstGlob() {
        Path cwd = Paths.get(".").toAbsolutePath();
        assertEquals(cwd, SourceFileResolver.baseDirectory("*.src.md"));
        assertEquals(cwd, SourceFileResolver.baseDirectory("*/docs/*.src.md"));
        assertEquals(cwd, SourceFileResolver.baseDirectory("*/a?/b.src.md"));
        assertEquals(Paths.get("docs").toAbsolutePath(), SourceFileResolver.baseDirectory("docs/**/*.src.md"));
        assertEquals(Paths.get("docs").toAbsolutePath(), SourceFileResolver.baseDirectory("docs\\*.src.md"));
        assertEquals(tempDir.resolve("sub"), SourceFileResolver.baseDirectory(tempDir + "/sub/a*/*.src.md"));
        assertEquals(Paths.get("/").toAbsolutePath(), SourceFileResolver.baseDirectory("/*.src.md"));
    }

    @Test
    void testUnreadableDirectoryIsReportedAsAnEmptyPattern() throws IOException {
        Path locked = Files.createDirectories(tempDir.resolve("locked"));
        touch("locked/a.src.md");
        String pattern = locked + "/*.src.md";
        try (ReadDenial ignored = ReadDenial.of(locked); MemoryLogger log = MemoryLogger.capture()) {
            SourceFileResolver.Result r = SourceFileResolver.resolve(Collections.singletonList(pattern));

            assertTrue(r.getFiles().isEmpty());
            assertEquals(Collections.singletonList(pattern), r.getEmptyPatterns());
            log.assertLogged(WARN, "Failed to walk");
        }
    }
}
