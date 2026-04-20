package com.nanolaba.nrg.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class ImportRegionExtractor {

    private static final Pattern ANY_MARKER = Pattern.compile("nrg:(?:begin|end):[A-Za-z0-9_-]+");

    private ImportRegionExtractor() {
    }

    /**
     * Extracts lines between matching begin/end markers for the given region name.
     * Marker lines themselves are not included. Lines containing any other nrg markers
     * (nested or unrelated) have those marker lines stripped from the output entirely.
     *
     * @return the extracted lines, or null if the region was not found or its end marker
     * is missing.
     */
    static List<String> extract(List<String> sourceLines, String regionName) {
        Pattern beginPattern = Pattern.compile("nrg:begin:" + Pattern.quote(regionName) + "(?![A-Za-z0-9_-])");
        Pattern endPattern = Pattern.compile("nrg:end:" + Pattern.quote(regionName) + "(?![A-Za-z0-9_-])");

        int beginIdx = -1;
        for (int i = 0; i < sourceLines.size(); i++) {
            if (beginPattern.matcher(sourceLines.get(i)).find()) {
                beginIdx = i;
                break;
            }
        }
        if (beginIdx < 0) {
            return null;
        }
        int endIdx = -1;
        for (int i = beginIdx + 1; i < sourceLines.size(); i++) {
            if (endPattern.matcher(sourceLines.get(i)).find()) {
                endIdx = i;
                break;
            }
        }
        if (endIdx < 0) {
            return null;
        }
        List<String> result = new ArrayList<>(endIdx - beginIdx - 1);
        for (int i = beginIdx + 1; i < endIdx; i++) {
            String line = sourceLines.get(i);
            if (ANY_MARKER.matcher(line).find()) {
                continue;
            }
            result.add(line);
        }
        return result;
    }
}
