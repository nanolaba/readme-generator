package com.nanolaba.nrg.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes a common leading-whitespace prefix from a list of imported lines.
 *
 * <p>Mirrors Python's {@code textwrap.dedent}: the longest prefix of whitespace shared by
 * every non-blank line is stripped; blank lines are preserved as empty. The shared prefix
 * must match character-for-character (mixing tabs and spaces aborts the dedent), so an
 * accidentally aligned tab/space mix never silently corrupts the output.
 */
final class ImportDedenter {

    private ImportDedenter() {
    }

    static List<String> dedent(List<String> lines) {
        int minPrefix = Integer.MAX_VALUE;
        for (String line : lines) {
            if (!isWhitespaceOnly(line)) {
                minPrefix = Math.min(minPrefix, leadingWhitespaceLength(line));
            }
        }
        if (minPrefix == 0 || minPrefix == Integer.MAX_VALUE) {
            return lines;
        }
        // Verify all non-empty lines share the same prefix character-for-character
        String reference = lines.stream()
                .filter(s -> !isWhitespaceOnly(s))
                .findFirst()
                .orElse("")
                .substring(0, minPrefix);
        for (String line : lines) {
            if (isWhitespaceOnly(line)) {
                continue;
            }
            if (!line.startsWith(reference)) {
                return lines; // mixed prefixes — no-op
            }
        }
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            // A whitespace-only line may be shorter than the prefix: it becomes empty.
            result.add(line.substring(Math.min(minPrefix, line.length())));
        }
        return result;
    }

    private static boolean isWhitespaceOnly(String s) {
        return leadingWhitespaceLength(s) == s.length();
    }

    private static int leadingWhitespaceLength(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }
}
