package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.core.TemplateLine;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TableOfContentsWidget implements NRGWidget {

    @Override
    public String getName() {
        return "tableOfContents";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config tocConfig = getConfig(widgetTag.getParameters());

        String toc = createTOC(widgetTag, config, language, tocConfig);

        return (StringUtils.isNotEmpty(tocConfig.getTitle()) ? "## " + tocConfig.getTitle() + System.lineSeparator() : "") + toc;
    }

    protected String createTOC(WidgetTag widgetTag, GeneratorConfig config, String language, Config tocConfig) {

        List<Header> allHeaders = new ArrayList<>();

        return config.getSourceFileBody().lines()
                .skip(widgetTag.getLine().getLineNumber())
                .filter(this::isHeader)
                .filter(s -> new TemplateLine(config, s, 0).isLineVisible(language))
                .map(s -> new Header(s, tocConfig, allHeaders))
                .filter(h -> h.level > 0)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    private Config getConfig(String parameters) {
        Config config = new Config();
        Map<String, String> map = NRGUtil.parseParametersLine(parameters);

        if (map.containsKey("title")) {
            config.setTitle(map.get("title"));
        }
        if (map.containsKey("ordered")) {
            config.setOrdered(Boolean.parseBoolean(map.get("ordered")));
        }

        return config;
    }

    private boolean isHeader(String line) {
        return StringUtils.trimToEmpty(line).startsWith("#");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Config {
        private String title;
        private boolean ordered;

        public String getTitle() {
            return title;
        }

        public Config setTitle(String title) {
            this.title = title;
            return this;
        }

        public boolean isOrdered() {
            return ordered;
        }

        public Config setOrdered(boolean ordered) {
            this.ordered = ordered;
            return this;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Header {

        public static final Pattern HEADER_PATTERN = Pattern.compile("(#+)(.*)");
        private String title;
        private String anchor;
        private int level;
        private int number;
        private final List<Header> headers;
        private final Config config;

        public Header(String line, Config config, List<Header> headers) {
            this.headers = headers;
            this.config = config;

            Matcher m = HEADER_PATTERN.matcher(line);

            if (m.find()) {
                level = m.group(1).length() - 1;
                title = StringUtils.trimToEmpty(m.group(2));
                anchor = title.toLowerCase()
                        .replace("[", "")
                        .replace("]", "")
                        .replace("(", "")
                        .replace(")", "")
                        .replace("https://", "")
                        .replace("http://", "")
                        .replace(".", "")
                        .replace(" ", "-");

                Header prev = getPreviousHeader(level);
                if (prev != null) {
                    number = prev.number + 1;
                } else {
                    number = 1;
                }

                headers.add(this);
            }
        }

        private Header getPreviousHeader(int level) {
            for (int i = headers.size() - 1; i >= 0; i--) {
                Header header = headers.get(i);
                if (header.level < level) {
                    return null;
                }
                if (header.level == level) {
                    return header;
                }
            }

            return null;
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            res.append("\t".repeat(Math.max(0, level - 1)));
            if (config.isOrdered()) {
                res.append(number).append(".");
            } else {
                res.append("-");
            }
            res.append(" [").append(title).append("](#").append(anchor).append(")");
            res.append(System.lineSeparator());
            return res.toString();
        }
    }
}
