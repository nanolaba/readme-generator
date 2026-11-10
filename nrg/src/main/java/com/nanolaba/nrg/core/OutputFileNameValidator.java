package com.nanolaba.nrg.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Validates the configured filename patterns produce distinct output paths across all languages.
 *
 * <p>Returns an error message describing the first problem found, or {@link Optional#empty()}
 * if all configured languages would produce distinct output paths.
 */
public final class OutputFileNameValidator {

    private OutputFileNameValidator() {/**/}

    public static Optional<String> findError(File sourceFile, String defaultLanguage,
                                             List<String> languages, Properties properties) {
        return findError(sourceFile, defaultLanguage, languages, properties, null);
    }

    /**
     * @param projectRoot when non-null, every output path must also lie inside it (see
     *                    {@link ProjectRoot}): a file name pattern is template data, and
     *                    {@code ../} in it must not let a template write anywhere on disk.
     */
    public static Optional<String> findError(File sourceFile, String defaultLanguage,
                                             List<String> languages, Properties properties,
                                             File projectRoot) {
        Map<String, String> seen = new HashMap<>();
        for (String lang : languages) {
            File f;
            try {
                f = OutputFileNameResolver.resolve(sourceFile, defaultLanguage, lang, properties);
            } catch (IllegalStateException e) {
                return Optional.of(e.getMessage());
            }
            if (projectRoot != null && !ProjectRoot.contains(projectRoot, f)) {
                return Optional.of("Output file for language '" + lang + "' is outside the project root: "
                        + ProjectRoot.canonical(f).getPath() + " (root: " + ProjectRoot.canonical(projectRoot).getPath()
                        + "). Pass --root <dir> (Maven: <root>) to widen the root.");
            }
            String key = f.getAbsolutePath();
            String previous = seen.put(key, lang);
            if (previous != null) {
                return Optional.of("Languages '" + previous + "' and '" + lang +
                        "' collide on the same output path: " + key);
            }
        }
        return Optional.empty();
    }
}
