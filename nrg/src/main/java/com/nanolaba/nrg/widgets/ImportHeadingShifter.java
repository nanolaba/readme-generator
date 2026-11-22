package com.nanolaba.nrg.widgets;

import org.apache.commons.lang3.StringUtils;

import java.util.function.UnaryOperator;
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
        return shift(content, offset, UnaryOperator.identity());
    }

    /**
     * @param fenceLineView maps a line to the form in which it reaches the generated file.
     *                      Imported content is sub-generator output, so a fence line may still
     *                      carry an NRG marker ({@code ```<!--en-->}) that the root generator
     *                      strips later; fences are matched on that final form, otherwise such
     *                      a closer is missed and every heading after it is taken for code.
     */
    static Result shift(String content, int offset, UnaryOperator<String> fenceLineView) {
        if (offset == 0 || content.isEmpty()) {
            return new Result(content, 0, null);
        }

        String sep = separatorOf(content, System.lineSeparator());
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

            String fenceLine = fenceLineView.apply(line);
            if (insideFence) {
                if (isFenceClose(fenceLine, fenceChar, fenceLen)) {
                    insideFence = false;
                    fenceChar = 0;
                    fenceLen = 0;
                }
                out.append(line);
                continue;
            }

            Matcher openM = FENCE_OPEN.matcher(fenceLine);
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
                int newLevel = Math.max(1, Math.min(6, target));
                if (newLevel != target) {
                    clampedCount++;
                    if (firstClampedLine == null) {
                        firstClampedLine = line;
                    }
                }
                if (newLevel != level) {
                    rewritten = StringUtils.repeat('#', newLevel) + m.group(2);
                }
            }

            out.append(rewritten);
        }

        return new Result(out.toString(), clampedCount, firstClampedLine);
    }

    /**
     * The single separator the rewritten lines are joined with, so that mixed line endings come
     * out normalised to the dominant one: {@code \r\n} if any is present, else {@code \n} if the
     * content has any {@code \n} or lone {@code \r}, else {@code fallback} — the content is then
     * one line, or broken only by one of the rarer separators {@code \R} also matches (U+2028,
     * NEL, ...).
     */
    static String separatorOf(String content, String fallback) {
        if (content.contains("\r\n")) {
            return "\r\n";
        }
        if (content.indexOf('\n') >= 0 || content.indexOf('\r') >= 0) {
            return "\n";
        }
        return fallback;
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
}
