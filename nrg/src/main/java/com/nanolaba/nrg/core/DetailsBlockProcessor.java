package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-processes a source body: matches {@code ${widget:details(...)}} … {@code ${widget:endDetails}}
 * block pairs and rewrites the marker lines into a GitHub-friendly {@code <details>} block,
 * leaving the inner content untouched so the per-line pipeline still renders nested widgets,
 * properties, and language tags inside it.
 *
 * <p>An opener whose parameter list contains {@code content=} is treated as the single-tag form
 * and left intact — {@code DetailsWidget} renders it during the normal per-line pass. This way
 * one widget name serves both forms; the {@code content=} parameter is the disambiguator.
 *
 * <p>Open and close tags must appear on their own line (whitespace around the marker allowed).
 * Marker lines are consumed; their content is replaced by the rendered HTML.
 *
 * <p>Error handling mirrors {@link IfBlockProcessor}:
 * <ul>
 *   <li>A stray {@code ${widget:endDetails}} with no open frame logs an error and the line is
 *       dropped.</li>
 *   <li>Unclosed openers at EOF log an error and the output is rolled back to the outermost
 *       opening marker, so a partial block does not leak HTML into the output.</li>
 * </ul>
 */
final class DetailsBlockProcessor {

    private static final Pattern OPEN_LINE = Pattern.compile(
            "^\\s*\\$\\{\\s*widget:details\\s*\\(([^)]*)\\)\\s*}\\s*$");
    private static final Pattern END_LINE = Pattern.compile(
            "^\\s*\\$\\{\\s*widget:endDetails\\s*}\\s*$");

    private DetailsBlockProcessor() {/* utility */}

    static String process(String body, GeneratorConfig config, String language) {
        if (body == null || (body.indexOf("widget:details") < 0 && body.indexOf("widget:endDetails") < 0)) {
            return body;
        }

        String[] lines = body.split("\n", -1);
        List<String> output = new ArrayList<>(lines.length);

        Deque<Frame> stack = new ArrayDeque<>();
        int outermostOpenIndexInOutput = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher openMatcher = OPEN_LINE.matcher(line);
            if (openMatcher.matches()) {
                Map<String, String> params = NRGUtil.parseParametersLine(openMatcher.group(1));
                if (params.containsKey("content")) {
                    // Single-tag form — leave for the per-line widget.
                    output.add(line);
                    continue;
                }
                if (stack.isEmpty()) {
                    outermostOpenIndexInOutput = output.size();
                }
                stack.push(new Frame());

                String summaryRaw = params.get("summary");
                if (summaryRaw == null) {
                    LOG.error("details widget: missing required 'summary' parameter at line {}", i + 1);
                    summaryRaw = "";
                }
                String summary = TemplateLine.renderPropertiesIn(summaryRaw, config, language);
                boolean open = Boolean.parseBoolean(params.get("open"));
                String openAttr = open ? " open" : "";

                output.add("<details" + openAttr + ">");
                output.add("<summary>" + summary + "</summary>");
                output.add("");
                continue;
            }

            Matcher endMatcher = END_LINE.matcher(line);
            if (endMatcher.matches()) {
                if (stack.isEmpty()) {
                    LOG.error("details widget: unmatched stray ${{widget:endDetails}} at line {}", i + 1);
                    continue;
                }
                stack.pop();
                output.add("");
                output.add("</details>");
                if (stack.isEmpty()) {
                    outermostOpenIndexInOutput = -1;
                }
                continue;
            }

            output.add(line);
        }

        if (!stack.isEmpty()) {
            LOG.error("details widget: unclosed ${{widget:details}} block ({} still open)", stack.size());
            if (outermostOpenIndexInOutput >= 0) {
                while (output.size() > outermostOpenIndexInOutput) {
                    output.remove(output.size() - 1);
                }
            }
        }
        return String.join("\n", output);
    }

    private static final class Frame {
        // No per-frame state; present to maintain stack depth symmetry with IfBlockProcessor.
    }
}
