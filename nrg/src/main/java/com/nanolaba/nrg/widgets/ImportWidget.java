package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.sugar.Code;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ImportWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Config widgetConfig = getConfig(widgetTag.getParameters());
        return Code.run(() -> render(config, widgetConfig, language));
    }

    private String render(GeneratorConfig config, Config widgetConfig, String language) throws IOException {
        // 1. Validate parameter combinations
        if (widgetConfig.getLines() != null && widgetConfig.getRegion() != null) {
            LOG.error("import widget: 'lines' and 'region' are mutually exclusive (path={})", widgetConfig.getPath());
            return "";
        }
        if (!isValidBool(widgetConfig.getWrap())) {
            LOG.error("import widget: invalid 'wrap' value '{}' (expected true|false)", widgetConfig.getWrap());
            return "";
        }
        if (!isValidTriState(widgetConfig.getDedent())) {
            LOG.error("import widget: invalid 'dedent' value '{}' (expected auto|true|false)", widgetConfig.getDedent());
            return "";
        }
        if (widgetConfig.getRegion() != null && !widgetConfig.getRegion().matches("[A-Za-z0-9_-]+")) {
            LOG.error("import widget: invalid region name '{}'", widgetConfig.getRegion());
            return "";
        }

        File sourceFile = new File(config.getSourceFile().getParentFile(), widgetConfig.getPath());
        if (!sourceFile.exists()) {
            LOG.error("import widget: file not found: {}", sourceFile.getAbsolutePath());
            return "";
        }

        // 2. Read raw lines
        Charset charset;
        try {
            charset = Charset.forName(widgetConfig.getCharset());
        } catch (Exception e) {
            LOG.error(e, "import widget: invalid charset '" + widgetConfig.getCharset() + "'");
            return "";
        }
        List<String> rawLines = new ArrayList<>(Arrays.asList(
                FileUtils.readFileToString(sourceFile, charset).split("\\R", -1)));
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
                LOG.error("import widget: " + e.getMessage());
                return "";
            }
            selected = spec.apply(rawLines);
        } else if (widgetConfig.getRegion() != null) {
            selected = ImportRegionExtractor.extract(rawLines, widgetConfig.getRegion());
            if (selected == null) {
                LOG.error("import widget: region '{}' not found or unclosed in {}",
                        widgetConfig.getRegion(), widgetConfig.getPath());
                return "";
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

        return config;
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
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class InMemoryImportedFileGenerator extends Generator {
        InMemoryImportedFileGenerator(File sourceFile, String body, Config widgetConfig, GeneratorConfig parentConfig) {
            super(sourceFile, body);
            getConfig().setRootGenerator(false);
            getConfig().setLanguages(parentConfig.getLanguages());
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
