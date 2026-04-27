package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.sugar.Code;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
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

    private static final char[] GLOB_CHARS = {'*', '?', '[', '{'};

    private SourceFileResolver() {/**/}

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
        if (!hasGlobChars(pattern)) {
            File f = new File(pattern);
            return f.isFile() ? Collections.singletonList(f) : Collections.emptyList();
        }
        Path base = baseDirectory(pattern);
        if (!Files.isDirectory(base)) {
            return Collections.emptyList();
        }
        List<PathMatcher> matchers = buildMatchers(base, pattern);
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
     * Builds the {@link PathMatcher}(s) to apply for a pattern. {@code **\/} is treated as a
     * "zero or more directory components" wildcard (bash globstar semantics): besides the
     * literal pattern, an additional matcher is generated for each {@code **\/} segment with
     * the segment removed, so {@code docs/**\/*.src.md} matches both {@code docs/a.src.md}
     * (zero intermediate dirs) and {@code docs/sub/b.src.md} (one or more).
     */
    private static List<PathMatcher> buildMatchers(Path base, String pattern) {
        String absolutePattern = absolutePatternString(pattern);
        List<String> variants = new ArrayList<>();
        variants.add(absolutePattern);
        // Generate one variant per "**/" position with the segment elided. Iterating a fixed
        // index avoids overlapping replacements when the pattern contains multiple "**/".
        int from = 0;
        while (true) {
            int idx = absolutePattern.indexOf("**/", from);
            if (idx < 0) break;
            variants.add(absolutePattern.substring(0, idx) + absolutePattern.substring(idx + 3));
            from = idx + 3;
        }
        List<PathMatcher> matchers = new ArrayList<>(variants.size());
        for (String v : variants) {
            matchers.add(base.getFileSystem().getPathMatcher("glob:" + v));
        }
        return matchers;
    }

    private static boolean hasGlobChars(String s) {
        for (char c : GLOB_CHARS) {
            if (s.indexOf(c) >= 0) return true;
        }
        return false;
    }

    /**
     * Returns the pattern as an absolute, forward-slash-normalised string suitable for feeding
     * to {@code glob:}. Cannot use {@link Paths#get(String, String...)} on the raw pattern
     * because Windows rejects {@code *}, {@code ?}, etc. as illegal path characters — instead,
     * if the pattern is already absolute (Unix root or Windows drive letter / UNC) it is used
     * verbatim, otherwise it is prefixed with the cwd.
     */
    private static String absolutePatternString(String pattern) {
        String normalized = pattern.replace('\\', '/');
        if (isAbsoluteNormalized(normalized)) {
            return normalized;
        }
        String cwd = Paths.get("").toAbsolutePath().toString().replace('\\', '/');
        if (cwd.endsWith("/")) {
            return cwd + normalized;
        }
        return cwd + "/" + normalized;
    }

    private static boolean isAbsoluteNormalized(String normalized) {
        if (normalized.startsWith("/")) {
            return true;
        }
        // Windows drive letter, e.g. "C:/foo".
        if (normalized.length() >= 3
                && Character.isLetter(normalized.charAt(0))
                && normalized.charAt(1) == ':'
                && normalized.charAt(2) == '/') {
            return true;
        }
        return false;
    }

    /**
     * Walks back from the first glob character to the previous path separator and returns the
     * absolute directory at that prefix. With no separator before the glob, walks from the
     * current working directory; with no characters at all before the glob, walks from the
     * filesystem root. Always returned absolute so {@link PathMatcher} can match against
     * absolute paths produced by {@link Files#walk}.
     */
    private static Path baseDirectory(String pattern) {
        String normalized = pattern.replace('\\', '/');
        int firstGlob = -1;
        for (char c : GLOB_CHARS) {
            int idx = normalized.indexOf(c);
            if (idx >= 0 && (firstGlob < 0 || idx < firstGlob)) firstGlob = idx;
        }
        String prefix = firstGlob < 0 ? normalized : normalized.substring(0, firstGlob);
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
