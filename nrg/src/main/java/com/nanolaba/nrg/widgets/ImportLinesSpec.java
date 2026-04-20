package com.nanolaba.nrg.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ImportLinesSpec {

    private final List<int[]> ranges;

    private ImportLinesSpec(List<int[]> ranges) {
        this.ranges = Collections.unmodifiableList(ranges);
    }

    List<int[]> getRanges() {
        return ranges;
    }

    List<String> apply(List<String> sourceLines) {
        List<String> result = new ArrayList<>();
        int total = sourceLines.size();
        for (int[] range : ranges) {
            int start = range[0];
            int end = range[1] == Integer.MAX_VALUE ? total : Math.min(range[1], total);
            if (start > total) {
                // fully out of range — log warn at caller side; here we just emit nothing
                continue;
            }
            for (int i = start; i <= end; i++) {
                result.add(sourceLines.get(i - 1));
            }
        }
        return result;
    }

    static ImportLinesSpec parse(String spec) {
        if (spec == null || spec.trim().isEmpty()) {
            throw new IllegalArgumentException("lines spec is empty");
        }
        String[] parts = spec.split(",", -1);
        List<int[]> ranges = new ArrayList<>(parts.length);
        for (String raw : parts) {
            String part = raw.trim();
            if (part.isEmpty()) {
                throw new IllegalArgumentException("empty range in lines spec: '" + spec + "'");
            }
            ranges.add(parseSingleRange(part, spec));
        }
        return new ImportLinesSpec(ranges);
    }

    private static int[] parseSingleRange(String part, String fullSpec) {
        int dashCount = countDashes(part);
        if (dashCount == 0) {
            int n = parsePositiveInt(part, fullSpec);
            return new int[]{n, n};
        }
        if (dashCount > 1) {
            throw new IllegalArgumentException("invalid range '" + part + "' in lines spec '" + fullSpec + "'");
        }
        int dashIdx = part.indexOf('-');
        String startStr = part.substring(0, dashIdx);
        String endStr = part.substring(dashIdx + 1);
        int start = startStr.isEmpty() ? 1 : parsePositiveInt(startStr, fullSpec);
        int end = endStr.isEmpty() ? Integer.MAX_VALUE : parsePositiveInt(endStr, fullSpec);
        if (start > end) {
            throw new IllegalArgumentException("reversed range '" + part + "' in lines spec '" + fullSpec + "'");
        }
        return new int[]{start, end};
    }

    private static int countDashes(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '-') {
                n++;
            }
        }
        return n;
    }

    private static int parsePositiveInt(String s, String fullSpec) {
        int n;
        try {
            n = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("non-numeric value '" + s + "' in lines spec '" + fullSpec + "'", e);
        }
        if (n < 1) {
            throw new IllegalArgumentException("non-positive value '" + s + "' in lines spec '" + fullSpec + "'");
        }
        return n;
    }
}
