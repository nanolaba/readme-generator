package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.Map;

/**
 * {@code ${widget:details(summary='…', content='…')}} — emits a compact inline
 * GitHub {@code <details>} disclosure block ({@code <details><summary>…</summary>…</details>}).
 *
 * <p>This widget handles the <em>single-tag</em> form only. The block form
 * ({@code ${widget:details(summary='…')}} … {@code ${widget:endDetails}}) is rewritten by
 * {@link com.nanolaba.nrg.core.DetailsBlockProcessor} during a pre-pass over the source body
 * and never reaches this code path. Disambiguator: the {@code content=} parameter — when
 * present the pre-pass skips the opener and lets the per-line pipeline render here.
 *
 * <p>{@code \n} and {@code \\} inside {@code content=} are interpreted via
 * {@link NRGUtil#processWidgetEscapes(String)} — broader escape vocabulary (e.g. {@code \r},
 * {@code \t}) is not supported. {@code summary=} is taken verbatim (any property or language
 * substitution has already happened in the per-line pipeline before {@code getBody} runs).
 */
public class DetailsWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "details";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> params = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String summary = params.get("summary");
        if (summary == null) {
            LOG.error("details widget: missing required 'summary' parameter");
            return "";
        }

        String content = params.get("content");
        if (content == null) {
            // No content= means this is a block-form opener that the pre-pass didn't catch.
            // Log and render empty so the literal tag doesn't leak into the output.
            LOG.error("details widget: single-tag form requires 'content' (block form must be "
                    + "closed by ${widget:endDetails})");
            return "";
        }
        content = NRGUtil.processWidgetEscapes(content);

        boolean open = Boolean.parseBoolean(params.get("open"));
        String openAttr = open ? " open" : "";

        return "<details" + openAttr + "><summary>" + summary + "</summary>" + content + "</details>";
    }
}
