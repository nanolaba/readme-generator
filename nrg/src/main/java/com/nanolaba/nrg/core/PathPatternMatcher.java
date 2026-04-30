package com.nanolaba.nrg.core;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Matches a {@link File} against a list of {@code glob:}-style path patterns.
 *
 * <p>Pattern syntax follows {@link java.nio.file.FileSystem#getPathMatcher(String)} with one
 * extension borrowed from bash globstar: {@code **\/} matches zero or more directory
 * components, so {@code docs/**\/README.md} matches both {@code docs/README.md} and
 * {@code docs/sub/README.md}. Relative patterns are resolved against the current working
 * directory; both file and pattern paths are normalised to forward slashes before matching
 * so Windows backslashes do not get treated as escape characters by {@link PathMatcher}.
 *
 * <p>{@link #compile(List)} with {@code null} or an empty list returns an <em>unconstrained</em>
 * matcher whose {@link #matches(File)} always returns {@code true} — useful for callers that
 * want to flow through unchanged when no filter was supplied.
 */
public final class PathPatternMatcher {

    private static final char[] GLOB_CHARS = {'*', '?', '[', '{'};

    private final List<String> patterns;
    private final List<List<PathMatcher>> matchersPerPattern;

    private PathPatternMatcher(List<String> patterns, List<List<PathMatcher>> matchersPerPattern) {
        this.patterns = patterns;
        this.matchersPerPattern = matchersPerPattern;
    }

    /**
     * Compiles the supplied raw patterns into an immutable matcher. Null or blank entries are
     * silently dropped; a {@code null} or empty list yields an {@linkplain #isUnconstrained()
     * unconstrained} matcher that accepts every file.
     */
    public static PathPatternMatcher compile(List<String> rawPatterns) {
        if (rawPatterns == null || rawPatterns.isEmpty()) {
            return new PathPatternMatcher(Collections.<String>emptyList(),
                    Collections.<List<PathMatcher>>emptyList());
        }
        List<String> kept = new ArrayList<>();
        List<List<PathMatcher>> matchers = new ArrayList<>();
        for (String raw : rawPatterns) {
            if (raw == null || raw.isEmpty()) continue;
            kept.add(raw);
            matchers.add(buildMatchersFor(raw));
        }
        return new PathPatternMatcher(kept, matchers);
    }

    public boolean isUnconstrained() {
        return patterns.isEmpty();
    }

    public List<String> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    /**
     * Returns {@code true} if the file matches any compiled pattern, or if this matcher is
     * unconstrained. The file's absolute path is normalised to forward slashes before matching
     * so the same pattern works on Windows and POSIX.
     */
    public boolean matches(File file) {
        if (isUnconstrained()) {
            return true;
        }
        Path normalised = Paths.get(file.getAbsolutePath().replace('\\', '/'));
        for (List<PathMatcher> patternMatchers : matchersPerPattern) {
            for (PathMatcher m : patternMatchers) {
                if (m.matches(normalised)) return true;
            }
        }
        return false;
    }

    /**
     * Builds the {@link PathMatcher}(s) for a single pattern. {@code **\/} is treated as a
     * "zero or more directory components" wildcard (bash globstar semantics): besides the
     * literal pattern, an additional matcher is generated for each {@code **\/} occurrence with
     * the segment elided, so {@code docs/**\/*.md} matches both {@code docs/a.md} (zero
     * intermediate dirs) and {@code docs/sub/b.md} (one or more).
     */
    private static List<PathMatcher> buildMatchersFor(String pattern) {
        String absolutePattern = absolutePatternString(pattern);
        List<String> variants = new ArrayList<>();
        variants.add(absolutePattern);
        int from = 0;
        while (true) {
            int idx = absolutePattern.indexOf("**/", from);
            if (idx < 0) break;
            variants.add(absolutePattern.substring(0, idx) + absolutePattern.substring(idx + 3));
            from = idx + 3;
        }
        List<PathMatcher> matchers = new ArrayList<>(variants.size());
        for (String v : variants) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + v));
        }
        return matchers;
    }

    /**
     * Returns the pattern as an absolute, forward-slash-normalised string suitable for feeding
     * to {@code glob:}. Cannot use {@link Paths#get(String, String...)} on the raw pattern
     * because Windows rejects {@code *}, {@code ?}, etc. as illegal path characters — instead,
     * if the pattern is already absolute (Unix root or Windows drive letter / UNC) it is used
     * verbatim, otherwise it is prefixed with the cwd.
     */
    static String absolutePatternString(String pattern) {
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

    static boolean hasGlobChars(String s) {
        for (char c : GLOB_CHARS) {
            if (s.indexOf(c) >= 0) return true;
        }
        return false;
    }
}
