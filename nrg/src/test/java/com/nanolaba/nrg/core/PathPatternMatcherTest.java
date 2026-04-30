package com.nanolaba.nrg.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathPatternMatcherTest {

    @TempDir
    Path tempDir;

    private File touch(String relative) throws IOException {
        Path p = tempDir.resolve(relative);
        Files.createDirectories(p.getParent() == null ? tempDir : p.getParent());
        Files.write(p, new byte[0]);
        return p.toFile();
    }

    @Test
    void testNullInputProducesUnconstrainedMatcher() {
        PathPatternMatcher m = PathPatternMatcher.compile(null);
        assertTrue(m.isUnconstrained());
        assertTrue(m.matches(new File("anything.md")));
        assertTrue(m.getPatterns().isEmpty());
    }

    @Test
    void testEmptyInputProducesUnconstrainedMatcher() {
        PathPatternMatcher m = PathPatternMatcher.compile(Collections.<String>emptyList());
        assertTrue(m.isUnconstrained());
        assertTrue(m.matches(new File("anything.md")));
    }

    @Test
    void testEmptyAndNullStringsAreSilentlyDropped() {
        PathPatternMatcher m = PathPatternMatcher.compile(Arrays.asList("", null));
        assertTrue(m.isUnconstrained(),
                "empty/null entries should drop and leave matcher unconstrained");
    }

    @Test
    void testLiteralAbsolutePathMatchesItself() throws IOException {
        File f = touch("README.md");
        PathPatternMatcher m = PathPatternMatcher.compile(
                Collections.singletonList(f.getAbsolutePath()));
        assertTrue(m.matches(f));
        assertFalse(m.matches(touch("OTHER.md")));
    }

    @Test
    void testStarMatchesSingleSegment() throws IOException {
        File a = touch("README.md");
        File b = touch("README.ru.md");
        File c = touch("docs/README.md");
        PathPatternMatcher m = PathPatternMatcher.compile(
                Collections.singletonList(tempDir.toString() + "/README*.md"));
        assertTrue(m.matches(a));
        assertTrue(m.matches(b));
        assertFalse(m.matches(c), "single-star should not cross directory boundaries");
    }

    @Test
    void testGlobstarMatchesZeroOrMoreDirectories() throws IOException {
        File direct = touch("docs/README.md");
        File nested = touch("docs/sub/README.md");
        PathPatternMatcher m = PathPatternMatcher.compile(
                Collections.singletonList(tempDir.toString() + "/docs/**/README.md"));
        assertTrue(m.matches(direct), "**/README.md must match docs/README.md (zero dirs)");
        assertTrue(m.matches(nested), "**/README.md must match docs/sub/README.md");
    }

    @Test
    void testRelativePatternPrefixedWithCwd() throws IOException {
        // Verifies that a relative pattern is resolved against the *current* working directory.
        // Drops a marker file directly into the cwd, compiles a relative pattern of just its
        // filename, then matches the file's absolute path against it.
        Path cwd = Paths.get("").toAbsolutePath();
        Path relativeMarker = cwd.resolve("__nrg-pattern-test-" + System.nanoTime() + ".tmp");
        try {
            Files.write(relativeMarker, new byte[0]);
            PathPatternMatcher m = PathPatternMatcher.compile(
                    Collections.singletonList(relativeMarker.getFileName().toString()));
            assertTrue(m.matches(relativeMarker.toFile()));
        } finally {
            Files.deleteIfExists(relativeMarker);
        }
    }

    @Test
    void testMultiplePatternsAreOred() throws IOException {
        File md = touch("README.md");
        File ru = touch("README.ru.md");
        File de = touch("README.de.md");
        PathPatternMatcher m = PathPatternMatcher.compile(Arrays.asList(
                md.getAbsolutePath(), ru.getAbsolutePath()));
        assertTrue(m.matches(md));
        assertTrue(m.matches(ru));
        assertFalse(m.matches(de));
    }

    @Test
    void testGetPatternsRoundTripsInputOrderWithoutNullsOrEmpties() {
        PathPatternMatcher m = PathPatternMatcher.compile(
                Arrays.asList("a.md", "", null, "b.md"));
        assertEquals(Arrays.asList("a.md", "b.md"), m.getPatterns());
    }
}
