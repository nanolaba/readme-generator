package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import com.nanolaba.nrg.widgets.WidgetTag;
import org.apache.commons.text.TextStringBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Per-line rendering primitive of the generation pipeline.
 *
 * <p>Each instance holds a single source line plus a back-reference to its owning
 * {@link GeneratorConfig}. {@link #generateLine(String)} is the main entry point — for the
 * given target language it sequentially:
 * <ol>
 *   <li>checks line visibility (drops {@code <!--otherLang-->}-tagged and pure
 *       property-definition lines);</li>
 *   <li>substitutes {@code ${env.NAME}}, {@code ${pom.path}}, {@code ${npm.path}},
 *       {@code ${gradle.path}} references;</li>
 *   <li>substitutes {@code ${en:'...', ru:'...'}} per-language constructs;</li>
 *   <li>substitutes {@code ${var}} property references with optional language scoping;</li>
 *   <li>renders {@code ${widget:name(params)}} via every registered {@link NRGWidget}
 *       (with {@code beforeRenderLine}/{@code afterRenderLine} hooks);</li>
 *   <li>(root generator only) strips NRG metadata and unescapes backslash-escaped tags.</li>
 * </ol>
 *
 * <p>Backslash-escaped occurrences ({@code \${...}}) are honoured everywhere via
 * {@link #isNotEscaped}.
 */
public class TemplateLine {

    /**
     * Regex source for the parameter list of a widget tag — everything between the tag's
     * parentheses. A quoted value ({@code '…'} or {@code "…"}) is consumed as a unit, so a
     * {@code )} inside it does not end the list: {@code cond='endsWith(${v}, -SNAPSHOT)'}.
     *
     * <p>A quote character with no partner later on the line is treated as plain text
     * rather than as an opening quote; tags such as {@code text='it's'} were recognised
     * before quotes became significant and must stay recognised. The alternatives are mutually
     * exclusive and the quantifier is possessive, so matching never backtracks into a
     * different split of the same text.
     */
    public static final String WIDGET_PARAMETERS_REGEX =
            "(?:[^)'\"]|'[^']*'|\"[^\"]*\"|'(?![^']*')|\"(?![^\"]*\"))*+";

    public static final Pattern WIDGET_TAG_PATTERN =
            Pattern.compile("\\$\\{ *widget:(\\w*)(\\((" + WIDGET_PARAMETERS_REGEX + ")\\))? *}");
    /**
     * Quoted text of a language construct ({@code ${en:'…', ru:"…"}}) with the quote doubled
     * to escape it. Written as "plain run, then (doubled quote, plain run) repeated" with
     * possessive quantifiers rather than the obvious {@code (?:[^']|'')*}: java.util.regex
     * recurses once per iteration of an alternation inside a loop, so the obvious form threw
     * {@link StackOverflowError} on a paragraph of about two thousand characters.
     */
    private static final String SINGLE_QUOTED_BODY = "[^']*+(?:''[^']*+)*+";
    private static final String DOUBLE_QUOTED_BODY = "[^\"]*+(?:\"\"[^\"]*+)*+";

    /** A line break inside a widget body that is followed by a non-empty line. */
    private static final Pattern CONTINUATION_LINE_START = Pattern.compile("(\\r\\n|\\r|\\n)(?=[^\\r\\n])");
    /** {@code <!--variant:a,!b-->}; group 1 is the list. */
    public static final Pattern VARIANT_MARKER = Pattern.compile("<!--\\s*variant:([^>]*?)\\s*-->");
    private static final Pattern ENV_TAG_PATTERN = Pattern.compile("\\$\\{\\s*env\\.([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?\\s*}");
    private static final Pattern POM_TAG_PATTERN = Pattern.compile("\\$\\{\\s*pom\\.([A-Za-z_][A-Za-z0-9_.\\-]*?)(?::([^}]*))?\\s*}");
    private static final Pattern POM_INTERNAL_REF = Pattern.compile("\\$\\{\\s*([A-Za-z_][A-Za-z0-9_.\\-]*)(?::([^}]*))?\\s*}");
    private static final Pattern NPM_TAG_PATTERN =
            Pattern.compile("\\$\\{\\s*npm\\.([A-Za-z_][A-Za-z0-9_.\\-@/]*?)(?::([^}]*))?\\s*}");
    private static final Pattern GRADLE_TAG_PATTERN =
            Pattern.compile("\\$\\{\\s*gradle\\.([A-Za-z_][A-Za-z0-9_.\\-]*?)(?::([^}]*))?\\s*}");
    private static final Set<String> POM_INHERITED_FIELDS =
            new HashSet<>(Arrays.asList("groupId", "version", "name"));

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
        line = line.replace(NRGConstants.PARITY_IGNORE_MARKER, "");
        if (line.contains("variant:")) {
            line = VARIANT_MARKER.matcher(line).replaceAll("");
        }
        return NRGUtil.stripPropertyComments(line);
    }

    /**
     * A line that carries property definitions and nothing a reader would see: once the
     * property comments and language markers are gone, no letter or digit is left.
     * Punctuation may remain — {@code - <!--@a=1-->} is still such a line.
     *
     * <p>The check used to be the regex {@code \W*<!-- *@.*-->\W*}. Its greedy middle took a
     * line like {@code <!--@a=1--> text <!--@b=2-->} for a pure definition and dropped the
     * text, and {@code \W} is ASCII-only in Java, so {@code <!--@a=1--> Привет} was dropped as
     * well.
     */
    private boolean hasOnlyPropertyDefinitions() {
        String withoutProperties = NRGUtil.stripPropertyComments(line);
        if (withoutProperties == line) {
            return false; // same instance: there was no property comment at all
        }
        String visible = withoutProperties;
        for (String language : config.getLanguages()) {
            visible = visible.replaceAll("<!-- *" + Pattern.quote(language) + " *-->", "");
        }
        visible = VARIANT_MARKER.matcher(visible).replaceAll("");
        for (int i = 0; i < visible.length(); i++) {
            char c = visible.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                return false;
            }
        }
        return true;
    }

    public boolean isLineVisible(String language) {
        boolean hasLanguageMark = config.getLanguages().stream().anyMatch(s -> line.matches(".*<!-- *" + Pattern.quote(s) + " *-->.*"));
        boolean hasCurrentLanguageMark = line.matches(".*<!-- *" + Pattern.quote(language) + " *-->.*");
        boolean hasOnlyPropertyDefinition = hasOnlyPropertyDefinitions();

        return (!hasLanguageMark || hasCurrentLanguageMark) && !hasOnlyPropertyDefinition && isVisibleInVariant();
    }

    /**
     * {@code <!--variant:a,b-->} shows a line in variants {@code a} and {@code b};
     * {@code <!--variant:!c-->} shows it everywhere but {@code c}. Without a current variant (the
     * template declares none) the markers select nothing; {@code --validate} reports them.
     */
    private boolean isVisibleInVariant() {
        String variant = config.getVariant();
        if (variant == null || !line.contains("variant:")) {
            return true;
        }
        Matcher m = VARIANT_MARKER.matcher(line);
        while (m.find()) {
            if (!variantListSelects(m.group(1), variant)) {
                return false;
            }
        }
        return true;
    }

    static boolean variantListSelects(String list, String variant) {
        boolean anyPositive = false;
        boolean positiveMatch = false;
        for (String raw : list.split(",")) {
            String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.startsWith("!")) {
                if (entry.substring(1).trim().equals(variant)) {
                    return false;
                }
            } else {
                anyPositive = true;
                positiveMatch |= entry.equals(variant);
            }
        }
        return !anyPositive || positiveMatch;
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
                        value = renderProperties(value, language);
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
                        String body = indentContinuationLines(
                                widget.getBody(tag, config, language), result, shift + tag.getStart());
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

    /**
     * Repeats the indentation of the widget line on every further line of a multi-line
     * widget body. The tag is replaced in place, so only the first line of the body would
     * otherwise sit at that indentation — and an indented fence, alert or tree (typically
     * inside a list item) would fall apart in Markdown renderers.
     *
     * <p>Applies only when nothing but spaces and tabs precedes the tag; with other text in
     * front of it there is no indentation to speak of. Blank lines stay blank and a trailing
     * line break gets nothing appended, so no whitespace-only lines are produced.
     */
    private static String indentContinuationLines(String body, CharSequence line, int tagStart) {
        if (body == null || tagStart == 0 || (body.indexOf('\n') < 0 && body.indexOf('\r') < 0)) {
            return body;
        }
        for (int i = 0; i < tagStart; i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') {
                return body;
            }
        }
        String indent = line.subSequence(0, tagStart).toString();
        return CONTINUATION_LINE_START.matcher(body).replaceAll("$1" + Matcher.quoteReplacement(indent));
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
                    if (config.isEnvRestricted()) {
                        LOG.warn("Environment variable '{}' is not available under --no-env / --env-allow; rendering empty", name);
                    } else {
                        LOG.warn("Environment variable '{}' is not set; rendering empty", name);
                    }
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
        Optional<String> raw = reader.read(path);
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

    /**
     * The {@code pom.x} / {@code npm.x} / {@code gradle.x} references on this line that would
     * render empty: no {@code :fallback}, and the descriptor of {@link #config} does not define
     * the path. Resolution is the one rendering uses, so {@code --validate} reports exactly the
     * references generation would warn about. Escaped references are skipped.
     */
    List<String> findUnresolvedDescriptorReferences() {
        List<String> unresolved = new ArrayList<>();
        collectUnresolved(POM_TAG_PATTERN, "pom.", this::resolvePomPath, unresolved);
        collectUnresolved(NPM_TAG_PATTERN, "npm.", this::resolveNpmPath, unresolved);
        collectUnresolved(GRADLE_TAG_PATTERN, "gradle.", this::resolveGradlePath, unresolved);
        return unresolved;
    }

    private void collectUnresolved(Pattern pattern, String prefix,
                                   Function<String, String> resolver, List<String> sink) {
        Matcher m = pattern.matcher(line);
        while (m.find()) {
            if (isNotEscaped(line, m.start()) && m.group(2) == null && resolver.apply(m.group(1)) == null) {
                sink.add(prefix + m.group(1));
            }
        }
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
            return config.getEnvProvider().apply(envName);
        }
        if (name.startsWith("project.")) {
            String sub = name.substring("project.".length());
            Optional<String> v = reader.read(sub);
            if (!v.isPresent() && POM_INHERITED_FIELDS.contains(sub)) {
                v = reader.read("parent." + sub);
            }
            return v.orElse(null);
        }
        return reader.read("properties." + name).orElse(null);
    }

    /**
     * Public static counterpart of {@link #renderProperties(String, String)} usable from
     * pre-pass code (e.g. {@code BlockWidget} subclasses) that doesn't have a {@link TemplateLine}
     * instance. Public access is required because {@code BlockWidget} subclasses live in the
     * {@code com.nanolaba.nrg.widgets} package and cannot reach package-private members of
     * {@code com.nanolaba.nrg.core}.
     *
     * <p>Behaviour is identical to the instance method: substitutes {@code ${name}} occurrences
     * against {@link GeneratorConfig#getProperties()}, honours backslash-escapes, and re-scans
     * after each substitution to handle nested references.
     */
    public static String renderPropertiesIn(String line, GeneratorConfig config, String language) {
        Pattern pattern = Pattern.compile("\\$\\{\\s*([\\p{Alnum}-_.]+)\\s*}");
        Matcher m = pattern.matcher(line);
        while (m.find()) {
            if (isNotEscaped(line, m.start())) {
                String propertyName = m.group(1);
                String value = NRGUtil.getLanguageScopedProperty(config.getProperties(), propertyName, language);
                if (value != null) {
                    line = (m.start() > 0 ? line.substring(0, m.start()) : "") +
                            value +
                            (m.end() < line.length() ? line.substring(m.end()) : "");
                    m = pattern.matcher(line);
                }
            }
        }
        return line;
    }

    protected String renderProperties(String line, String language) {
        return renderPropertiesIn(line, config, language);
    }

    private static boolean isNotEscaped(String line, int start) {
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
        String entry = "\\s*(?:" + langAlt + ")\\s*:\\s*(?:'" + SINGLE_QUOTED_BODY + "'|\"" + DOUBLE_QUOTED_BODY + "\")\\s*";
        String regex = "\\$\\{(" + entry + "(?:," + entry + ")*)}";

        StringBuilder result = new StringBuilder(line);
        int shift = 0;
        Matcher matcher = Pattern.compile(regex).matcher(line);
        while (matcher.find()) {
            if (isNotEscaped(line, matcher.start())) {
                String body = "";

                String content = matcher.group(1);
                String entryPattern = "\\s*" + Pattern.quote(language) + ": *(?:'(?<single>"
                        + SINGLE_QUOTED_BODY + ")'|\"(?<double>" + DOUBLE_QUOTED_BODY + ")\")[,\\s]*";
                Matcher entryMatcher = Pattern.compile(entryPattern).matcher(content);
                while (entryMatcher.find()) {
                    String single = entryMatcher.group("single");
                    body = single != null
                            ? single.replace("''", "'")
                            : entryMatcher.group("double").replace("\"\"", "\"");
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
            result = renderProperties(result, language);
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
}
