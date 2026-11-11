package com.nanolaba.nrg.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Centralised string constants shared across the generator: NRG-recognised property names
 * (the {@code nrg.*} keys parsed from {@code <!--@key=value-->} markers), the default
 * source-file extension, and the default charset.
 *
 * <p>Kept as plain {@code public static final} fields rather than an enum so they can be
 * referenced inline in property keys, regex literals, and log messages without ceremony.
 */
public class NRGConstants {

    public static final String PROPERTY_LANGUAGES = "nrg.languages";
    public static final String PROPERTY_DEFAULT_LANGUAGE = "nrg.defaultLanguage";
    public static final String PROPERTY_WIDGETS = "nrg.widgets";
    public static final String PROPERTY_POM_PATH = "nrg.pom.path";
    public static final String PROPERTY_NPM_PATH = "nrg.npm.path";
    public static final String PROPERTY_GRADLE_PATH = "nrg.gradle.path";
    public static final String PROPERTY_FILE_NAME_PATTERN = "nrg.fileNamePattern";
    public static final String PROPERTY_DEFAULT_LANGUAGE_FILE_NAME_PATTERN = "nrg.defaultLanguageFileNamePattern";
    public static final String PROPERTY_ALLOW_REMOTE_IMPORTS = "nrg.allowRemoteImports";
    public static final String PROPERTY_CACHE_DIR = "nrg.cacheDir";
    public static final String PROPERTY_REQUIRE_SHA256_FOR_REMOTE = "nrg.requireSha256ForRemote";
    public static final String PROPERTY_NO_HEADER = "nrg.noHeader";
    public static final String PROPERTY_HEADER_TEXT = "nrg.headerText";
    public static final String PROPERTY_VALIDATE_PARITY = "nrg.validate.parity";
    /** Comma-separated output variants (e.g. {@code agents,claude,gemini}); each is generated in every language. */
    public static final String PROPERTY_VARIANTS = "nrg.variants";
    /** The variant being generated; set by NRG, readable in templates as {@code ${nrg.variant}}. */
    public static final String PROPERTY_VARIANT = "nrg.variant";
    /** {@code nrg.variant.<name>.file} — output path pattern of one variant. */
    public static final String PROPERTY_VARIANT_FILE_PREFIX = "nrg.variant.";
    public static final String PROPERTY_VARIANT_FILE_SUFFIX = ".file";

    /** Leaves a line out of the translation-parity checks of {@code --validate}; stripped from the output. */
    public static final String PARITY_IGNORE_MARKER = "<!--nrg.parity.ignore-->";

    public static final String FREEZE_OPEN_MARKER_NAME = "nrg.freeze";
    public static final String FREEZE_CLOSE_MARKER_NAME = "/nrg.freeze";
    public static final String FREEZE_ATTR_ID = "id";
    public static final String FREEZE_ATTR_SOURCE_LANG = "source-lang";

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final String DEFAULT_SOURCE_EXTENSION = "src.md";

}
