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
        Map<String, String> seen = new HashMap<>();
        for (String lang : languages) {
            File f;
            try {
                f = OutputFileNameResolver.resolve(sourceFile, defaultLanguage, lang, properties);
            } catch (IllegalStateException e) {
                return Optional.of(e.getMessage());
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
