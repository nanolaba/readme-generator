package com.nanolaba.nrg.core;

import java.util.Optional;

/**
 * Resolves a Gradle-flavoured key against a project's {@code gradle.properties} file
 * and/or its {@code build.gradle} / {@code build.gradle.kts} script.
 *
 * <p>Lookup order:
 * <ol>
 *   <li>Flat lookup in {@code gradle.properties} (the full path is used verbatim as key).</li>
 *   <li>If still missing and the path is {@code "version"} or {@code "group"}: regex extraction
 *       from the build script for {@code version = '...'} / {@code group = '...'} (works for
 *       both Groovy and Kotlin DSLs).</li>
 *   <li>Otherwise {@link Optional#empty()}.</li>
 * </ol>
 */
public interface GradleReader {

    Optional<String> read(String path);
}
