package com.nanolaba.nrg.core;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

/**
 * Resolves the output file path for a given (source file, language) pair.
 *
 * <p>Resolution order, most-specific first:
 * <ol>
 *   <li>{@code nrg.fileNamePattern.<language>} — per-language override.</li>
 *   <li>{@code nrg.defaultLanguageFileNamePattern} — applies when {@code language}
 *       equals the default language.</li>
 *   <li>{@code nrg.fileNamePattern} — global pattern.</li>
 *   <li>Built-in: {@code <base>.md} for the default language,
 *       {@code <base>.<lang>.md} otherwise.</li>
 * </ol>
 *
 * <p>Patterns may include {@code /} separators; the result is a {@link File} relative
 * to the source file's parent directory.
 *
 * <p>Placeholders inside a pattern:
 * <ul>
 *   <li>{@code <base>} — source filename minus {@code .src.md} (or just the bare
 *       filename if it doesn't end in {@code .src.md}).</li>
 *   <li>{@code <lang>} — language code as written.</li>
 *   <li>{@code <LANG>} — language code uppercased.</li>
 *   <li>{@code <variant>} / {@code <VARIANT>} — the output variant being generated
 *       ({@code nrg.variants}), as written / uppercased; empty without variants.</li>
 * </ul>
 *
 * <p>With a variant, {@code nrg.variant.<name>.file} beats every other pattern, and the built-in
 * names become {@code <base>.<variant>.md} / {@code <base>.<variant>.<lang>.md}.
 */
public final class OutputFileNameResolver {

    private OutputFileNameResolver() {/**/}

    public static File resolve(File sourceFile, String defaultLanguage, String language, Properties properties) {
        String variant = properties.getProperty(NRGConstants.PROPERTY_VARIANT);
        String pattern = variant == null ? null : properties.getProperty(
                NRGConstants.PROPERTY_VARIANT_FILE_PREFIX + variant + NRGConstants.PROPERTY_VARIANT_FILE_SUFFIX);
        if (pattern == null) {
            pattern = pickPattern(defaultLanguage, language, properties);
        }
        if (pattern != null) {
            if (pattern.isEmpty()) {
                throw new IllegalStateException(
                        "Empty file name pattern for language '" + language + "'");
            }
            return applyPattern(sourceFile, language, variant, pattern);
        }
        return legacyFile(sourceFile, defaultLanguage, language, variant);
    }

    private static String pickPattern(String defaultLanguage, String language, Properties properties) {
        if (language != null) {
            String perLang = properties.getProperty(NRGConstants.PROPERTY_FILE_NAME_PATTERN + "." + language);
            if (perLang != null) {
                return perLang;
            }
        }
        if (language != null && language.equals(defaultLanguage)) {
            String defaultLangPattern = properties.getProperty(NRGConstants.PROPERTY_DEFAULT_LANGUAGE_FILE_NAME_PATTERN);
            if (defaultLangPattern != null) {
                return defaultLangPattern;
            }
        }
        return properties.getProperty(NRGConstants.PROPERTY_FILE_NAME_PATTERN);
    }

    private static File applyPattern(File sourceFile, String language, String variant, String pattern) {
        String base = baseName(sourceFile);
        String lang = language == null ? "" : language;
        String var = variant == null ? "" : variant;
        String resolved = pattern
                .replace("<base>", base)
                .replace("<LANG>", lang.toUpperCase(Locale.ROOT))
                .replace("<lang>", lang)
                .replace("<VARIANT>", var.toUpperCase(Locale.ROOT))
                .replace("<variant>", var);
        File parent = sourceFile.getAbsoluteFile().getParentFile();
        return parent == null ? new File(resolved) : new File(parent, resolved);
    }

    private static File legacyFile(File sourceFile, String defaultLanguage, String language, String variant) {
        String path = StringUtils.substringBeforeLast(sourceFile.getAbsolutePath(),
                "." + NRGConstants.DEFAULT_SOURCE_EXTENSION) +
                (variant == null ? "" : "." + variant) +
                (language != null && language.equals(defaultLanguage) ? ".md" : "." + language + ".md");
        return new File(path);
    }

    private static String baseName(File sourceFile) {
        String name = sourceFile.getName();
        String suffix = "." + NRGConstants.DEFAULT_SOURCE_EXTENSION;
        if (name.endsWith(suffix)) {
            return name.substring(0, name.length() - suffix.length());
        }
        return name;
    }
}
