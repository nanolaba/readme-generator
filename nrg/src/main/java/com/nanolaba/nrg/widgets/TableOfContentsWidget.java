package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
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
    public void afterRenderLine(TextStringBuilder line, GeneratorConfig config) {
        if (config.isRootGenerator()) {
            line.replaceAll(IGNORE_ATTR, "");
        }
    }

    protected String createTOC(WidgetTag widgetTag, GeneratorConfig config, String language, Config tocConfig) {

        List<Header> allHeaders = new ArrayList<>();

        Generator generator = new Generator(config.getSourceFile(), config.getSourceFileBody());
        generator.getConfig().getWidgets().stream()
                .filter(widget -> widget instanceof TableOfContentsWidget)
                .forEach(widget -> widget.setEnabled(false));
        String sourceFileBody = generator.getResult(language).getContent().toString();

        try (BufferedReader reader = new BufferedReader(new StringReader(sourceFileBody))) {

            int indexOfTOC = NRGUtil.findFirstUnescapedOccurrenceLine(sourceFileBody, "${widget:tableOfContents");

            int minLevel = tocConfig.getMinDepth() - 1;
            int maxLevel = tocConfig.getMaxDepth() - 1;

            return indexOfTOC < 0 ? "" : reader.lines()
                    .skip(indexOfTOC)
                    .filter(line -> !line.contains(IGNORE_ATTR))
                    .map(s -> new TemplateLine(config, s, 0).fillLineWithProperties(language))
                    .filter(Objects::nonNull)
                    .map(line -> new Header(line, tocConfig, allHeaders))
                                         .filter(header -> header.level >= minLevel && header.level <= maxLevel)
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
        if (map.containsKey("min-depth")) {
            Integer value = parseDepth(map.get("min-depth"), "min-depth");
            if (value != null) {
                config.setMinDepth(value);
            }
        }
        if (map.containsKey("max-depth")) {
            Integer value = parseDepth(map.get("max-depth"), "max-depth");
            if (value != null) {
                config.setMaxDepth(value);
            }
        }
        if (config.getMinDepth() > config.getMaxDepth()) {
            LOG.error("tableOfContents widget: min-depth ({}) must not exceed max-depth ({}); using defaults",
                    config.getMinDepth(), config.getMaxDepth());
            config.setMinDepth(2);
            config.setMaxDepth(6);
        }

        return config;
    }

    private static Integer parseDepth(String raw, String paramName) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1 || value > 6) {
                LOG.error("tableOfContents widget: {} must be between 1 and 6, got '{}'", paramName, raw);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            LOG.error("tableOfContents widget: {} must be an integer, got '{}'", paramName, raw);
            return null;
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {
        private String title;
        private boolean ordered;
        private int minDepth = 2;
        private int maxDepth = 6;

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

        public int getMinDepth() {
            return minDepth;
        }

        public void setMinDepth(int minDepth) {
            this.minDepth = minDepth;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Header {

        public static final Pattern HEADER_PATTERN = Pattern.compile("(#+)(.*)");
        private String title;
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

                Header prev = getPreviousHeader(level);
                if (prev != null) {
                    number = prev.number + 1;
                } else {
                    number = 1;
                }

                headers.add(this);
            }
        }

        public String getAnchor() {
            if (title == null || title.trim().isEmpty()) {
                return "";
            }

            // Удаляем markdown-разметку
            String cleaned = title
                    .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1") // ссылки [text](url) -> text
                    .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // жирный текст **text** -> text
                    .replaceAll("\\*([^*]+)\\*", "$1") // курсив *text* -> text
                    .replaceAll("`([^`]+)`", "$1") // код `text` -> text
                    .replaceAll("~~([^~]+)~~", "$1") // зачеркнутый текст ~~text~~ -> text
                    .replaceAll("<[^>]+>", "") // Удаляем HTML-теги
                    .replaceAll("https?://\\S+", "") // Удаляем URL
                    .replaceAll("www\\.\\S+", ""); // Удаляем URL

            // GitHub-style anchor generation - сохраняем Unicode символы
            return cleaned.toLowerCase()
                    .replaceAll("[^\\p{L}\\p{N}\\p{M}\\s_-]", "") // Оставляем буквы, цифры, модификаторы, пробелы, дефисы и подчеркивания
                    .replaceAll("\\s+", "-") // Заменяем пробелы на дефисы
                    .replaceAll("_", "-") // Заменяем подчеркивания на дефисы
                    .replaceAll("-{2,}", "-") // Удаляем множественные дефисы
                    .replaceAll("^-+|-+$", ""); // Удаляем дефисы в начале и конце
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
            int minLevel = config.getMinDepth() - 1;
            for (int i = 0; i < Math.max(0, level - minLevel); i++) {
                res.append("\t");
            }
            if (config.isOrdered()) {
                res.append(number).append(".");
            } else {
                res.append("-");
            }
            res.append(" [").append(title).append("](#").append(getAnchor()).append(")");
            res.append(System.lineSeparator());
            return res.toString();
        }
    }
}
