package com.nanolaba.nrg.widgets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites ATX-style headings in imported content by adding a configurable level offset,
 * so e.g. an imported {@code # Title} becomes {@code ## Title} when nested under a parent
 * section ({@code heading-offset='+1'}).
 *
 * <p>Resulting levels are clamped to 1..6 (Markdown's max), and clamped occurrences are
 * counted and reported back to the caller for warning purposes. Code-fence content is
 * skipped — opening fences toggle a "do not rewrite" mode until the matching close fence
 * is seen, so inline {@code #} characters inside code blocks survive unchanged.
 *
 * <p>Mixed line endings are normalised to whichever separator dominates the input: CRLF
 * if any present, else LF if any newline present, else the platform default.
 */
final class ImportHeadingShifter {

    private static final Pattern ATX = Pattern.compile("^(#{1,6})(\\s.*|\\s*)$");
    private static final Pattern FENCE_OPEN = Pattern.compile("^(`{3,}|~{3,}).*$");

    private ImportHeadingShifter() {
    }

    static final class Result {
        final String content;
        final int clampedCount;
        final String firstClampedLine;

        Result(String content, int clampedCount, String firstClampedLine) {
            this.content = content;
            this.clampedCount = clampedCount;
            this.firstClampedLine = firstClampedLine;
        }
    }

    static Result shift(String content, int offset) {
        if (offset == 0 || content.isEmpty()) {
            return new Result(content, 0, null);
        }

        // Choose a single separator for the rejoin. Inputs with mixed line endings
        // are normalized to whichever separator dominates: \r\n if any present,
        // else \n if any newline present, else the platform default.
        String sep;
        if (content.contains("\r\n")) {
            sep = "\r\n";
        } else if (content.indexOf('\n') >= 0 || content.indexOf('\r') >= 0) {
            sep = "\n";
        } else {
            sep = System.lineSeparator();
        }

        String[] lines = content.split("\\R", -1);
        StringBuilder out = new StringBuilder(content.length() + 16);
        int clampedCount = 0;
        String firstClampedLine = null;

        boolean insideFence = false;
        char fenceChar = 0;
        int fenceLen = 0;

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append(sep);
            String line = lines[i];
            String rewritten = line;

            if (insideFence) {
                if (isFenceClose(line, fenceChar, fenceLen)) {
                    insideFence = false;
                    fenceChar = 0;
                    fenceLen = 0;
                }
                out.append(line);
                continue;
            }

            Matcher openM = FENCE_OPEN.matcher(line);
            if (openM.matches()) {
                insideFence = true;
                fenceChar = openM.group(1).charAt(0);
                fenceLen = openM.group(1).length();
                out.append(line);
                continue;
            }

            Matcher m = ATX.matcher(line);
            if (m.matches()) {
                int level = m.group(1).length();
                int target = level + offset;
                int newLevel = clamp(target, 1, 6);
                if (newLevel != target) {
                    clampedCount++;
                    if (firstClampedLine == null) {
                        firstClampedLine = line;
                    }
                }
                if (newLevel != level) {
                    rewritten = repeatHash(newLevel) + m.group(2);
                }
            }

            out.append(rewritten);
        }

        return new Result(out.toString(), clampedCount, firstClampedLine);
    }

    private static boolean isFenceClose(String line, char marker, int minLen) {
        int i = 0;
        int n = line.length();
        int count = 0;
        while (i < n && line.charAt(i) == marker) {
            count++;
            i++;
        }
        if (count < minLen) return false;
        while (i < n) {
            if (!Character.isWhitespace(line.charAt(i))) return false;
            i++;
        }
        return true;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private static String repeatHash(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append('#');
        return sb.toString();
    }
}
