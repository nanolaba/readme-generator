package com.nanolaba.nrg.core.freeze;

import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.core.Validator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nanolaba.nrg.core.NRGConstants.FREEZE_ATTR_ID;
import static com.nanolaba.nrg.core.NRGConstants.FREEZE_ATTR_SOURCE_LANG;

/**
 * Linear scanner that extracts {@code <!--nrg.freeze ...-->} / {@code <!--/nrg.freeze-->}
 * pairs from a body of text. Used both for template authoring validation and for indexing
 * on-disk generator output.
 *
 * <p>Markers must each appear on their own line (whitespace around them is tolerated).
 * The parser produces a {@link Result} pairing every well-formed {@link FreezeMarker} with
 * a list of {@link Validator.Diagnostic} entries describing authoring mistakes (missing
 * {@code id}, duplicate ids, unbalanced markers, nesting, unknown attributes). Markers
 * with a valid {@code id} but unknown attributes are still emitted — the diagnostic
 * surfaces the issue without invalidating the marker. Callers decide whether diagnostics
 * are fatal.
 */
public final class FreezeBlockParser {

    private static final Pattern OPEN_LINE = Pattern.compile(
            "^\\s*<!--\\s*nrg\\.freeze(\\s+[^>]*?)?\\s*-->\\s*$");
    private static final Pattern CLOSE_LINE = Pattern.compile(
            "^\\s*<!--\\s*/nrg\\.freeze\\s*-->\\s*$");

    private static final Set<String> ALLOWED_ATTRIBUTES =
            new HashSet<>(Arrays.asList(FREEZE_ATTR_ID, FREEZE_ATTR_SOURCE_LANG));

    private static final String UNKNOWN_ID = "<unknown>";

    private FreezeBlockParser() {
    }

    public static Result parse(String body, File source) {
        List<FreezeMarker> markers = new ArrayList<>();
        List<Validator.Diagnostic> diagnostics = new ArrayList<>();
        if (body == null || body.isEmpty()) {
            return new Result(markers, diagnostics);
        }
        String[] lines = body.split("\\R", -1);

        int openLine = -1;
        String openLineText = null;
        Map<String, String> openAttrs = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher openMatcher = OPEN_LINE.matcher(line);
            if (openMatcher.matches()) {
                if (openLine >= 0) {
                    // Drop the inner open: we keep the outer block live so the next close
                    // pairs with it. Continuing to scan also surfaces any cascading issues.
                    diagnostics.add(error(source, i + 1,
                            "nested freeze block at line " + (i + 1)
                                    + " inside open block from line " + (openLine + 1)
                                    + "; nesting is not supported"));
                    continue;
                }
                openAttrs = parseAttributes(openMatcher.group(1));
                openLine = i;
                openLineText = line;
                continue;
            }
            Matcher closeMatcher = CLOSE_LINE.matcher(line);
            if (closeMatcher.matches()) {
                if (openLine < 0) {
                    diagnostics.add(error(source, i + 1,
                            "unmatched <!--/nrg.freeze--> at line " + (i + 1)));
                    continue;
                }
                String id = openAttrs.get(FREEZE_ATTR_ID);
                String sourceLang = openAttrs.get(FREEZE_ATTR_SOURCE_LANG);
                String idForMessages = (id == null || id.isEmpty()) ? UNKNOWN_ID : id;

                for (Map.Entry<String, String> entry : openAttrs.entrySet()) {
                    if (!ALLOWED_ATTRIBUTES.contains(entry.getKey())) {
                        diagnostics.add(error(source, openLine + 1,
                                "freeze id='" + idForMessages + "' at line " + (openLine + 1)
                                        + ": unknown attribute '" + entry.getKey()
                                        + "' (allowed: id, source-lang)"));
                    }
                }

                if (id == null || id.isEmpty()) {
                    diagnostics.add(error(source, openLine + 1,
                            "freeze marker at line " + (openLine + 1)
                                    + ": missing required 'id' attribute"));
                } else {
                    markers.add(new FreezeMarker(id, sourceLang, openLine, i,
                            openLineText, line));
                }
                openLine = -1;
                openLineText = null;
                openAttrs = null;
            }
        }

        if (openLine >= 0) {
            String id = openAttrs.get(FREEZE_ATTR_ID);
            String idForMessages = (id == null || id.isEmpty()) ? UNKNOWN_ID : id;
            diagnostics.add(error(source, openLine + 1,
                    "freeze id='" + idForMessages + "' at line "
                            + (openLine + 1) + ": missing closing <!--/nrg.freeze--> marker"));
        }

        diagnostics.addAll(detectDuplicateIds(markers, source));

        return new Result(markers, diagnostics);
    }

    /**
     * Reports each pair of duplicate-id markers exactly once, keyed by the first occurrence.
     * Subsequent duplicates of the same id are each paired with the first-seen line so the
     * user sees one diagnostic per offending occurrence rather than O(n^2) noise.
     */
    private static List<Validator.Diagnostic> detectDuplicateIds(List<FreezeMarker> markers, File source) {
        List<Validator.Diagnostic> out = new ArrayList<>();
        Map<String, Integer> firstSeen = new LinkedHashMap<>();
        for (FreezeMarker m : markers) {
            Integer firstLine = firstSeen.get(m.getId());
            if (firstLine == null) {
                firstSeen.put(m.getId(), m.getOpenLineIndex() + 1);
            } else {
                out.add(error(source, m.getOpenLineIndex() + 1,
                        "duplicate freeze id '" + m.getId() + "' at lines "
                                + firstLine + " and " + (m.getOpenLineIndex() + 1)));
            }
        }
        return out;
    }

    private static Map<String, String> parseAttributes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new HashMap<>();
        }
        return NRGUtil.parseParametersLine(raw.trim());
    }

    private static Validator.Diagnostic error(File file, int line, String message) {
        return new Validator.Diagnostic(file, line, message, Validator.Severity.ERROR);
    }

    public static final class Result {
        private final List<FreezeMarker> markers;
        private final List<Validator.Diagnostic> diagnostics;

        Result(List<FreezeMarker> markers, List<Validator.Diagnostic> diagnostics) {
            this.markers = markers;
            this.diagnostics = diagnostics;
        }

        public List<FreezeMarker> getMarkers() {
            return markers;
        }

        public List<Validator.Diagnostic> getDiagnostics() {
            return diagnostics;
        }
    }
}
