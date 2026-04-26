package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.apache.commons.text.TextStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

public class TemplateLine {

    public static final Pattern WIDGET_TAG_PATTERN = Pattern.compile("\\$\\{ *widget:(\\w*)(\\(([^)]*)\\))? *}");
    private static final Pattern ENV_TAG_PATTERN = Pattern.compile("\\$\\{\\s*env\\.([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?\\s*}");
    private static final Pattern POM_TAG_PATTERN = Pattern.compile("\\$\\{\\s*pom\\.([A-Za-z_][A-Za-z0-9_.\\-]*?)(?::([^}]*))?\\s*}");
    private static final Pattern POM_INTERNAL_REF = Pattern.compile("\\$\\{\\s*([A-Za-z_][A-Za-z0-9_.\\-]*)(?::([^}]*))?\\s*}");
    private static final Pattern NPM_TAG_PATTERN =
            Pattern.compile("\\$\\{\\s*npm\\.([A-Za-z_][A-Za-z0-9_.\\-@/]*?)(?::([^}]*))?\\s*}");
    private static final Pattern GRADLE_TAG_PATTERN =
            Pattern.compile("\\$\\{\\s*gradle\\.([A-Za-z_][A-Za-z0-9_.\\-]*?)(?::([^}]*))?\\s*}");
    private static final java.util.Set<String> POM_INHERITED_FIELDS =
            new java.util.HashSet<>(java.util.Arrays.asList("groupId", "version", "name"));

    private final GeneratorConfig config;
    private final String line;
    private final int lineNumber;

    public TemplateLine(GeneratorConfig config, String line, int lineNumber) {
        this.config = config;
        this.line = line;
        this.lineNumber = lineNumber;
    }

    public String removeNrgDataFromText(String line) {
        for (String language : config.getLanguages()) {
            line = line.replaceAll("<!-- *" + Pattern.quote(language) + " *-->", "");
        }
        return line.replaceAll("<!-- *@.*-->", "");
    }

    public boolean isLineVisible(String language) {
        boolean hasLanguageMark = config.getLanguages().stream().anyMatch(s -> line.matches(".*<!-- *" + Pattern.quote(s) + " *-->.*"));
        boolean hasCurrentLanguageMark = line.matches(".*<!-- *" + Pattern.quote(language) + " *-->.*");
        boolean hasOnlyPropertyDefinition = line.matches("\\W*<!-- *@.*-->\\W*");

        return (!hasLanguageMark || hasCurrentLanguageMark) && !hasOnlyPropertyDefinition;
    }

    public Properties readProperties(String language) {
        String[] strings = substringsBetween(line, "<!--", "-->");
        if (strings != null) {
            for (String comment : strings) {
                String s = trimToEmpty(comment);
                if (s.contains("@")) {
                    s = substringAfter(s, "@");
                    if (s.contains("=")) {
                        String key = trimToEmpty(substringBefore(s, "="));
                        String value = trimToEmpty(substringAfter(s, "="));
                        value = renderEnvProperties(value);
                        value = renderPomProperties(value);
                        value = renderNpmProperties(value);
                        value = renderGradleProperties(value);
                        value = renderProperties(value);
                        value = renderLanguageProperties(value, language);
                        NRGUtil.mergeProperty(key, value, config.getProperties());
                    } else {
                        String key = trimToEmpty(s);
                        NRGUtil.mergeProperty(key, "", config.getProperties());
                    }
                }
            }
        }
        return config.getProperties();
    }

    public String getProperty(String property, String language) {
        return readProperties(language).getProperty(property);
    }

    protected String renderWidgets(String line, String language) {
        TextStringBuilder result = new TextStringBuilder(line);

        config.getWidgets()
                .stream().filter(NRGWidget::isEnabled)
                .forEach(w -> w.beforeRenderLine(result));

        int shift = 0;
        for (WidgetTag tag : getWidgetTags(line)) {
            NRGWidget widget = config.getWidget(tag.getName());
            if (widget != null) {
                if (widget.isEnabled()) {
                    try {
                        String body = widget.getBody(tag, config, language);
                        result.replace(shift + tag.getStart(), shift + tag.getEnd(), body);
                        shift += body.length() - (tag.getEnd() - tag.getStart());
                    }
                    // Unchecked exceptions from widgets must propagate to the user; only the catch-all
                    // below should swallow truly unexpected Throwables (Errors, Throwables, etc).
                    catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable e) {
                        LOG.error(e, "Can't render widget '" + tag.getName() + "' at the line #" + lineNumber + " (" + this.line + ")");
                    }
                }
            } else {
                LOG.warn("Unknown widget name: {}", tag.getName());
            }
        }

        config.getWidgets()
                .stream().filter(NRGWidget::isEnabled)
                .forEach(w -> w.afterRenderLine(result, config));

        return result.toString();
    }

    protected List<WidgetTag> getWidgetTags(String line) {
        ArrayList<WidgetTag> res = new ArrayList<>();
        Matcher m = WIDGET_TAG_PATTERN.matcher(line);

        while (m.find()) {
            if (isNotEscaped(line, m.start())) {
                res.add(new WidgetTag(this, m.group(1), m.group(3), m.start(), m.end()));
            }
        }

        return res;
    }

    protected String renderEnvProperties(String line) {
        Matcher m = ENV_TAG_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (m.find()) {
            if (!isNotEscaped(line, m.start())) {
                continue;
            }
            String name = m.group(1);
            String fallback = m.group(2);
            String value = config.getEnvProvider().apply(name);
            String replacement;
            if (value != null) {
                replacement = value;
            } else if (fallback != null) {
                replacement = fallback;
            } else {
                if (config.getWarnedMissingEnvVars().add(name)) {
                    LOG.warn("Environment variable '{}' is not set; rendering empty", name);
                }
                replacement = "";
            }
            result.append(line, last, m.start()).append(replacement);
            last = m.end();
        }
        result.append(line, last, line.length());
        return result.toString();
    }

    protected String renderPomProperties(String line) {
        Matcher m = POM_TAG_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (m.find()) {
            if (!isNotEscaped(line, m.start())) {
                continue;
            }
            String path = m.group(1);
            String fallback = m.group(2);
            String value = resolvePomPath(path);
            String replacement;
            if (value != null) {
                replacement = value;
            } else if (fallback != null) {
                replacement = fallback;
            } else {
                if (config.getWarnedMissingPomPaths().add(path)) {
                    LOG.warn("POM path '{}' is not present; rendering empty", path);
                }
                replacement = "";
            }
            result.append(line, last, m.start()).append(replacement);
            last = m.end();
        }
        result.append(line, last, line.length());
        return result.toString();
    }

    private String resolvePomPath(String path) {
        PomReader reader = config.getPomReader();
        if (reader == null) {
            return null;
        }
        java.util.Optional<String> raw = reader.read(path);
        if (!raw.isPresent() && POM_INHERITED_FIELDS.contains(path)) {
            raw = reader.read("parent." + path);
        }
        return raw.map(s -> resolvePomInternalReferences(s, reader)).orElse(null);
    }

    protected String renderNpmProperties(String line) {
        Matcher m = NPM_TAG_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (m.find()) {
            if (!isNotEscaped(line, m.start())) {
                continue;
            }
            String path = m.group(1);
            String fallback = m.group(2);
            String value = resolveNpmPath(path);
            String replacement;
            if (value != null) {
                replacement = value;
            } else if (fallback != null) {
                replacement = fallback;
            } else {
                if (config.getWarnedMissingNpmPaths().add(path)) {
                    LOG.warn("npm path '{}' is not present; rendering empty", path);
                }
                replacement = "";
            }
            result.append(line, last, m.start()).append(replacement);
            last = m.end();
        }
        result.append(line, last, line.length());
        return result.toString();
    }

    private String resolveNpmPath(String path) {
        NpmReader reader = config.getNpmReader();
        if (reader == null) {
            return null;
        }
        return reader.read(path).orElse(null);
    }

    protected String renderGradleProperties(String line) {
        Matcher m = GRADLE_TAG_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (m.find()) {
            if (!isNotEscaped(line, m.start())) {
                continue;
            }
            String path = m.group(1);
            String fallback = m.group(2);
            String value = resolveGradlePath(path);
            String replacement;
            if (value != null) {
                replacement = value;
            } else if (fallback != null) {
                replacement = fallback;
            } else {
                if (config.getWarnedMissingGradlePaths().add(path)) {
                    LOG.warn("gradle path '{}' is not present; rendering empty", path);
                }
                replacement = "";
            }
            result.append(line, last, m.start()).append(replacement);
            last = m.end();
        }
        result.append(line, last, line.length());
        return result.toString();
    }

    private String resolveGradlePath(String path) {
        GradleReader reader = config.getGradleReader();
        if (reader == null) {
            return null;
        }
        return reader.read(path).orElse(null);
    }

    private String resolvePomInternalReferences(String value, PomReader reader) {
        Matcher m = POM_INTERNAL_REF.matcher(value);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (m.find()) {
            if (!isNotEscaped(value, m.start())) {
                continue;
            }
            String name = m.group(1);
            String fallback = m.group(2);
            String resolved = resolvePomInternalName(name, reader);
            if (resolved == null && fallback != null) {
                resolved = fallback;
            }
            if (resolved == null) {
                continue;
            }
            result.append(value, last, m.start()).append(resolved);
            last = m.end();
        }
        result.append(value, last, value.length());
        return result.toString();
    }

    private String resolvePomInternalName(String name, PomReader reader) {
        if (name.startsWith("env.")) {
            String envName = name.substring("env.".length());
            String envValue = config.getEnvProvider().apply(envName);
            return envValue;
        }
        if (name.startsWith("project.")) {
            String sub = name.substring("project.".length());
            java.util.Optional<String> v = reader.read(sub);
            if (!v.isPresent() && POM_INHERITED_FIELDS.contains(sub)) {
                v = reader.read("parent." + sub);
            }
            return v.orElse(null);
        }
        java.util.Optional<String> v = reader.read("properties." + name);
        return v.orElse(null);
    }

    protected String renderProperties(String line) {
        Pattern pattern = Pattern.compile("\\$\\{\\s*([\\p{Alnum}-_.]+)\\s*}");
        Matcher m = pattern.matcher(line);
        while (m.find()) {
            if (isNotEscaped(line, m.start())) {
                String propertyName = m.group(1);
                if (config.getProperties().containsKey(propertyName)) {
                    String value = config.getProperties().getProperty(propertyName);
                    line = (m.start() > 0 ? line.substring(0, m.start()) : "") +
                            value +
                            (m.end() < line.length() ? line.substring(m.end()) : "");
                    m = pattern.matcher(line);
                }
            }
        }

        return line;
    }

    private boolean isNotEscaped(String line, int start) {
        return start == 0 || line.charAt(start - 1) != '\\';
    }

    protected String renderLanguageProperties(String line, String language) {

        if (language == null) {
            return line;
        }

        String langAlt = config.getLanguages().stream().map(Pattern::quote).collect(Collectors.joining("|"));
        if (langAlt.isEmpty()) {
            return line;
        }
        String quoted = "(?:'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\")";
        String entry = "\\s*(?:" + langAlt + ")\\s*:\\s*" + quoted + "\\s*";
        String regex = "\\$\\{(" + entry + "(?:," + entry + ")*)}";

        StringBuilder result = new StringBuilder(line);
        int shift = 0;
        Matcher matcher = Pattern.compile(regex).matcher(line);
        while (matcher.find()) {
            if (isNotEscaped(line, matcher.start())) {
                String body = "";

                String content = matcher.group(1);
                String ENTRY_PATTERN = "(\\s*LANG: *(?<quote>['\"])(?<text>(?:(?!\\k<quote>).|\\k<quote>\\k<quote>)*)\\k<quote>[,\\s]*)"
                        .replace("LANG", Pattern.quote(language));
                Matcher entryMatcher = Pattern.compile(ENTRY_PATTERN).matcher(content);
                while (entryMatcher.find()) {
                    String quote = entryMatcher.group("quote");
                    body = entryMatcher.group("text").replace(quote + quote, quote);
                }

                result.replace(shift + matcher.start(), shift + matcher.end(), body);
                shift += body.length() - (matcher.end() - matcher.start());
            }
        }
        return result.toString();
    }


    public String fillLineWithProperties(String language) {
        return fillLineWithProperties(language, true);
    }

    public String fillLineWithProperties(String language, boolean substituteEnv) {
        if (isLineVisible(language)) {
            String result = line;
            if (substituteEnv) {
                result = renderEnvProperties(result);
                result = renderPomProperties(result);
                result = renderNpmProperties(result);
                result = renderGradleProperties(result);
            }
            result = renderLanguageProperties(result, language);
            result = renderProperties(result);
            return result;
        } else {
            return null;
        }
    }

    public String generateLine(String language) {
        if (isLineVisible(language)) {
            String result = fillLineWithProperties(language);
            result = renderWidgets(result, language);
            if (config.isRootGenerator()) {
                result = removeNrgDataFromText(result);
                result = replaceEscapedCharacters(result);
            }
            return result;
        } else {
            return null;
        }
    }

    public GeneratorConfig getConfig() {
        return config;
    }

    public String getLine() {
        return line;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    private String replaceEscapedCharacters(String line) {
        return line
                .replace("\\$", "$")
                .replace("<\\!--", "<!--")
                .replace("<!--\\@", "<!--@");
    }
}
