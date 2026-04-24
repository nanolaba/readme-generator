package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.*;

public class AlertWidget extends DefaultWidget {

    static final Set<String> VALID_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("note", "tip", "important", "warning", "caution")));

    @Override
    public String getName() {
        return "alert";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> params = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String rawType = params.get("type");
        if (rawType == null || !VALID_TYPES.contains(rawType.trim().toLowerCase())) {
            LOG.error("alert widget: invalid type '{}' (expected one of {})", rawType, VALID_TYPES);
            return "";
        }

        String text = params.get("text");
        if (text == null) {
            text = "";
        }
        text = processEscapes(text);

        String type = rawType.trim().toUpperCase();
        String ls = System.lineSeparator();

        StringBuilder sb = new StringBuilder();
        sb.append("> [!").append(type).append("]");
        if (!text.isEmpty()) {
            for (String line : text.split("\n", -1)) {
                sb.append(ls).append("> ").append(line);
            }
        }
        return sb.toString();
    }

    static String processEscapes(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'n') {
                    out.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    out.append('\\');
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
