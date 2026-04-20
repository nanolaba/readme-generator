package com.nanolaba.nrg.widgets;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class ImportLanguageDetector {

    private static final Map<String, String> EXTENSION_TO_LANG;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("java", "java");
        m.put("kt", "kotlin");
        m.put("kts", "kotlin");
        m.put("groovy", "groovy");
        m.put("gradle", "groovy");
        m.put("scala", "scala");
        m.put("js", "javascript");
        m.put("mjs", "javascript");
        m.put("cjs", "javascript");
        m.put("ts", "typescript");
        m.put("tsx", "typescript");
        m.put("py", "python");
        m.put("rb", "ruby");
        m.put("go", "go");
        m.put("rs", "rust");
        m.put("c", "c");
        m.put("h", "c");
        m.put("cpp", "cpp");
        m.put("cc", "cpp");
        m.put("hpp", "cpp");
        m.put("cs", "csharp");
        m.put("php", "php");
        m.put("swift", "swift");
        m.put("sh", "bash");
        m.put("bash", "bash");
        m.put("ps1", "powershell");
        m.put("bat", "batch");
        m.put("cmd", "batch");
        m.put("sql", "sql");
        m.put("xml", "xml");
        m.put("xsl", "xml");
        m.put("xsd", "xml");
        m.put("html", "html");
        m.put("htm", "html");
        m.put("css", "css");
        m.put("scss", "scss");
        m.put("json", "json");
        m.put("yaml", "yaml");
        m.put("yml", "yaml");
        m.put("toml", "toml");
        m.put("properties", "properties");
        m.put("md", "markdown");
        m.put("lua", "lua");
        m.put("dart", "dart");
        EXTENSION_TO_LANG = Collections.unmodifiableMap(m);
    }

    private ImportLanguageDetector() {
    }

    static String detectFromFilename(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        String lang = EXTENSION_TO_LANG.get(ext);
        return lang == null ? "" : lang;
    }
}
