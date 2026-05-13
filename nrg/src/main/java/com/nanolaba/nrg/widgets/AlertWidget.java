package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.util.*;

/**
 * {@code ${widget:alert(type='note|tip|important|warning|caution', text='...')}} — emits
 * GitHub-flavoured alert/admonition blockquotes ({@code > [!NOTE]} et al.).
 *
 * <p>Multi-line text is supported via literal {@code \n} escapes inside the {@code text}
 * parameter. Unknown {@code type} values are rejected with an error and the widget renders
 * an empty string; valid types are normalised to upper case for the marker line.
 */
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
        text = NRGUtil.processWidgetEscapes(text);

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

}
