package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.nrg.core.TemplateLine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TableOfContentsWidget extends DefaultWidget {

    public static final String IGNORE_ATTR = "<!--toc.ignore-->";

    @Override
    public String getName() {
        return "tableOfContents";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config tocConfig = getConfig(widgetTag.getParameters());

        String title = StringUtils.isNotEmpty(tocConfig.getTitle()) ? "## " + tocConfig.getTitle() + System.lineSeparator() : "";

        return title + createTOC(widgetTag, config, language, tocConfig);
    }

    @Override
    public void afterRenderLine(TextStringBuilder line) {
        line.replaceAll(IGNORE_ATTR, "");
    }

    protected String createTOC(WidgetTag widgetTag, GeneratorConfig config, String language, Config tocConfig) {

        List<Header> allHeaders = new ArrayList<>();

        Generator generator = new Generator(config.getSourceFile(), config.getSourceFileBody());
        generator.getConfig().getWidgets().removeIf(widget -> widget instanceof TableOfContentsWidget);
        String sourceFileBody = generator.getResult(language).getContent().toString();

        try (BufferedReader reader = new BufferedReader(new StringReader(sourceFileBody))) {

            int indexOfTOC = NRGUtil.findFirstUnescapedOccurrenceLine(sourceFileBody, "${widget:tableOfContents");

            return indexOfTOC < 0 ? "" : reader.lines()
                    .skip(indexOfTOC)
                    .filter(line -> !line.contains(IGNORE_ATTR))
                    .map(s -> new TemplateLine(config, s, 0).fillLineWithProperties(language))
                    .filter(Objects::nonNull)
                    .map(line -> new Header(line, tocConfig, allHeaders))
                    .filter(header -> header.level > 0)
                    .map(Object::toString)
                    .collect(Collectors.joining());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {
        private String title;
        private boolean ordered;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isOrdered() {
            return ordered;
        }

        public void setOrdered(boolean ordered) {
            this.ordered = ordered;
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
                        .replace("'", "")
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
                } else if (header.level == level) {
                    return header;
                }
            }

            return null;
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < Math.max(0, level - 1); i++) {
                res.append("\t");
            }
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
