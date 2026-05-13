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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *
 * <p>When {@code ordered='true'}, the optional {@code numbering-style} parameter
 * picks one of nine prefix flavours: {@code default} (today's tab-indented {@code 1.}
 * markers, byte-identical when omitted), hierarchical {@code dotted} / {@code legal} /
 * {@code appendix} (e.g. {@code 1.2.3}, {@code 1.2.3.}, {@code A.1.2}) emitted on indented
 * unordered lists so Markdown renderers cannot re-number them, or flat global counters
 * {@code arabic} / {@code roman} / {@code roman-upper} / {@code alpha} / {@code alpha-upper}
 * on a single-level unordered list. The optional {@code start} parameter overrides the first
 * top-level counter value (digits for arabic-family styles, roman numeral for {@code roman}/
 * {@code roman-upper}, single letter for {@code alpha}-family + {@code appendix}); invalid
 * values log an error and fall back to the natural first.
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
    public Set<String> getAliases() {
        return Collections.singleton("toc");
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

        NumberingFormatter formatter = new NumberingFormatter(tocConfig.getNumberingStyle(), tocConfig.getStart());

        if (tocConfig.getNumberingStyle() != NumberingStyle.DEFAULT && !tocConfig.isOrdered()) {
            LOG.warn("tableOfContents widget: numbering-style='{}' has no effect without ordered='true'; rendering as unordered list",
                    tocConfig.getNumberingStyle().name().toLowerCase().replace('_', '-'));
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
            // Fallback for the short alias form.
            indexOfTOC = NRGUtil.findFirstUnescapedOccurrenceLine(sourceFileBody, "${widget:toc");
        }
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

                int flatIndex = items.size() + 1;
                Header header = new Header(rendered, tocConfig, allHeaders, formatter, flatIndex);
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
        if (map.containsKey("numbering-style")) {
            NumberingStyle style = NumberingStyle.from(map.get("numbering-style"));
            if (style == null) {
                LOG.error("tableOfContents widget: unknown numbering-style '{}' (expected default|dotted|legal|appendix|arabic|roman|roman-upper|alpha|alpha-upper); falling back to default",
                        map.get("numbering-style"));
            } else {
                config.setNumberingStyle(style);
            }
        }
        if (map.containsKey("start")) {
            config.setStart(map.get("start"));
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

    /**
     * Selects the visible counter prefix shape for ordered TOCs. {@code DEFAULT} preserves
     * pre-#43 byte-identical output (tab-indented Markdown {@code 1.} markers); the other
     * values pivot to NRG-controlled counters rendered as unordered {@code -} list items
     * so GitHub/GitLab/Bitbucket cannot re-number them.
     *
     * <p>Hierarchical styles ({@link #DOTTED}, {@link #LEGAL}, {@link #APPENDIX}) produce
     * indented {@code 1.2.3}-shape prefixes that reset deeper counters when an ancestor
     * advances. Flat styles ({@link #ARABIC}, {@link #ROMAN}, {@link #ROMAN_UPPER},
     * {@link #ALPHA}, {@link #ALPHA_UPPER}) emit a single global counter on a flat list,
     * regardless of heading depth. {@link #from(String)} returns {@code null} for unknown
     * input so callers can log + fall back without exceptions.
     */
    public enum NumberingStyle {
        DEFAULT, DOTTED, LEGAL, APPENDIX, ARABIC, ROMAN, ROMAN_UPPER, ALPHA, ALPHA_UPPER;

        public static NumberingStyle from(String raw) {
            if (raw == null) {
                return null;
            }
            switch (raw.trim().toLowerCase()) {
                case "default":
                    return DEFAULT;
                case "dotted":
                    return DOTTED;
                case "legal":
                    return LEGAL;
                case "appendix":
                    return APPENDIX;
                case "arabic":
                    return ARABIC;
                case "roman":
                    return ROMAN;
                case "roman-upper":
                    return ROMAN_UPPER;
                case "alpha":
                    return ALPHA;
                case "alpha-upper":
                    return ALPHA_UPPER;
                default:
                    return null;
            }
        }

        public boolean isFlat() {
            return this == ARABIC || this == ROMAN || this == ROMAN_UPPER
                    || this == ALPHA || this == ALPHA_UPPER;
        }
    }

    protected static class Config {
        private String title;
        private boolean ordered;
        private int minDepth = 2;
        private int maxDepth = 6;
        private int minItems = 1;
        private AnchorStyle anchorStyle = AnchorStyle.GITHUB;
        private NumberingStyle numberingStyle = NumberingStyle.DEFAULT;
        private String start;

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

        public NumberingStyle getNumberingStyle() {
            return numberingStyle;
        }

        public void setNumberingStyle(NumberingStyle numberingStyle) {
            this.numberingStyle = numberingStyle;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Renders the visible counter prefix for a single TOC entry under the chosen
     * {@link NumberingStyle}. Stateless beyond the immutable style + start offset —
     * each call receives the per-entry counter array (for hierarchical styles) or
     * the global flat index (for flat styles).
     *
     * <p>Type-validates the {@code start} parameter against the style: digits for
     * dotted/legal/arabic, roman numeral for roman/roman-upper, single letter for
     * alpha/alpha-upper/appendix. A type mismatch logs an error once and silently
     * falls back to the natural first value (1 / I / a / A).
     */
    protected static class NumberingFormatter {
        private final NumberingStyle style;
        private final int startOffset; // added to each top-level counter; 0 means natural first

        public NumberingFormatter(NumberingStyle style, String rawStart) {
            this.style = style;
            this.startOffset = parseStart(style, rawStart);
        }

        public boolean isFlat() {
            return style.isFlat();
        }

        public boolean isDefault() {
            return style == NumberingStyle.DEFAULT;
        }

        /** List marker emitted before the counter prefix. {@code "1."} for default, {@code "-"} otherwise. */
        public String marker() {
            return style == NumberingStyle.DEFAULT ? "1." : "-";
        }

        /** Trailing punctuation after the counter prefix (e.g. {@code "."} for legal). */
        public String suffixAfterPrefix() {
            return style == NumberingStyle.LEGAL ? "." : "";
        }

        /**
         * Hierarchical counter prefix (e.g. {@code "1.2.3"}, {@code "A.1.2"}).
         * Caller passes the full counter path indexed from the visible top.
         */
        public String hierarchicalPrefix(int[] counters) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < counters.length; i++) {
                if (i > 0) {
                    sb.append('.');
                }
                int value = counters[i] + (i == 0 ? startOffset : 0);
                if (i == 0 && style == NumberingStyle.APPENDIX) {
                    sb.append(toAlpha(value, true));
                } else {
                    sb.append(value);
                }
            }
            return sb.toString();
        }

        /** Flat counter rendering (1-based index, with {@code start} offset applied). */
        public String flatPrefix(int oneBasedIndex) {
            int value = oneBasedIndex + startOffset;
            switch (style) {
                case ARABIC:
                    return Integer.toString(value);
                case ROMAN:
                    return toRoman(value, false);
                case ROMAN_UPPER:
                    return toRoman(value, true);
                case ALPHA:
                    return toAlpha(value, false);
                case ALPHA_UPPER:
                    return toAlpha(value, true);
                default:
                    return Integer.toString(value); // unreachable for flat
            }
        }

        /**
         * Validates {@code raw} against the chosen style and returns the offset to add to
         * the natural first counter (so {@code start='5'} for dotted yields offset 4 → first
         * top-level counter renders as 5). Returns 0 (no offset) on null/blank/invalid input
         * after logging an error. Logs a warning if {@code start} is supplied with the
         * default style, since default delegates numbering to the Markdown renderer.
         */
        private static int parseStart(NumberingStyle style, String raw) {
            if (style == NumberingStyle.DEFAULT) {
                if (raw != null) {
                    LOG.warn("tableOfContents widget: 'start' is ignored when numbering-style is 'default'");
                }
                return 0;
            }
            if (raw == null) {
                return 0;
            }
            String trimmed = raw.trim();
            try {
                switch (style) {
                    case DOTTED:
                    case LEGAL:
                    case ARABIC:
                        return requirePositive(Integer.parseInt(trimmed)) - 1;
                    case ROMAN:
                    case ROMAN_UPPER:
                        return requirePositive(fromRoman(trimmed)) - 1;
                    case ALPHA:
                    case ALPHA_UPPER:
                    case APPENDIX:
                        return requirePositive(fromAlpha(trimmed)) - 1;
                    default:
                        return 0;
                }
            } catch (NumberFormatException e) {
                LOG.error("tableOfContents widget: invalid 'start' value '{}' for numbering-style '{}'; falling back to natural first value",
                        raw, style.name().toLowerCase().replace('_', '-'));
                return 0;
            }
        }

        /** Throws {@link NumberFormatException} when {@code n < 1}; lets {@link #parseStart} share one catch arm. */
        private static int requirePositive(int n) {
            if (n < 1) {
                throw new NumberFormatException();
            }
            return n;
        }

        /** Excel-style: 1 to a, 26 to z, 27 to aa, 52 to az, 53 to ba, ... */
        private static String toAlpha(int n, boolean upper) {
            if (n < 1) {
                return upper ? "A" : "a";
            }
            StringBuilder sb = new StringBuilder();
            int x = n;
            while (x > 0) {
                int rem = (x - 1) % 26;
                sb.insert(0, (char) ((upper ? 'A' : 'a') + rem));
                x = (x - 1) / 26;
            }
            return sb.toString();
        }

        private static int fromAlpha(String s) {
            if (s == null || s.isEmpty()) {
                return -1;
            }
            int n = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = Character.toLowerCase(s.charAt(i));
                if (c < 'a' || c > 'z') {
                    return -1;
                }
                n = n * 26 + (c - 'a' + 1);
            }
            return n;
        }

        /** 1..3999. Outside that range we return Integer.toString(n) — practical TOCs never exceed it. */
        private static String toRoman(int n, boolean upper) {
            if (n < 1 || n > 3999) {
                return Integer.toString(n);
            }
            int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
            String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
            StringBuilder sb = new StringBuilder();
            int x = n;
            for (int i = 0; i < values.length; i++) {
                while (x >= values[i]) {
                    sb.append(symbols[i]);
                    x -= values[i];
                }
            }
            return upper ? sb.toString() : sb.toString().toLowerCase();
        }

        private static int fromRoman(String s) {
            if (s == null || s.isEmpty()) {
                return -1;
            }
            String upper = s.toUpperCase();
            int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
            String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
            int n = 0;
            int i = 0;
            for (int k = 0; k < symbols.length; k++) {
                while (upper.startsWith(symbols[k], i)) {
                    n += values[k];
                    i += symbols[k].length();
                }
            }
            if (i != upper.length() || n < 1) {
                return -1;
            }
            // Canonical round-trip: reject non-canonical input (e.g. "IIII" → 4 silently)
            // so a typoed start logs a warning instead of being mis-interpreted.
            if (!upper.equals(toRoman(n, true))) {
                return -1;
            }
            return n;
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Header {

        public static final Pattern HEADER_PATTERN = Pattern.compile("(#+)(.*)");
        private String title;
        private int level;
        // Sibling counter for the legacy DEFAULT path of toString(); computed but unread for every other style.
        private int number;
        private int[] counters = new int[0];
        private final List<Header> headers;
        private final Config config;
        private final NumberingFormatter formatter;
        // 1-based index across visible (post-depth-clip) headers; ignored for depth-clipped headers.
        private final int flatIndex;

        /**
         * Backward-compatible constructor used by anchor-slug-only tests. The TOC pipeline
         * always uses the formatter-aware overload below; this entry point exists so callers
         * that only care about {@link #getAnchor()} aren't forced to plumb a formatter.
         */
        public Header(String line, Config config, List<Header> headers) {
            this(line, config, headers, null, 0);
        }

        public Header(String line, Config config, List<Header> headers, NumberingFormatter formatter, int flatIndex) {
            this.headers = headers;
            this.config = config;
            this.formatter = formatter;
            this.flatIndex = flatIndex;

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

                this.counters = computeCounters();
                headers.add(this);
            }
        }

        /**
         * Builds the hierarchical counter path for this header, indexed from the visible top
         * (i.e. the {@code min-depth} level becomes index 0). Returns an empty array for headers
         * outside the visible depth window — those entries are still tracked in {@code headers}
         * so {@link #getPreviousHeader(int)} keeps the legacy walk-back semantics intact, but
         * their counter slot is never read by hierarchical-style rendering, which is gated by
         * the same depth window.
         *
         * <p>Walks back to the most recent header inside {@code [minLevel, maxLevel]} and either
         * (a) increments the sibling slot when this header's depth equals the predecessor's,
         * (b) extends the path with fresh {@code 1}s when descending, or (c) drops back to the
         * ancestor slot and increments when ascending.
         */
        private int[] computeCounters() {
            int minLevel = config.getMinDepth() - 1;
            int maxLevel = config.getMaxDepth() - 1;
            if (level < minLevel || level > maxLevel) {
                return new int[0];
            }
            int depthFromTop = level - minLevel;
            int[] result = new int[depthFromTop + 1];

            Header prev = lastVisibleHeader(minLevel, maxLevel);
            if (prev == null) {
                for (int i = 0; i <= depthFromTop; i++) {
                    result[i] = 1;
                }
                return result;
            }

            int prevDepth = prev.counters.length;
            if (depthFromTop + 1 == prevDepth) {
                for (int i = 0; i < depthFromTop; i++) {
                    result[i] = prev.counters[i];
                }
                result[depthFromTop] = prev.counters[depthFromTop] + 1;
            } else if (depthFromTop + 1 > prevDepth) {
                for (int i = 0; i < prevDepth; i++) {
                    result[i] = prev.counters[i];
                }
                for (int i = prevDepth; i <= depthFromTop; i++) {
                    result[i] = 1;
                }
            } else {
                for (int i = 0; i < depthFromTop; i++) {
                    result[i] = prev.counters[i];
                }
                result[depthFromTop] = prev.counters[depthFromTop] + 1;
            }
            return result;
        }

        private Header lastVisibleHeader(int minLevel, int maxLevel) {
            for (int i = headers.size() - 1; i >= 0; i--) {
                Header h = headers.get(i);
                if (h.level >= minLevel && h.level <= maxLevel) {
                    return h;
                }
            }
            return null;
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

        private void appendIndent(StringBuilder sb) {
            int minLevel = config.getMinDepth() - 1;
            int depth = Math.max(0, level - minLevel);
            for (int i = 0; i < depth; i++) {
                sb.append("\t");
            }
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();

            if (formatter == null || formatter.isDefault()) {
                // legacy path — byte-identical to pre-#43 behaviour
                appendIndent(res);
                if (config.isOrdered()) {
                    res.append(number).append(".");
                } else {
                    res.append("-");
                }
                res.append(" [").append(title).append("](#").append(getAnchor()).append(")");
                res.append(System.lineSeparator());
                return res.toString();
            }

            if (!config.isOrdered()) {
                // numbering-style requested but ordered=false — emit unordered list, no prefix
                appendIndent(res);
                res.append("- [").append(title).append("](#").append(getAnchor()).append(")");
                res.append(System.lineSeparator());
                return res.toString();
            }

            if (formatter.isFlat()) {
                res.append("- ").append(formatter.flatPrefix(flatIndex))
                        .append(" [").append(title).append("](#").append(getAnchor()).append(")");
                res.append(System.lineSeparator());
                return res.toString();
            }

            // hierarchical (dotted / legal / appendix)
            appendIndent(res);
            res.append("- ").append(formatter.hierarchicalPrefix(counters))
                    .append(formatter.suffixAfterPrefix())
                    .append(" [").append(title).append("](#").append(getAnchor()).append(")");
            res.append(System.lineSeparator());
            return res.toString();
        }
    }
}
