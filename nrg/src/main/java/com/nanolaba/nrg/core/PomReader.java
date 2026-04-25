package com.nanolaba.nrg.core;

import java.util.Optional;

/**
 * Resolves Maven-style dotted paths against a project descriptor.
 *
 * <p>The path is interpreted as a walk from the implicit {@code <project>} root: top-level
 * elements such as {@code "version"}, {@code "groupId"}, nested elements such as
 * {@code "scm.url"} or {@code "parent.version"}, and {@code <properties>} entries via the
 * {@code "properties.<key>"} prefix where the remainder of the path (including any further
 * dots) is used verbatim as the property key.
 */
public interface PomReader {

    /**
     * Returns the resolved value for the given Maven-style path, or {@link Optional#empty()}
     * if no such element / property is present.
     */
    Optional<String> read(String path);
}
