package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static helpers shared between the CLI, the {@link GeneratorConfig} bootstrap, and the
 * per-line {@link TemplateLine} renderer.
 *
 * <p>Covers five orthogonal concerns:
 * <ul>
 *   <li>Reflective widget loading from FQCN lists ({@link #loadWidgets}, {@link #loadWidget}).</li>
 *   <li>Property merging and language-scoped lookup ({@link #mergeProperty},
 *       {@link #getLanguageScopedProperty}).</li>
 *   <li>Marker / parameter parsing for the template DSL
 *       ({@link #extractRawPropertyMarkers}, {@link #parseParametersLine},
 *       {@link #unwrapParameterValue}).</li>
 *   <li>Widget parameter escape processing ({@link #processWidgetEscapes}).</li>
 *   <li>Cheap text-search utilities ({@link #findFirstUnescapedOccurrenceLine}).</li>
 * </ul>
 */
public class NRGUtil {

    /** What {@code .} refuses to match in java.util.regex by default. */
    private static final Pattern REGEX_LINE_TERMINATOR = Pattern.compile("[\\n\\r\\u0085\\u2028\\u2029]");

    private NRGUtil() {/**/}

    public static List<NRGWidget> loadWidgets(String commaSeparatedFqcns, ClassLoader classLoader) {
        List<NRGWidget> result = new ArrayList<>();
        if (StringUtils.isNotBlank(commaSeparatedFqcns)) {
            ClassLoader cl = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
            for (String raw : commaSeparatedFqcns.split(",")) {
                String fqcn = raw.trim();
                if (fqcn.isEmpty()) {
                    continue;
                }
                NRGWidget widget = loadWidget(fqcn, cl);
                if (widget != null) {
                    result.add(widget);
                }
            }
        }
        return result;
    }

    private static NRGWidget loadWidget(String fqcn, ClassLoader classLoader) {
        Class<?> cls;
        try {
            // initialize=false: the name can come from a template (nrg.widgets), and naming a
            // class must not run its static initialiser before we know it is a widget.
            cls = Class.forName(fqcn, false, classLoader);
        } catch (ClassNotFoundException e) {
            LOG.error("Widget class not found: '{}' (check the classpath and the spelling)", fqcn);
            return null;
        }
        if (!NRGWidget.class.isAssignableFrom(cls)) {
            LOG.error("Class '{}' does not implement {}", fqcn, NRGWidget.class.getName());
            return null;
        }
        try {
            Object instance = cls.getDeclaredConstructor().newInstance();
            return (NRGWidget) instance;
        } catch (NoSuchMethodException e) {
            LOG.error("Widget class '{}' must declare a public no-argument constructor", fqcn);
            return null;
        } catch (InvocationTargetException e) {
            LOG.error(e.getCause() != null ? e.getCause() : e,
                    () -> "Widget class '" + fqcn + "' threw during construction");
            return null;
        } catch (Throwable e) {
            LOG.error(e, () -> "Failed to instantiate widget class '" + fqcn + "'");
            return null;
        }
    }

    public static void mergeProperty(Object key, Object value, Properties properties) {
        properties.setProperty(key.toString(), String.valueOf(value));
    }

    /**
     * Returns the value of a property, preferring a language-scoped variant if defined.
     *
     * <p>Lookup order, most-specific first:
     * <ol>
     *   <li>{@code name + "." + language} — when {@code language} is non-null and the key exists.</li>
     *   <li>{@code name} — bare key as cross-language fallback.</li>
     *   <li>{@code null} — neither key is defined.</li>
     * </ol>
     *
     * <p>An explicitly defined empty per-language value wins over the bare key.
     *
     * @param properties the property bag to query.
     * @param name       the property name (without language suffix).
     * @param language   the target language code, or {@code null} to skip the scoped lookup.
     * @return the resolved value, or {@code null} if neither the scoped nor the bare key is defined.
     */
    public static String getLanguageScopedProperty(Properties properties, String name, String language) {
        if (language != null) {
            String langScoped = properties.getProperty(name + "." + language);
            if (langScoped != null) {
                return langScoped;
            }
        }
        return properties.getProperty(name);
    }

    /**
     * Parses raw {@code <!--@key=value-->} markers from a single line without any substitution
     * (no env / language / property rendering) and without mutating any caller state.
     * Returns a key→value map preserving insertion order.
     *
     * @param line the source line to scan; never {@code null}.
     * @return an insertion-ordered map of declared key/value pairs (empty if none found).
     */
    public static Map<String, String> extractRawPropertyMarkers(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        // substringsBetween gives null, not an empty array, for a line without comments
        for (String comment : ArrayUtils.nullToEmpty(StringUtils.substringsBetween(line, "<!--", "-->"))) {
            String s = StringUtils.trimToEmpty(comment);
            if (!s.contains("@")) {
                continue;
            }
            s = StringUtils.substringAfter(s, "@");
            if (!s.contains("=")) {
                continue;
            }
            String key = StringUtils.trimToEmpty(StringUtils.substringBefore(s, "="));
            String value = StringUtils.trimToEmpty(StringUtils.substringAfter(s, "="));
            map.put(key, value);
        }
        return map;
    }

    /**
     * Removes every {@code <!--@…-->} property comment from {@code text} and returns the same
     * instance when there is none. Each comment ends at the first {@code -->} after it on the
     * same line; a comment with no terminator on its line is left alone.
     *
     * <p>Replaces the regex {@code <!-- *@.*-->}, which was wrong twice over. Being greedy, it
     * ran from the first property comment to the <em>last</em> {@code -->} on the line and
     * deleted whatever text stood between them. And on a line with many unterminated
     * {@code <!--@} it rescanned the rest of the line for every one of them — quadratic time
     * that a 40 KB line turned into seconds. This scan is linear: one pass from the end builds
     * a table of where the terminator of a comment starting at each position lies, and every
     * comment is then decided by a single lookup.
     */
    public static String stripPropertyComments(String text) {
        int open = text.indexOf("<!--");
        if (open < 0) {
            return text;
        }
        int n = text.length();
        // closeOnLine[p]: start of the first "-->" at or after p, or n when a line break (or the
        // end of the text) comes first. Only positions from the first comment on are looked up.
        int[] closeOnLine = new int[n + 1];
        closeOnLine[n] = n;
        for (int p = n - 1; p >= open; p--) {
            char c = text.charAt(p);
            if (c == '\n' || c == '\r') {
                closeOnLine[p] = n;
            } else {
                closeOnLine[p] = text.startsWith("-->", p) ? p : closeOnLine[p + 1];
            }
        }
        StringBuilder out = null;
        int copied = 0;
        while (open >= 0) {
            int at = skipSpaces(text, open + 4);
            // No terminator can start within the "<!--" and the spaces before the '@', so the
            // first one from the start of the comment is the first one after its '@'.
            int close = closeOnLine[open];
            if (at < n && text.charAt(at) == '@' && close < n) {
                if (out == null) {
                    out = new StringBuilder(n);
                }
                out.append(text, copied, open);
                copied = close + 3;
                open = text.indexOf("<!--", copied);
            } else {
                open = text.indexOf("<!--", open + 4);
            }
        }
        return out == null ? text : out.append(text, copied, n).toString();
    }

    /**
     * Parses a widget parameter list — {@code name='value', other="value"} — into a map.
     *
     * <p>A parameter is a name, {@code =}, and a value in single or double quotes, with
     * optional spaces around the {@code =}. The value runs to the next quote of the same kind
     * on the same line, so it may contain the other quote, commas and parentheses. Anything
     * between parameters — commas, whitespace, stray text — is skipped; a later parameter
     * with the same name wins.
     *
     * <p>A name is a run of characters other than whitespace, {@code =}, {@code ,} and
     * quotes. This used to be "any run of non-whitespace, the longer the better" (the regex
     * {@code (\S+) *= *(["'])…}), which made the space after a comma mandatory without saying
     * so: in {@code title="T",ordered="true"} the whole of {@code title="T",ordered} was
     * taken as one name and both parameters were lost.
     *
     * <p>The scan is linear. A value is searched for only up to its own closing quote, and no
     * two candidate values open at the same quote, so the searches together cover the text
     * at most once per kind of quote; the regex this replaced rescanned the rest of the text
     * from every character and needed minutes for a 300 KB parameter.
     */
    public static Map<String, String> parseParametersLine(String parameters) {
        Map<String, String> map = new HashMap<>();
        int n = parameters == null ? 0 : parameters.length();
        int i = 0;
        while (i < n) {
            if (!isParameterNameChar(parameters.charAt(i))) {
                i++;
                continue;
            }
            int nameEnd = i;
            while (nameEnd < n && isParameterNameChar(parameters.charAt(nameEnd))) {
                nameEnd++;
            }
            int equals = skipSpaces(parameters, nameEnd);
            boolean hasEquals = equals < n && parameters.charAt(equals) == '=';
            int openQuote = hasEquals ? skipSpaces(parameters, equals + 1) : n;
            int closeQuote = closingQuote(parameters, openQuote);
            if (closeQuote == -1) {
                i = nameEnd; // not a parameter: resume after this word
                continue;
            }
            map.put(parameters.substring(i, nameEnd), parameters.substring(openQuote + 1, closeQuote));
            i = closeQuote; // a quote is no name character, so the next round steps over it
        }
        return map;
    }

    private static boolean isParameterNameChar(char c) {
        return !isRegexWhitespace(c) && c != '=' && c != ',' && c != '\'' && c != '"';
    }

    /**
     * Given the position where the value of a parameter should open, returns the position of
     * the quote that closes it — the same kind of quote, later on the same line — or
     * {@code -1} when there is no quote at {@code quote} or nothing closes it.
     */
    private static int closingQuote(String text, int quote) {
        if (quote >= text.length()) {
            return -1;
        }
        char q = text.charAt(quote);
        if (q != '\'' && q != '"') {
            return -1;
        }
        int close = text.indexOf(q, quote + 1);
        return close != -1 && !REGEX_LINE_TERMINATOR.matcher(text).region(quote, close).find() ? close : -1;
    }

    private static int skipSpaces(String text, int from) {
        int p = from;
        while (p < text.length() && text.charAt(p) == ' ') {
            p++;
        }
        return p;
    }

    /** {@code \s} of java.util.regex without UNICODE_CHARACTER_CLASS. */
    private static boolean isRegexWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r';
    }

    public static String unwrapParameterValue(String wrapped) {
        wrapped = StringUtils.trimToEmpty(wrapped);
        return !wrapped.isEmpty() ? StringUtils.unwrap(wrapped, wrapped.charAt(0)) : "";
    }

    /**
     * Interprets the two escape sequences {@code \n} and {@code \\} inside widget parameter
     * values. {@code \n} becomes a literal newline ({@code '\n'}, not {@link System#lineSeparator()})
     * and {@code \\} becomes a single backslash. Every other backslash is preserved verbatim,
     * including {@code \r}, {@code \t}, and {@code \?} — widgets that need a broader escape
     * vocabulary should implement their own scan.
     *
     * <p>Single-pass — replacing escapes with {@link String#replace} one at a time would let an
     * earlier substitution's output (e.g. a literal {@code \n} unescaped from {@code \\n}) be
     * re-interpreted by the next pass.
     */
    public static String processWidgetEscapes(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'n') {
                    out.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    out.append('\\');
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    public static int findFirstUnescapedOccurrenceLine(String text, String searchString) {
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            Pattern pattern = Pattern.compile("(?<!\\\\)" + Pattern.quote(searchString));
            Matcher matcher = pattern.matcher(lines[i]);

            if (matcher.find()) {
                return i;
            }
        }

        return -1;
    }
}
