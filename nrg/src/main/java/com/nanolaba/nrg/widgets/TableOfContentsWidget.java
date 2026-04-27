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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code ${widget:tableOfContents(...)}} — generates a Markdown table of contents from
 * the headings that appear after the widget's own line in the rendered document.
 *
 * <p>Implementation strategy: spin up a <em>secondary</em> {@link Generator} over the
 * same source body with this widget disabled, scan the rendered output for {@code #}-prefixed
 * headers starting at the line where the TOC widget appears, then build links with anchor
 * slugs in the requested {@link AnchorStyle GitHub/GitLab/Bitbucket} flavour. Headers tagged
 * {@code <!--toc.ignore-->} are skipped; the marker itself is stripped from the surrounding
 * line via {@link #afterRenderLine}.
 *
 * <p>Configurable parameters: {@code title}, {@code ordered}, {@code min-depth},
 * {@code max-depth}, {@code min-items}, and {@code anchor-style}. Depth bounds clamp at
 * 1..6; the TOC degrades to an empty string when {@code anchor-style} is unrecognised, when
 * fewer than {@code min-items} headings match, or when the widget tag is preceded by an
 * escape backslash on the source line.
 */
public class TableOfContentsWidget extends DefaultWidget {

    public static final String IGNORE_ATTR = "<!--toc.ignore-->";

    private static final Pattern FENCE_OPEN = Pattern.compile("^(`{3,}|~{3,}).*$");
    private static final Pattern HEADING_LINE = Pattern.compile("^#+.*$");

    @Override
    public String getName() {
        return "tableOfContents";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config tocConfig = getConfig(widgetTag.getParameters());

        String toc = createTOC(widgetTag, config, language, tocConfig);
        if (toc.isEmpty()) {
            return "";
        }

        String title = StringUtils.isNotEmpty(tocConfig.getTitle()) ? "## " + tocConfig.getTitle() + System.lineSeparator() : "";

        return title + toc;
    }

    @Override
    public void afterRenderLine(TextStringBuilder line, GeneratorConfig config) {
        if (config.isRootGenerator()) {
            line.replaceAll(IGNORE_ATTR, "");
        }
    }

    protected String createTOC(WidgetTag widgetTag, GeneratorConfig config, String language, Config tocConfig) {

        if (tocConfig.getAnchorStyle() == null) {
            return "";
        }

        List<Header> allHeaders = new ArrayList<>();

        Generator generator = new Generator(config.getSourceFile(), config.getSourceFileBody());
        generator.getConfig().setRootGenerator(false);
        generator.getConfig().getWidgets().stream()
                .filter(widget -> widget instanceof TableOfContentsWidget)
                .forEach(widget -> widget.setEnabled(false));
        String sourceFileBody = generator.getResult(language).getContent().toString();

        int indexOfTOC = NRGUtil.findFirstUnescapedOccurrenceLine(sourceFileBody, "${widget:tableOfContents");
        if (indexOfTOC < 0) {
            return "";
        }

        int minLevel = tocConfig.getMinDepth() - 1;
        int maxLevel = tocConfig.getMaxDepth() - 1;

        List<String> items = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(sourceFileBody))) {
            String rawLine;
            int lineIndex = 0;
            boolean insideFence = false;
            char fenceChar = 0;
            int fenceLen = 0;

            while ((rawLine = reader.readLine()) != null) {
                int currentLine = lineIndex++;

                // Fence state must be tracked from the first line so the body's overall
                // fence layout is correct even when the TOC widget itself sits inside an
                // earlier fence; otherwise we'd start mid-block with insideFence=false.
                if (insideFence) {
                    if (isFenceClose(rawLine, fenceChar, fenceLen)) {
                        insideFence = false;
                        fenceChar = 0;
                        fenceLen = 0;
                    }
                    continue;
                }
                Matcher fenceOpen = FENCE_OPEN.matcher(rawLine);
                if (fenceOpen.matches()) {
                    insideFence = true;
                    fenceChar = fenceOpen.group(1).charAt(0);
                    fenceLen = fenceOpen.group(1).length();
                    continue;
                }

                if (currentLine < indexOfTOC) {
                    continue;
                }
                if (rawLine.contains(IGNORE_ATTR)) {
                    continue;
                }

                String rendered = new TemplateLine(config, rawLine, 0).fillLineWithProperties(language, false);
                if (rendered == null) {
                    continue;
                }

                // Anchored check: only treat lines that *start* with `#` as headings.
                // This filters out inline code spans (`# foo`), escaped headings (\# foo),
                // and indented code blocks — none of which should poison the heading list,
                // even though Header.HEADER_PATTERN below uses find() for lenient slug
                // generation when the class is exercised in isolation.
                if (!HEADING_LINE.matcher(rendered).matches()) {
                    continue;
                }

                Header header = new Header(rendered, tocConfig, allHeaders);
                if (header.level >= minLevel && header.level <= maxLevel) {
                    items.add(header.toString());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (items.size() < tocConfig.getMinItems()) {
            return "";
        }
        return String.join("", items);
    }

    private static boolean isFenceClose(String line, char marker, int minLen) {
        int i = 0;
        int n = line.length();
        int count = 0;
        while (i < n && line.charAt(i) == marker) {
            count++;
            i++;
        }
        if (count < minLen) {
            return false;
        }
        while (i < n) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return false;
            }
            i++;
        }
        return true;
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
        if (map.containsKey("min-items")) {
            Integer value = parseMinItems(map.get("min-items"));
            if (value != null) {
                config.setMinItems(value);
            }
        }
        if (map.containsKey("anchor-style")) {
            AnchorStyle style = AnchorStyle.from(map.get("anchor-style"));
            if (style == null) {
                LOG.error("tableOfContents widget: unknown anchor-style '{}' (expected github|gitlab|bitbucket)",
                        map.get("anchor-style"));
            }
            config.setAnchorStyle(style);
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

    private static Integer parseMinItems(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 1) {
                LOG.error("tableOfContents widget: min-items must be >= 1, got '{}'", raw);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            LOG.error("tableOfContents widget: min-items must be an integer, got '{}'", raw);
            return null;
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public enum AnchorStyle {
        GITHUB, GITLAB, BITBUCKET;

        public static AnchorStyle from(String raw) {
            if (raw == null) {
                return null;
            }
            switch (raw.trim().toLowerCase()) {
                case "github":
                    return GITHUB;
                case "gitlab":
                    return GITLAB;
                case "bitbucket":
                    return BITBUCKET;
                default:
                    return null;
            }
        }
    }

    protected static class Config {
        private String title;
        private boolean ordered;
        private int minDepth = 2;
        private int maxDepth = 6;
        private int minItems = 1;
        private AnchorStyle anchorStyle = AnchorStyle.GITHUB;

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

        public int getMinItems() {
            return minItems;
        }

        public void setMinItems(int minItems) {
            this.minItems = minItems;
        }

        public AnchorStyle getAnchorStyle() {
            return anchorStyle;
        }

        public void setAnchorStyle(AnchorStyle anchorStyle) {
            this.anchorStyle = anchorStyle;
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

        /**
         * Builds the anchor slug for this header in the configured {@link AnchorStyle}.
         *
         * <p>Markdown decoration ({@code [text](url)}, bold/italic, inline code, strikethrough,
         * raw HTML tags, bare URLs) is stripped before slugification. The three styles diverge
         * in how punctuation, casing, and the {@code markdown-header-} prefix are handled —
         * matched against each platform's known anchor algorithm.
         *
         * @return the anchor slug for this header; empty string if the title is blank.
         */
        public String getAnchor() {
            if (title == null || title.trim().isEmpty()) {
                return "";
            }

            String cleaned = title
                    .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1") // ссылки [text](url) -> text
                    .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // жирный текст **text** -> text
                    .replaceAll("\\*([^*]+)\\*", "$1") // курсив *text* -> text
                    .replaceAll("`([^`]+)`", "$1") // код `text` -> text
                    .replaceAll("~~([^~]+)~~", "$1") // зачеркнутый текст ~~text~~ -> text
                    .replaceAll("<[^>]+>", "") // Удаляем HTML-теги
                    .replaceAll("https?://\\S+", "") // Удаляем URL
                    .replaceAll("www\\.\\S+", ""); // Удаляем URL

            AnchorStyle style = config.getAnchorStyle() == null ? AnchorStyle.GITHUB : config.getAnchorStyle();
            switch (style) {
                case GITLAB:
                    return cleaned.toLowerCase()
                            .replaceAll("[^\\p{L}\\p{N}\\p{M}\\s_-]", "")
                            .replaceAll("\\s+", "-")
                            .replaceAll("^-+|-+$", "");
                case BITBUCKET:
                    return "markdown-header-" + cleaned.toLowerCase()
                            .replaceAll("[^\\p{L}\\p{N}\\p{M}\\s-]", "")
                            .replaceAll("\\s+", "-")
                            .replaceAll("-{2,}", "-")
                            .replaceAll("^-+|-+$", "");
                case GITHUB:
                default:
                    return cleaned.toLowerCase()
                            .replaceAll("[^\\p{L}\\p{N}\\p{M}\\s_-]", "")
                            .replaceAll("\\s+", "-")
                            .replaceAll("_", "-")
                            .replaceAll("-{2,}", "-")
                            .replaceAll("^-+|-+$", "");
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
