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
 * <p>Covers four orthogonal concerns:
 * <ul>
 *   <li>Reflective widget loading from FQCN lists ({@link #loadWidgets}, {@link #loadWidget}).</li>
 *   <li>Property merging and language-scoped lookup ({@link #mergeProperty},
 *       {@link #getLanguageScopedProperty}).</li>
 *   <li>Marker / parameter parsing for the template DSL
 *       ({@link #extractRawPropertyMarkers}, {@link #parseParametersLine},
 *       {@link #unwrapParameterValue}).</li>
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
            cls = Class.forName(fqcn, true, classLoader);
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

    public static Map<String, String> parseParametersLine(String parameters) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isNotEmpty(parameters)) {
            Pattern pattern = Pattern.compile("(\\S+) *= *([\"'])(((?!\\2).)*)(\\2)");
            Matcher matcher = pattern.matcher(parameters);
            while (matcher.find()) {
                map.put(matcher.group(1), matcher.group(3));
            }
        }
        return map;
    }

    public static String unwrapParameterValue(String wrapped) {
        wrapped = StringUtils.trimToEmpty(wrapped);
        return !wrapped.isEmpty() ? StringUtils.unwrap(wrapped, wrapped.charAt(0)) : "";
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
