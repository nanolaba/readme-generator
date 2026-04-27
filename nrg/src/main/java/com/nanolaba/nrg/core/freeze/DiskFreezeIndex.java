package com.nanolaba.nrg.core.freeze;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.Validator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * On-disk view of a previously-generated output file: a map from {@code freeze id} to the
 * raw text content found between its open and close markers.
 *
 * <p>Tolerant of authoring mistakes: a malformed or duplicate block is skipped with a
 * {@link com.nanolaba.logging.LOG#warn LOG.warn} and excluded from the map; the first
 * occurrence of a duplicate id wins. The content is preserved verbatim — no trimming,
 * no escape processing — so external-tool output (e.g. an HTML table) round-trips byte
 * for byte. Always reads files as UTF-8 to match how the generator writes them.
 */
public final class DiskFreezeIndex {

    private static final DiskFreezeIndex EMPTY =
            new DiskFreezeIndex(new HashMap<String, String>());

    private final Map<String, String> contentById;

    private DiskFreezeIndex(Map<String, String> contentById) {
        this.contentById = contentById;
    }

    public static DiskFreezeIndex read(File file) {
        if (file == null || !file.isFile()) {
            return EMPTY;
        }
        String body;
        try {
            body = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Could not read {} for freeze lookup: {}", file.getAbsolutePath(), e.getMessage());
            return EMPTY;
        }

        FreezeBlockParser.Result parsed = FreezeBlockParser.parse(body, file);
        for (Validator.Diagnostic d : parsed.getDiagnostics()) {
            LOG.warn("freeze: {}", d);
        }

        String[] lines = body.split("\\R", -1);
        Map<String, String> map = new LinkedHashMap<>();
        for (FreezeMarker m : parsed.getMarkers()) {
            if (map.containsKey(m.getId())) {
                LOG.warn("freeze id='{}' in {}: duplicate occurrences; using first, ignoring rest",
                        m.getId(), file.getName());
                continue;
            }
            map.put(m.getId(), extractContent(lines, m));
        }
        return new DiskFreezeIndex(map);
    }

    public Optional<String> lookup(String id) {
        return Optional.ofNullable(contentById.get(id));
    }

    /**
     * Extracts the raw body lines between {@code open} (exclusive) and {@code close} (exclusive),
     * joined with {@code "\n"} and terminated with a trailing newline if any content lines exist.
     * Empty regions (close immediately after open) yield the empty string.
     */
    private static String extractContent(String[] lines, FreezeMarker m) {
        int from = m.getOpenLineIndex() + 1;
        int to = m.getCloseLineIndex();
        if (from >= to) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString();
    }
}
