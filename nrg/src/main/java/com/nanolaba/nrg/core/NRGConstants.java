package com.nanolaba.nrg.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class NRGConstants {

    public static final String PROPERTY_LANGUAGES = "nrg.languages";
    public static final String PROPERTY_DEFAULT_LANGUAGE = "nrg.defaultLanguage";
    public static final String PROPERTY_WIDGETS = "nrg.widgets";
    public static final String PROPERTY_POM_PATH = "nrg.pom.path";
    public static final String PROPERTY_NPM_PATH = "nrg.npm.path";
    public static final String PROPERTY_GRADLE_PATH = "nrg.gradle.path";
    public static final String PROPERTY_FILE_NAME_PATTERN = "nrg.fileNamePattern";
    public static final String PROPERTY_DEFAULT_LANGUAGE_FILE_NAME_PATTERN = "nrg.defaultLanguageFileNamePattern";
    public static final String PROPERTY_FILE_NAME_PATTERN_PER_LANGUAGE_PREFIX = "nrg.fileNamePattern.";
    public static final String PROPERTY_ALLOW_REMOTE_IMPORTS = "nrg.allowRemoteImports";
    public static final String PROPERTY_CACHE_DIR = "nrg.cacheDir";
    public static final String PROPERTY_REQUIRE_SHA256_FOR_REMOTE = "nrg.requireSha256ForRemote";

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final String DEFAULT_SOURCE_EXTENSION = "src.md";

}
