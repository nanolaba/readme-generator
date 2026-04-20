package com.nanolaba.nrg.widgets;

import java.util.ArrayList;
import java.util.List;

final class ImportDedenter {

    private ImportDedenter() {
    }

    static List<String> dedent(List<String> lines) {
        if (lines.isEmpty()) {
            return lines;
        }
        int minPrefix = Integer.MAX_VALUE;
        for (String line : lines) {
            if (isWhitespaceOnly(line)) {
                continue;
            }
            int prefix = leadingWhitespaceLength(line);
            if (prefix < minPrefix) {
                minPrefix = prefix;
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
            if (line.length() <= minPrefix) {
                result.add("");
            } else {
                result.add(line.substring(minPrefix));
            }
        }
        return result;
    }

    private static boolean isWhitespaceOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int leadingWhitespaceLength(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }
}
