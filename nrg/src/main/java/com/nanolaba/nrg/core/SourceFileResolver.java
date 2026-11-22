package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.sugar.Code;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Expands a list of CLI-supplied path patterns into a concrete, deduplicated list of source files.
 *
 * <p>Each input is treated as a {@code glob:} pattern (per
 * {@link java.nio.file.FileSystem#getPathMatcher(String)}) so the same syntax behaves identically
 * across Windows, Linux, and macOS without relying on shell expansion. A pattern with no glob characters is resolved as a literal file path. The longest
 * leading non-glob path segment is used as the walk root; if the root does not exist or no file
 * underneath it matches, the pattern is reported in {@link Result#getEmptyPatterns()} so callers
 * can warn — emptiness is never an exception. Matches are returned in lexicographic order within
 * each pattern, deduplicated by canonical path across all patterns, with first-occurrence order
 * preserved across patterns.
 *
 * <p>Path comparisons normalise backslashes to forward slashes before matching so Windows
 * absolute paths (e.g. {@code C:\foo\bar}) work with the same {@code **} / {@code *} semantics
 * the JDK applies on POSIX filesystems — without this, {@link PathMatcher} treats {@code \} as
 * an escape character rather than a separator on Windows.
 */
public final class SourceFileResolver {

    private SourceFileResolver() {/**/}

    /**
     * @throws IllegalArgumentException when a pattern is not a valid glob (an unclosed <code>[</code>
     *                                  or <code>{</code>) — a mistake in the arguments, unlike a
     *                                  pattern that matches nothing
     */
    public static Result resolve(List<String> patterns) {
        Set<File> dedup = new LinkedHashSet<>();
        List<String> empty = new ArrayList<>();
        for (String raw : patterns) {
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            List<File> matches = expand(raw);
            if (matches.isEmpty()) {
                empty.add(raw);
                continue;
            }
            for (File f : matches) {
                dedup.add(Code.run(() -> f.getCanonicalFile()));
            }
        }
        return new Result(new ArrayList<>(dedup), empty);
    }

    private static List<File> expand(String pattern) {
        if (!PathPatternMatcher.hasGlobChars(pattern)) {
            File f = new File(pattern);
            return f.isFile() ? Collections.singletonList(f) : Collections.emptyList();
        }
        Path base = baseDirectory(pattern);
        if (!Files.isDirectory(base)) {
            return Collections.emptyList();
        }
        // The matchers belong to the file system that is walked, not to the default one.
        List<PathMatcher> matchers = PathPatternMatcher.buildMatchers(base.getFileSystem(), pattern);
        List<File> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> matchesAny(matchers, p))
                    .map(Path::toFile)
                    .forEach(hits::add);
        } catch (IOException e) {
            LOG.warn("Failed to walk {} for pattern {}: {}", base, pattern, e.getMessage());
            return Collections.emptyList();
        }
        Collections.sort(hits, (a, b) -> a.getAbsolutePath().compareTo(b.getAbsolutePath()));
        return hits;
    }

    private static boolean matchesAny(List<PathMatcher> matchers, Path p) {
        Path normalised = Paths.get(p.toAbsolutePath().toString().replace('\\', '/'));
        for (PathMatcher m : matchers) {
            if (m.matches(normalised)) return true;
        }
        return false;
    }

    /**
     * Walks back from the first glob character to the previous path separator and returns the
     * absolute directory at that prefix. With no separator before the glob ({@code *.md}), walks
     * from the current working directory; when the only separator before it is the leading one
     * ({@code /*.md}), from the filesystem root. Always returned absolute so {@link PathMatcher}
     * can match against absolute paths produced by {@link Files#walk}.
     *
     * <p>{@code pattern} must contain a glob character ({@link PathPatternMatcher#hasGlobChars}).
     */
    static Path baseDirectory(String pattern) {
        String normalized = pattern.replace('\\', '/');
        String prefix = normalized.substring(0, PathPatternMatcher.indexOfGlobChar(normalized));
        int lastSlash = prefix.lastIndexOf('/');
        String basePart = lastSlash < 0 ? "." : prefix.substring(0, lastSlash);
        if (basePart.isEmpty()) basePart = "/";
        return Paths.get(basePart).toAbsolutePath();
    }

    /**
     * Outcome of {@link #resolve(List)}: the resolved files in deterministic order, plus the raw
     * patterns from the input that produced zero matches (so the CLI can warn about each one).
     */
    public static final class Result {
        private final List<File> files;
        private final List<String> emptyPatterns;

        Result(List<File> files, List<String> emptyPatterns) {
            this.files = files;
            this.emptyPatterns = emptyPatterns;
        }

        public List<File> getFiles() {
            return files;
        }

        public List<String> getEmptyPatterns() {
            return emptyPatterns;
        }
    }
}
