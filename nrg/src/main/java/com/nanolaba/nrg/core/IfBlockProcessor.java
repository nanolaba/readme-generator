package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-processes a source body: matches {@code ${widget:if(cond='...')}} … {@code ${widget:endIf}}
 * block pairs, evaluates each condition, and drops the lines of false branches without invoking
 * any widget inside them. The remaining body is fed to the per-line generation pipeline.
 *
 * <p>Open and close tags must appear on their own line (whitespace around the marker allowed).
 * The marker line itself is consumed in either case.
 */
final class IfBlockProcessor {

    private static final Pattern OPEN_LINE = Pattern.compile(
            "^\\s*\\$\\{\\s*widget:if\\s*\\(([^)]*)\\)\\s*}\\s*$");
    private static final Pattern END_LINE = Pattern.compile(
            "^\\s*\\$\\{\\s*widget:endIf\\s*}\\s*$");

    private IfBlockProcessor() {/* utility */}

    static String process(String body, GeneratorConfig config, String language) {
        if (body == null || (body.indexOf("widget:if") < 0 && body.indexOf("widget:endIf") < 0)) {
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
                String cond = extractCond(openMatcher.group(1));
                boolean parentActive = stack.isEmpty() || stack.peek().active;
                boolean own = parentActive && evaluate(cond, config, language, i + 1);
                if (stack.isEmpty()) {
                    outermostOpenIndexInOutput = output.size();
                }
                stack.push(new Frame(own && parentActive));
                continue;
            }

            Matcher endMatcher = END_LINE.matcher(line);
            if (endMatcher.matches()) {
                if (stack.isEmpty()) {
                    LOG.error("if widget: unmatched stray ${{widget:endIf}} at line {}", i + 1);
                    continue;
                }
                stack.pop();
                if (stack.isEmpty()) {
                    outermostOpenIndexInOutput = -1;
                }
                continue;
            }

            if (!stack.isEmpty() && !stack.peek().active) {
                continue;
            }
            output.add(line);
        }

        if (!stack.isEmpty()) {
            LOG.error("if widget: unclosed ${{widget:if}} block ({} still open)", stack.size());
            if (outermostOpenIndexInOutput >= 0) {
                while (output.size() > outermostOpenIndexInOutput) {
                    output.remove(output.size() - 1);
                }
            }
        }
        return String.join("\n", output);
    }

    private static String extractCond(String params) {
        if (params == null) return "";
        Map<String, String> map = NRGUtil.parseParametersLine(params);
        String cond = map.get("cond");
        return cond == null ? "" : cond;
    }

    private static boolean evaluate(String cond, GeneratorConfig config, String language, int lineNo) {
        try {
            return IfCondition.evaluate(cond, config, language);
        } catch (RuntimeException e) {
            LOG.error("if widget: invalid condition at line {}: {} ({})", lineNo, cond, e.getMessage());
            return false;
        }
    }

    private static final class Frame {
        final boolean active;

        Frame(boolean active) {
            this.active = active;
        }
    }
}
