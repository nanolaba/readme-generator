package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.IfCondition;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@code ${widget:if(...)}} — conditional rendering. Two forms:
 *
 * <ul>
 *   <li><b>Block form</b> ({@code ${widget:if(cond='…')}} … {@code ${widget:endIf}}):
 *       evaluates {@code cond}; when truthy, the inner lines pass through to the per-line
 *       pipeline; when falsy, the entire block (including inner widgets) is dropped before
 *       any rendering — so widgets in dead branches never execute. Markers themselves are
 *       silently consumed in either case.</li>
 *   <li><b>Inline form</b> ({@code ${widget:if(cond='…', text='…')}}): emits {@code text}
 *       when {@code cond} is truthy; emits empty string when falsy. {@code \n} and {@code \\}
 *       inside {@code text} are interpreted via {@link NRGUtil#processWidgetEscapes(String)}.
 *       Property and language substitution against the outer template happen before the widget
 *       runs.</li>
 * </ul>
 *
 * <p>Disambiguator: presence of the {@code text=} parameter on the opener. Condition grammar
 * is documented under {@link IfCondition}.
 */
public class IfWidget extends BlockWidget {

    @Override
    public String getName() {
        return "if";
    }

    @Override
    protected BlockOpening onBlockOpen(Map<String, String> params, GeneratorConfig config,
                                       String language, boolean parentActive, int lineNo) {
        // Short-circuit when parent is inactive: don't evaluate the condition at all.
        // Preserves the historical IfBlockProcessor behaviour where a malformed condition
        // inside a dead branch does NOT log an error (because evaluate() is never called).
        if (!parentActive) {
            return BlockOpening.empty(false);
        }
        String cond = params.get("cond");
        if (cond == null) {
            cond = "";
        }
        boolean own;
        try {
            own = IfCondition.evaluate(cond, config, language);
        } catch (RuntimeException e) {
            LOG.error("if widget: invalid condition at line {}: {} ({})", lineNo, cond, e.getMessage());
            own = false;
        }
        return BlockOpening.empty(own);
    }

    @Override
    protected List<String> onBlockClose(int lineNo) {
        return Collections.emptyList();
    }

    @Override
    protected boolean isInlineForm(Map<String, String> params) {
        return params.containsKey("text");
    }

    @Override
    protected String renderInline(Map<String, String> params, GeneratorConfig config, String language) {
        String cond = params.get("cond");
        if (cond == null) {
            cond = "";
        }
        String text = params.get("text");
        if (text == null) {
            // Unreachable: isInlineForm() returned true only because text was present.
            text = "";
        }
        boolean active;
        try {
            active = IfCondition.evaluate(cond, config, language);
        } catch (RuntimeException e) {
            LOG.error("if widget: invalid condition '{}' in inline form: {}", cond, e.getMessage());
            active = false;
        }
        if (!active) {
            return "";
        }
        return NRGUtil.processWidgetEscapes(text);
    }
}
