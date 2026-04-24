package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class MathWidget extends DefaultWidget {

    static final String DEFAULT_SVG_SERVICE = "https://latex.codecogs.com/svg.image?";

    @Override
    public String getName() {
        return "math";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> params = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String rawExpr = params.get("expr");
        if (rawExpr == null || rawExpr.isEmpty()) {
            LOG.error("math widget: missing required 'expr' parameter");
            return "";
        }
        String expr = unescapeBackslashes(rawExpr);

        String display = params.getOrDefault("display", "inline").trim().toLowerCase();
        if (!display.equals("inline") && !display.equals("block")) {
            LOG.error("math widget: invalid display '{}' (expected 'inline' or 'block')", display);
            return "";
        }

        String renderer = params.getOrDefault("renderer", "native").trim().toLowerCase();
        if (!renderer.equals("native") && !renderer.equals("svg")) {
            LOG.error("math widget: invalid renderer '{}' (expected 'native' or 'svg')", renderer);
            return "";
        }

        if (renderer.equals("native")) {
            return display.equals("block") ? "$$" + expr + "$$" : "$" + expr + "$";
        }

        String service = params.get("service");
        if (service == null || service.isEmpty()) {
            service = DEFAULT_SVG_SERVICE;
        }
        String alt = params.get("alt");
        if (alt == null || alt.isEmpty()) {
            alt = expr;
        } else {
            alt = unescapeBackslashes(alt);
        }
        String urlExpr = display.equals("block") ? "\\displaystyle " + expr : expr;
        return "![" + alt + "](" + service + urlEncode(urlExpr) + ")";
    }

    static String unescapeBackslashes(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '\\') {
                out.append('\\');
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
