package com.nanolaba.nrg.widgets;

import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;
import com.nanolaba.sugar.Code;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class ImportWidget extends DefaultWidget {

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {

        Config widgetConfig = getConfig(widgetTag.getParameters());

        return Code.run(() -> getReadFileToString(config, widgetConfig, language));
    }

    protected String getReadFileToString(GeneratorConfig config, Config widgetConfig, String language) throws IOException {
        File sourceFile = new File(config.getSourceFile().getParentFile(), widgetConfig.getPath());

        if (widgetConfig.isRunGenerator()) {
            Generator generator = new Generator(sourceFile, Charset.forName(widgetConfig.getCharset())) {
                @Override
                protected String generateHeadComment(String language) {
                    return "";
                }
            };

            config.getProperties().forEach((key, value) -> {
                if (!generator.getConfig().getProperties().containsKey(key)) {
                    generator.getConfig().getProperties().setProperty(key.toString(), String.valueOf(value));
                }
            });

            GenerationResult generatorResult = generator.getResult(language);
            return generatorResult == null ? "" : generatorResult.getContent().toString();
        } else {
            return FileUtils.readFileToString(sourceFile, Charset.forName(widgetConfig.getCharset()));
        }
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

        return config;
    }

    /// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class Config {

        private String path;
        private String charset = "UTF-8";
        private boolean runGenerator = true;

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
    }
}
