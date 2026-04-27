package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@code ${widget:import(path='...' | url='...', ...)}} — embeds another file (local or
 * remote) into the current source body, with optional sub-region selection, dedent,
 * heading-offset shift, recursive sub-generation, and code-fence wrapping.
 *
 * <p>The pipeline is fixed: validate → read raw bytes → apply {@code lines}/{@code region}
 * selection → dedent → run sub-{@link Generator} (when {@code run-generator='true'}, the
 * default for {@code .src.md} sources) → shift headings → wrap in fence. Sub-generators
 * inherit parent properties and run with {@code rootGenerator=false} so escapes and
 * metadata survive for the parent's final pass.
 *
 * <p>Remote imports are gated behind {@code <!--@nrg.allowRemoteImports=true-->} and only
 * accept HTTP(S). Caching, request timeouts, and SHA-256 pinning are configured per
 * occurrence; see {@link RemoteFetcher} for the freshness/fallback semantics.
 */
public class ImportWidget extends DefaultWidget {

    private final RemoteFetcher fetcher;

    public ImportWidget() {
        this(new RemoteFetcher(new HttpUrlOpener(), Clock.systemUTC()));
    }

    ImportWidget(RemoteFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Config widgetConfig = getConfig(widgetTag.getParameters());
        try {
            return render(config, widgetConfig, language);
        } catch (IOException e) {
            throw new RuntimeException("import: I/O error reading "
                    + (widgetConfig.getPath() != null ? widgetConfig.getPath() : widgetConfig.getUrl())
                    + ": " + e.getMessage(), e);
        }
    }

    private String render(GeneratorConfig config, Config widgetConfig, String language) throws IOException {
        // 1. Validate parameter combinations
        if (widgetConfig.getLines() != null && widgetConfig.getRegion() != null) {
            throw new RuntimeException("import: 'lines' and 'region' are mutually exclusive (path=" + widgetConfig.getPath() + ")");
        }
        if (!isValidBool(widgetConfig.getWrap())) {
            throw new RuntimeException("import: invalid 'wrap' value '" + widgetConfig.getWrap() + "' (expected true|false)");
        }
        if (!isValidTriState(widgetConfig.getDedent())) {
            throw new RuntimeException("import: invalid 'dedent' value '" + widgetConfig.getDedent() + "' (expected auto|true|false)");
        }
        if (widgetConfig.getRegion() != null && !widgetConfig.getRegion().matches("[A-Za-z0-9_-]+")) {
            throw new RuntimeException("import: invalid region name '" + widgetConfig.getRegion() + "'");
        }
        int headingOffset = 0;
        if (widgetConfig.getHeadingOffset() != null) {
            try {
                headingOffset = Integer.parseInt(widgetConfig.getHeadingOffset().trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException("import: heading-offset must be an integer, got '"
                        + widgetConfig.getHeadingOffset() + "'");
            }
            if (headingOffset != 0 && "true".equals(widgetConfig.getWrap())) {
                throw new RuntimeException("import: heading-offset cannot be combined with wrap='true'");
            }
        }

        // Validate that path and url are not both set, and at least one is set.
        if (widgetConfig.getPath() != null && widgetConfig.getUrl() != null) {
            throw new RuntimeException("import: 'path' and 'url' are mutually exclusive");
        }
        if (widgetConfig.getPath() == null && widgetConfig.getUrl() == null) {
            throw new RuntimeException("import: either 'path' or 'url' is required");
        }

        // 2. Read raw bytes
        Charset charset;
        try {
            charset = Charset.forName(widgetConfig.getCharset());
        } catch (Exception e) {
            throw new RuntimeException("import: invalid charset '" + widgetConfig.getCharset() + "'", e);
        }

        File sourceFile;
        byte[] rawBytes;
        if (widgetConfig.getPath() != null) {
            sourceFile = new File(config.getSourceFile().getParentFile(), widgetConfig.getPath());
            if (!sourceFile.exists()) {
                throw new RuntimeException("import: file not found: " + sourceFile.getAbsolutePath());
            }
            rawBytes = FileUtils.readFileToByteArray(sourceFile);
        } else {
            if (!config.isAllowRemoteImports()) {
                throw new RuntimeException("import: remote URLs disabled — set <!--@nrg.allowRemoteImports=true--> to enable (url=" + widgetConfig.getUrl() + ")");
            }
            if (config.isRequireSha256ForRemote() && widgetConfig.getSha256() == null) {
                throw new RuntimeException("import: sha256 is required for remote imports under nrg.requireSha256ForRemote=true (url=" + widgetConfig.getUrl() + ")");
            }
            if (widgetConfig.getSha256() != null && !widgetConfig.getSha256().matches("[0-9a-f]{64}")) {
                throw new RuntimeException("import: sha256 must be 64 lowercase hex chars (got '" + widgetConfig.getSha256() + "')");
            }
            long timeoutMs;
            long cacheTtlMs;
            try {
                timeoutMs = DurationParser.parseMillis(widgetConfig.getTimeout());
                cacheTtlMs = DurationParser.parseMillis(widgetConfig.getCache());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("import: " + e.getMessage(), e);
            }
            if (timeoutMs == DurationParser.DISABLED) {
                throw new RuntimeException("import: timeout cannot be 'none'");
            }

            RemoteFetchSpec spec = new RemoteFetchSpec(
                    widgetConfig.getUrl(), timeoutMs, cacheTtlMs, widgetConfig.getSha256());
            try {
                rawBytes = fetcher.fetch(spec, config.getCacheDir());
            } catch (IOException e) {
                throw new RuntimeException("import: " + e.getMessage(), e);
            }
            // Synthesize a file for downstream code (sub-Generator, ImportLanguageDetector, region not-found message).
            String urlPath;
            try {
                urlPath = new URI(widgetConfig.getUrl()).getPath();
                if (urlPath == null || urlPath.isEmpty()) urlPath = "remote";
            } catch (URISyntaxException e) {
                urlPath = "remote";
            }
            sourceFile = new File(config.getSourceFile().getParentFile(), new File(urlPath).getName());
        }

        List<String> rawLines = new ArrayList<>(Arrays.asList(new String(rawBytes, charset).split("\\R", -1)));
        // split with limit -1 preserves trailing empty strings; trim trailing empty if file ends with newline
        if (!rawLines.isEmpty() && rawLines.get(rawLines.size() - 1).isEmpty()) {
            rawLines.remove(rawLines.size() - 1);
        }

        // 3. Apply lines / region selection (raw text)
        List<String> selected;
        if (widgetConfig.getLines() != null) {
            ImportLinesSpec spec;
            try {
                spec = ImportLinesSpec.parse(widgetConfig.getLines());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("import: " + e.getMessage(), e);
            }
            selected = spec.apply(rawLines);
        } else if (widgetConfig.getRegion() != null) {
            selected = ImportRegionExtractor.extract(rawLines, widgetConfig.getRegion());
            if (selected == null) {
                String sourceLabel = widgetConfig.getPath() != null ? widgetConfig.getPath() : widgetConfig.getUrl();
                throw new RuntimeException("import: region '" + widgetConfig.getRegion()
                        + "' not found or unclosed in " + sourceLabel);
            }
        } else {
            selected = rawLines;
        }

        // 4. Apply dedent
        boolean dedent = resolveAuto(widgetConfig.getDedent(), hasSelection(widgetConfig));
        if (dedent) {
            selected = ImportDedenter.dedent(selected);
        }

        // 5. Run-generator processing (if enabled) — over the joined extracted text
        String content = String.join(System.lineSeparator(), selected);
        if (widgetConfig.isRunGenerator()) {
            Generator subGenerator = new InMemoryImportedFileGenerator(sourceFile, content, widgetConfig, config);
            GenerationResult subResult = subGenerator.getResult(language);
            content = subResult == null ? "" : subResult.getContent().toString();
        }

        // 5b. Apply heading-offset (post-sub-generator so chained imports / language blocks
        //     / variable substitution all see the shift).
        if (headingOffset != 0) {
            ImportHeadingShifter.Result shifted = ImportHeadingShifter.shift(content, headingOffset);
            content = shifted.content;
            if (shifted.clampedCount > 0) {
                String src = widgetConfig.getPath() != null ? widgetConfig.getPath() : widgetConfig.getUrl();
                String signedOffset = headingOffset > 0 ? "+" + headingOffset : String.valueOf(headingOffset);
                LOG.warn("import: heading-offset clamped {} heading(s) in {} (offset={}, e.g. '{}')",
                        shifted.clampedCount, src, signedOffset, shifted.firstClampedLine);
            }
        }

        // 6. Wrap in fence (if enabled)
        if ("true".equals(widgetConfig.getWrap())) {
            // Strip any trailing newline so the closing fence hugs the content
            if (content.endsWith(System.lineSeparator())) {
                content = content.substring(0, content.length() - System.lineSeparator().length());
            }
            String lang = "auto".equals(widgetConfig.getLang())
                    ? ImportLanguageDetector.detectFromFilename(sourceFile.getName())
                    : widgetConfig.getLang();
            return "```" + lang + System.lineSeparator() + content + System.lineSeparator() + "```";
        }
        return content;
    }

    private static boolean hasSelection(Config c) {
        return c.getLines() != null || c.getRegion() != null;
    }

    private static boolean resolveAuto(String triState, boolean autoMeans) {
        if ("true".equals(triState)) return true;
        if ("false".equals(triState)) return false;
        return autoMeans;
    }

    private static boolean isValidTriState(String value) {
        return "auto".equals(value) || "true".equals(value) || "false".equals(value);
    }

    private static boolean isValidBool(String value) {
        return "true".equals(value) || "false".equals(value);
    }

    private Config getConfig(String parameters) {
        Config config = new Config();
        Map<String, String> map = NRGUtil.parseParametersLine(parameters);

        if (map.containsKey("path")) {
            config.setPath(map.get("path"));
        }
        if (map.containsKey("charset")) {
            config.setCharset(map.get("charset"));
        }
        if (map.containsKey("run-generator")) {
            config.setRunGenerator(Boolean.parseBoolean(map.get("run-generator")));
        }
        if (map.containsKey("lines")) {
            config.setLines(map.get("lines"));
        }
        if (map.containsKey("region")) {
            config.setRegion(map.get("region"));
        }
        if (map.containsKey("wrap")) {
            config.setWrap(map.get("wrap"));
        }
        if (map.containsKey("lang")) {
            config.setLang(map.get("lang"));
        }
        if (map.containsKey("dedent")) {
            config.setDedent(map.get("dedent"));
        }
        if (map.containsKey("heading-offset")) {
            config.setHeadingOffset(map.get("heading-offset"));
        }
        if (map.containsKey("url")) {
            config.setUrl(map.get("url"));
        }
        if (map.containsKey("cache")) {
            config.setCache(map.get("cache"));
        }
        if (map.containsKey("timeout")) {
            config.setTimeout(map.get("timeout"));
        }
        if (map.containsKey("sha256")) {
            config.setSha256(map.get("sha256"));
        }

        if (map.containsKey("url") && !map.containsKey("run-generator")) {
            String urlValue = map.get("url");
            boolean isSrcMd = urlValue != null && extractUrlPath(urlValue).endsWith(".src.md");
            config.setRunGenerator(isSrcMd);
        }

        return config;
    }

    private static String extractUrlPath(String url) {
        try {
            String p = new URI(url).getPath();
            return p == null ? "" : p;
        } catch (URISyntaxException e) {
            return "";
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {

        private String path;
        private String charset = "UTF-8";
        private boolean runGenerator = true;
        private String lines;            // null if not set
        private String region;           // null if not set
        private String wrap = "false";   // "true" | "false"
        private String lang = "auto";    // "auto" | explicit language string
        private String dedent = "auto";  // "auto" | "true" | "false"
        private String headingOffset;    // null if not set (treated as 0)
        private String url;              // null if not set
        private String cache = "none";   // "<int>{s,m,h,d}" or "none"
        private String timeout = "60s";
        private String sha256;           // null if not set

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public boolean isRunGenerator() {
            return runGenerator;
        }

        public void setRunGenerator(boolean runGenerator) {
            this.runGenerator = runGenerator;
        }

        public String getLines() {
            return lines;
        }

        public void setLines(String lines) {
            this.lines = lines;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getWrap() {
            return wrap;
        }

        public void setWrap(String wrap) {
            this.wrap = wrap;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getDedent() {
            return dedent;
        }

        public void setDedent(String dedent) {
            this.dedent = dedent;
        }

        public String getHeadingOffset() {
            return headingOffset;
        }

        public void setHeadingOffset(String headingOffset) {
            this.headingOffset = headingOffset;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCache() {
            return cache;
        }

        public void setCache(String cache) {
            this.cache = cache;
        }

        public String getTimeout() {
            return timeout;
        }

        public void setTimeout(String timeout) {
            this.timeout = timeout;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class InMemoryImportedFileGenerator extends Generator {
        InMemoryImportedFileGenerator(File sourceFile, String body, Config widgetConfig, GeneratorConfig parentConfig) {
            super(sourceFile, body);
            getConfig().setRootGenerator(false);
            getConfig().setLanguages(parentConfig.getLanguages());
            getConfig().setDefaultLanguage(parentConfig.getDefaultLanguage());
            getConfig().setRootSourceFile(parentConfig.getRootSourceFile());
            parentConfig.getProperties().forEach((key, value) -> {
                if (!getConfig().getProperties().containsKey(key)) {
                    getConfig().getProperties().setProperty(key.toString(), String.valueOf(value));
                }
            });
        }

        @Override
        protected String generateHeadComment(String language) {
            return "";
        }
    }
}
