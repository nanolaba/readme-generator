package com.nanolaba.nrg.widgets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the simple duration grammar accepted by the remote-import {@code timeout} and
 * {@code cache} parameters: a positive integer followed by a unit suffix
 * ({@code s}, {@code m}, {@code h}, {@code d}), or the literal {@code "none"} which
 * disables the feature.
 */
final class DurationParser {

    private static final Pattern GRAMMAR = Pattern.compile("^(\\d+)([smhd])$");
    static final long DISABLED = -1L;

    private DurationParser() {
    }

    /** Parses {@code <int><s|m|h|d>} or the literal "none". Returns {@link #DISABLED} for "none". */
    static long parseMillis(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("duration: empty value");
        }
        if ("none".equals(value)) {
            return DISABLED;
        }
        Matcher m = GRAMMAR.matcher(value);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "duration: invalid format '" + value + "' (expected <int>{s,m,h,d} or 'none')");
        }
        long n = Long.parseLong(m.group(1));
        if (n <= 0) {
            throw new IllegalArgumentException("duration: must be positive: " + value);
        }
        switch (m.group(2)) {
            case "s": return n * 1_000L;
            case "m": return n * 60_000L;
            case "h": return n * 3_600_000L;
            case "d": return n * 86_400_000L;
            default:  throw new IllegalStateException("unreachable");
        }
    }
}
