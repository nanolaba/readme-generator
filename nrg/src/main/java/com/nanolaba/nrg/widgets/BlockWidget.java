package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for widgets that recognise paired {@code ${widget:NAME(...)}} /
 * {@code ${widget:endNAME}} markers in the source body during a pre-pass, and rewrite them
 * to whatever the widget wants emitted in their place. Inner lines between the markers
 * stay in the body unchanged (subject to the per-frame {@code active} flag), so the normal
 * per-line pipeline keeps rendering nested widgets, properties, and language tags inside.
 *
 * <p>Subclasses implement three abstract hooks ({@link #onBlockOpen},
 * {@link #onBlockClose}, {@link #isInlineForm}) and may optionally override
 * {@link #renderInline}. The base class owns the line scan, the {@code Deque<Frame>} stack,
 * and the error recovery (stray-closer log+drop, unclosed-opener log+rollback) — subclasses
 * do not override {@link #processBlocks}.
 *
 * <p>Open and close markers must appear on their own line (whitespace allowed). The base
 * class compiles the two regex patterns once at construction time from {@link #getName()}
 * and {@link #getEndMarkerName()}. Subclasses that need a different closer-name convention
 * may override {@link #getEndMarkerName()}; the default is {@code "end" + Capitalize(name)}
 * — so {@code "if"} → {@code "endIf"}, {@code "details"} → {@code "endDetails"}.
 *
 * <p><b>NRG internal API; subject to change in minor releases.</b>
 */
public abstract class BlockWidget extends DefaultWidget {

    private final Pattern openLine;
    private final Pattern endLine;

    protected BlockWidget() {
        String name = getName();
        String end = getEndMarkerName();
        this.openLine = Pattern.compile(
                "^\\s*\\$\\{\\s*widget:" + Pattern.quote(name) + "\\s*(?:\\(([^)]*)\\))?\\s*}\\s*$");
        this.endLine = Pattern.compile(
                "^\\s*\\$\\{\\s*widget:" + Pattern.quote(end) + "\\s*}\\s*$");
    }

    /**
     * Inline-form dispatch from the per-line pipeline. The base implementation parses
     * parameters once and delegates: if {@link #isInlineForm} returns true the call goes
     * to {@link #renderInline}, otherwise an error is logged (an opener without an inline
     * form must be closed by {@code ${widget:endNAME}}, which is handled by
     * {@link #processBlocks} during the pre-pass — reaching here means the user wrote a
     * malformed opener).
     */
    @Override
    public final String getBody(WidgetTag tag, GeneratorConfig config, String language) {
        if (getEndMarkerName().equals(tag.getName())) {
            // The pre-pass's END_LINE regex requires the closer on its own line. Reaching here
            // means the closer was embedded mid-line, where it should not appear.
            LOG.error("{} widget: closer ${{widget:{}}} must appear on its own line "
                    + "(not embedded in surrounding text)", getName(), getEndMarkerName());
            return "";
        }
        Map<String, String> params = NRGUtil.parseParametersLine(tag.getParameters());
        if (!isInlineForm(params)) {
            LOG.error("{} widget: opener without inline-form parameter must be closed by "
                    + "${{widget:{}}}", getName(), getEndMarkerName());
            return "";
        }
        return renderInline(params, config, language);
    }

    /**
     * Pre-pass entry point invoked once per source body by {@code BlockDispatcher}.
     * Subclasses do not override.
     */
    public final String processBlocks(String body, GeneratorConfig config, String language) {
        if (body == null) {
            return null;
        }
        String openProbe = "widget:" + getName();
        String endProbe = "widget:" + getEndMarkerName();
        if (body.indexOf(openProbe) < 0 && body.indexOf(endProbe) < 0) {
            return body;
        }

        String[] lines = body.split("\n", -1);
        List<String> output = new ArrayList<>(lines.length);

        Deque<Frame> stack = new ArrayDeque<>();
        int outermostOpenIndexInOutput = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher openMatcher = openLine.matcher(line);
            if (openMatcher.matches()) {
                Map<String, String> params = NRGUtil.parseParametersLine(openMatcher.group(1));
                if (isInlineForm(params)) {
                    output.add(line);
                    continue;
                }
                boolean parentActive = stack.isEmpty() || stack.peek().active;
                if (stack.isEmpty()) {
                    outermostOpenIndexInOutput = output.size();
                }
                BlockOpening opening = onBlockOpen(params, config, language, parentActive, i + 1);
                stack.push(new Frame(parentActive && opening.isContentActive()));
                if (parentActive) {
                    output.addAll(opening.getEmittedLines());
                }
                continue;
            }

            Matcher endMatcher = endLine.matcher(line);
            if (endMatcher.matches()) {
                if (stack.isEmpty()) {
                    LOG.error("{} widget: unmatched stray ${{widget:{}}} at line {}",
                            getName(), getEndMarkerName(), i + 1);
                    continue;
                }
                // Emit the closer iff the *enclosing* scope is active (same gate as the opener,
                // but evaluated after pop so nested inactive blocks suppress their own closers).
                stack.pop();
                boolean parentActiveAfterPop = stack.isEmpty() || stack.peek().active;
                if (parentActiveAfterPop) {
                    output.addAll(onBlockClose(i + 1));
                }
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
            LOG.error("{} widget: unclosed ${{widget:{}}} block ({} still open)",
                    getName(), getName(), stack.size());
            if (outermostOpenIndexInOutput >= 0) {
                while (output.size() > outermostOpenIndexInOutput) {
                    output.remove(output.size() - 1);
                }
            }
        }
        return String.join("\n", output);
    }

    /**
     * Hook: handle a parsed opener. Return the lines to emit in place of the opener marker
     * plus the activity state for the new frame. Widgets without an inactive state ignore
     * {@code parentActive} and always return {@code active=true}.
     */
    protected abstract BlockOpening onBlockOpen(
            Map<String, String> params, GeneratorConfig config, String language,
            boolean parentActive, int lineNo);

    /**
     * Hook: handle a closer. Return the lines to emit in place of the closer marker.
     * Empty list means the closer is silently consumed (e.g. {@code if}).
     */
    protected abstract List<String> onBlockClose(int lineNo);

    /**
     * Predicate: opener parameters indicate the inline form. When this returns true the
     * pre-pass passes the opener line through unchanged so the per-line pipeline can render
     * it via {@link #renderInline}.
     */
    protected abstract boolean isInlineForm(Map<String, String> params);

    /**
     * Render the inline form. Default throws — subclasses that have no inline form should
     * return {@code false} from {@link #isInlineForm} for every params, so this is never
     * reached.
     */
    protected String renderInline(Map<String, String> params, GeneratorConfig config, String language) {
        throw new UnsupportedOperationException(getName() + " widget has no inline form");
    }

    /**
     * Default closer marker name: {@code "end" + capitalize(getName())}. So {@code "if"} →
     * {@code "endIf"}, {@code "details"} → {@code "endDetails"}, {@code "foo"} →
     * {@code "endFoo"}.
     */
    protected String getEndMarkerName() {
        String name = getName();
        return "end" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Returns the closer marker name as an alias so the validator treats
     * {@code ${widget:end<Name>}} as known, and per-line dispatch resolving the closer to
     * this widget short-circuits via {@link #getBody}'s closer-name check.
     *
     * <p>Subclasses that want their own true aliases (e.g. a short synonym for the opener)
     * should override and merge with {@code super.getAliases()} so the closer stays in the set.
     */
    @Override
    public Set<String> getAliases() {
        Set<String> base = super.getAliases();
        if (base.isEmpty()) {
            return Collections.singleton(getEndMarkerName());
        }
        Set<String> combined = new LinkedHashSet<>(base);
        combined.add(getEndMarkerName());
        return Collections.unmodifiableSet(combined);
    }

    private static final class Frame {
        final boolean active;

        Frame(boolean active) {
            this.active = active;
        }
    }
}
