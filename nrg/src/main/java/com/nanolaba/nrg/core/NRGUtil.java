package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.widgets.NRGWidget;
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

    private NRGUtil() {/**/}

    public static List<NRGWidget> loadWidgets(String commaSeparatedFqcns, ClassLoader classLoader) {
        List<NRGWidget> result = new ArrayList<>();
        if (StringUtils.isBlank(commaSeparatedFqcns)) {
            return result;
        }
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
        String[] strings = StringUtils.substringsBetween(line, "<!--", "-->");
        if (strings == null) {
            return map;
        }
        for (String comment : strings) {
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
     * that a 40 KB line turned into seconds. This scan is linear: the position of the next
     * terminator is remembered instead of being searched for again.
     */
    public static String stripPropertyComments(String text) {
        int open = text.indexOf("<!--");
        if (open < 0) {
            return text;
        }
        StringBuilder out = null;
        int copied = 0;
        int close = -1;     // start of the nearest "-->" at or after the current comment, once known
        int lineBreak = -1; // nearest line break at or after the current comment, once known
        while (open >= 0) {
            int at = open + 4;
            while (at < text.length() && text.charAt(at) == ' ') {
                at++;
            }
            if (at < text.length() && text.charAt(at) == '@') {
                if (close < at) {
                    close = text.indexOf("-->", at);
                    if (close < 0) {
                        break; // no terminator anywhere further on
                    }
                }
                if (lineBreak < at) {
                    lineBreak = nextLineBreak(text, at);
                }
                if (close < lineBreak) {
                    if (out == null) {
                        out = new StringBuilder(text.length());
                    }
                    out.append(text, copied, open);
                    copied = close + 3;
                    open = text.indexOf("<!--", copied);
                    continue;
                }
            }
            open = text.indexOf("<!--", open + 4);
        }
        if (out == null) {
            return text;
        }
        return out.append(text, copied, text.length()).toString();
    }

    /** Index of the first line break at or after {@code from}, or the text length if none. */
    private static int nextLineBreak(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
        }
        return text.length();
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
     * <p>The scan is a single pass. The tables built up front answer "where is the next
     * quote" and "where does the line end" in constant time; the regex this replaced
     * rescanned the rest of the text from every character and needed minutes for a 300 KB
     * parameter.
     */
    public static Map<String, String> parseParametersLine(String parameters) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(parameters)) {
            return map;
        }
        final String text = parameters;
        final int n = text.length();
        int[] nextSingle = new int[n + 1];
        int[] nextDouble = new int[n + 1];
        int[] nextLineEnd = new int[n + 1];
        nextSingle[n] = nextDouble[n] = nextLineEnd[n] = n;
        for (int p = n - 1; p >= 0; p--) {
            char c = text.charAt(p);
            nextSingle[p] = c == '\'' ? p : nextSingle[p + 1];
            nextDouble[p] = c == '"' ? p : nextDouble[p + 1];
            nextLineEnd[p] = isRegexLineTerminator(c) ? p : nextLineEnd[p + 1];
        }

        int i = 0;
        while (i < n) {
            if (!isParameterNameChar(text.charAt(i))) {
                i++;
                continue;
            }
            int nameEnd = i;
            while (nameEnd < n && isParameterNameChar(text.charAt(nameEnd))) {
                nameEnd++;
            }
            int equals = skipSpaces(text, nameEnd);
            int openQuote = equals < n && text.charAt(equals) == '='
                    ? openingQuoteAfter(text, equals, nextSingle, nextDouble, nextLineEnd)
                    : -1;
            if (openQuote < 0) {
                i = nameEnd; // not a parameter: resume after this word
                continue;
            }
            int closeQuote = (text.charAt(openQuote) == '\'' ? nextSingle : nextDouble)[openQuote + 1];
            map.put(text.substring(i, nameEnd), text.substring(openQuote + 1, closeQuote));
            i = closeQuote + 1;
        }
        return map;
    }

    private static boolean isParameterNameChar(char c) {
        return !isRegexWhitespace(c) && c != '=' && c != ',' && c != '\'' && c != '"';
    }

    /**
     * Given the position of an {@code =}, returns the position of the quote that opens the
     * value — after optional spaces, and only if the same quote closes it before the line
     * ends — or {@code -1}.
     */
    private static int openingQuoteAfter(String text, int equals, int[] nextSingle, int[] nextDouble,
                                         int[] nextLineEnd) {
        int quote = skipSpaces(text, equals + 1);
        if (quote >= text.length()) {
            return -1;
        }
        char q = text.charAt(quote);
        if (q != '\'' && q != '"') {
            return -1;
        }
        int close = (q == '\'' ? nextSingle : nextDouble)[quote + 1];
        return close < text.length() && close < nextLineEnd[quote + 1] ? quote : -1;
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

    /** What {@code .} refuses to match in java.util.regex by default. */
    private static boolean isRegexLineTerminator(char c) {
        return c == '\n' || c == '\r' || c == '\u0085' || c == ' ' || c == ' ';
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
