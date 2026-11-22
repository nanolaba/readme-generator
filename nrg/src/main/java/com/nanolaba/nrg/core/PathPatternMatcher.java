package com.nanolaba.nrg.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    private static final Pattern GLOBSTAR_SEGMENT = Pattern.compile("**/", Pattern.LITERAL);

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
     *
     * @throws IllegalArgumentException when a pattern is not a valid glob
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
            matchers.add(buildMatchers(FileSystems.getDefault(), raw));
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
     * The {@link PathMatcher}s of one pattern on {@code fileSystem}: one per {@linkplain
     * #globstarVariants globstar variant}. Shared with {@link SourceFileResolver}, which passes the
     * file system of the directory it walks; this class targets real-disk paths and uses the
     * default one.
     *
     * @throws IllegalArgumentException when the pattern is not a valid glob (an unclosed <code>[</code>
     *                                  or <code>{</code>); the message names the pattern as the user
     *                                  wrote it, not the absolute form the JDK was given
     */
    static List<PathMatcher> buildMatchers(FileSystem fileSystem, String pattern) {
        List<PathMatcher> matchers = new ArrayList<>();
        try {
            for (String variant : globstarVariants(absolutePatternString(pattern))) {
                matchers.add(fileSystem.getPathMatcher("glob:" + variant));
            }
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("pattern '" + pattern + "': " + e.getDescription(), e);
        }
        return matchers;
    }

    /**
     * The {@code glob:} strings one pattern stands for. {@code **\/} is treated as a "zero or more
     * directory components" wildcard (bash globstar semantics): besides the literal pattern, one
     * variant is generated for each {@code **\/} occurrence with that occurrence elided, so
     * {@code docs/**\/*.md} matches both {@code docs/a.md} (zero intermediate dirs) and
     * {@code docs/sub/b.md} (one or more).
     *
     * <p>Each variant elides exactly one occurrence, found left to right without overlaps;
     * shared with {@link SourceFileResolver}, which builds its matchers on the walked path's
     * file system instead of the default one.
     */
    static List<String> globstarVariants(String absolutePattern) {
        List<String> variants = new ArrayList<>();
        variants.add(absolutePattern);
        Matcher globstar = GLOBSTAR_SEGMENT.matcher(absolutePattern);
        while (globstar.find()) {
            variants.add(absolutePattern.substring(0, globstar.start()) + absolutePattern.substring(globstar.end()));
        }
        return variants;
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
        // Only a file system root ("/", "C:/") already ends with the separator.
        String cwd = Paths.get("").toAbsolutePath().toString().replace('\\', '/');
        return Strings.CS.removeEnd(cwd, "/") + "/" + normalized;
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
        return StringUtils.containsAny(s, GLOB_CHARS);
    }

    /** Index of the first glob character in {@code s}, or {@code -1} when it has none. */
    static int indexOfGlobChar(String s) {
        return StringUtils.indexOfAny(s, GLOB_CHARS);
    }
}
