package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.core.TemplateLine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@code ${widget:details(...)}} — emits a GitHub-flavored {@code <details>} collapsible
 * disclosure block. Supports two forms:
 *
 * <ul>
 *   <li><b>Block form</b> ({@code ${widget:details(summary='…')}} … {@code ${widget:endDetails}}):
 *       handled by {@link BlockWidget#processBlocks} during a pre-pass. The opener is rewritten
 *       to {@code <details[ open]>} + {@code <summary>…</summary>} + a blank line; the closer to
 *       a blank line + {@code </details>}. Inner content flows through the normal per-line
 *       pipeline, so nested widgets, {@code ${var}} substitution, and language tags inside the
 *       block keep working.</li>
 *   <li><b>Single-tag form</b> ({@code ${widget:details(summary='…', content='…')}}):
 *       handled by {@link #renderInline}. Emits a compact inline
 *       {@code <details><summary>…</summary>…</details>}.</li>
 * </ul>
 *
 * <p>Disambiguator: presence of the {@code content=} parameter on the opener — when present
 * the pre-pass passes the line through unchanged and the per-line pipeline renders inline.
 *
 * <p>{@code \n} and {@code \\} inside {@code content=} are interpreted via
 * {@link NRGUtil#processWidgetEscapes(String)}. {@code summary=} in the block form is run
 * through {@code ${var}} property substitution before being emitted (via
 * {@link TemplateLine#renderPropertiesIn}); in the single-tag form, property and language
 * substitution have already happened in the per-line pipeline before {@link #renderInline}
 * runs.
 */
public class DetailsWidget extends BlockWidget {

    @Override
    public String getName() {
        return "details";
    }

    @Override
    protected BlockOpening onBlockOpen(Map<String, String> params, GeneratorConfig config,
                                       String language, boolean parentActive, int lineNo) {
        String summaryRaw = params.get("summary");
        if (summaryRaw == null) {
            LOG.error("details widget: missing required 'summary' parameter at line {}", lineNo);
            summaryRaw = "";
        }
        String summary = TemplateLine.renderPropertiesIn(summaryRaw, config, language);
        boolean open = Boolean.parseBoolean(params.get("open"));
        String openAttr = open ? " open" : "";

        List<String> lines = Arrays.asList(
                "<details" + openAttr + ">",
                "<summary>" + summary + "</summary>",
                ""
        );
        return BlockOpening.of(lines, true);
    }

    @Override
    protected List<String> onBlockClose(int lineNo) {
        return Arrays.asList("", "</details>");
    }

    @Override
    protected boolean isInlineForm(Map<String, String> params) {
        return params.containsKey("content");
    }

    @Override
    protected String renderInline(Map<String, String> params, GeneratorConfig config, String language) {
        String summary = params.get("summary");
        if (summary == null) {
            LOG.error("details widget: missing required 'summary' parameter");
            return "";
        }
        String content = params.get("content");
        if (content == null) {
            // Unreachable: isInlineForm() returned true only because content was present.
            LOG.error("details widget: missing required 'content' parameter");
            return "";
        }
        content = NRGUtil.processWidgetEscapes(content);
        boolean open = Boolean.parseBoolean(params.get("open"));
        String openAttr = open ? " open" : "";
        return "<details" + openAttr + "><summary>" + summary + "</summary>" + content + "</details>";
    }
}
