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
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void testAnInvalidGlobIsRejectedWithThePatternAsWritten() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PathPatternMatcher.compile(Arrays.asList("*.md", "docs/[")));
        assertEquals("pattern 'docs/[': Missing ']", e.getMessage());
        assertInstanceOf(PatternSyntaxException.class, e.getCause());
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

    @Test
    void testAbsolutePatternsAreUsedAsWritten() {
        assertEquals("/abs/*.md", PathPatternMatcher.absolutePatternString("/abs/*.md"));
        assertEquals("//server/share/*.md", PathPatternMatcher.absolutePatternString("\\\\server\\share\\*.md"));
        assertEquals("C:/docs/*.md", PathPatternMatcher.absolutePatternString("C:\\docs\\*.md"));
        assertEquals("C:/", PathPatternMatcher.absolutePatternString("C:/"));
    }

    @Test
    void testPatternsThatOnlyLookAbsoluteArePrefixedWithCwd() {
        String cwd = Paths.get("").toAbsolutePath().toString().replace('\\', '/');
        assertEquals(cwd + "/docs/*.md", PathPatternMatcher.absolutePatternString("docs/*.md"));
        assertEquals(cwd + "/C:docs", PathPatternMatcher.absolutePatternString("C:docs"));
        assertEquals(cwd + "/1:/docs", PathPatternMatcher.absolutePatternString("1:/docs"));
        assertEquals(cwd + "/ab/docs", PathPatternMatcher.absolutePatternString("ab/docs"));
        assertEquals(cwd + "/C:", PathPatternMatcher.absolutePatternString("C:"));
    }

    @Test
    void testGlobCharacters() {
        assertFalse(PathPatternMatcher.hasGlobChars("docs/README.md"));
        for (String glob : Arrays.asList("*.md", "a?.md", "[ab].md", "{a,b}.md")) {
            assertTrue(PathPatternMatcher.hasGlobChars(glob), glob);
        }
        assertEquals(-1, PathPatternMatcher.indexOfGlobChar("docs/README.md"));
        assertEquals(0, PathPatternMatcher.indexOfGlobChar("*/a?"));
        assertEquals(5, PathPatternMatcher.indexOfGlobChar("docs/{a,b}/*.md"));
    }

    @Test
    void testEachGlobstarIsElidedOnceInItsOwnVariant() {
        assertEquals(Collections.singletonList("/docs/*.md"), PathPatternMatcher.globstarVariants("/docs/*.md"));
        assertEquals(Arrays.asList("/a/**/b/**/c.md", "/a/b/**/c.md", "/a/**/b/c.md"),
                PathPatternMatcher.globstarVariants("/a/**/b/**/c.md"));
    }
}
