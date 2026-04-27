package com.nanolaba.nrg.core;

import java.util.Optional;

/**
 * Resolves dotted paths against a {@code package.json} descriptor.
 *
 * <p>The path is interpreted as a walk from the JSON root: top-level fields such as
 * {@code "version"} or {@code "name"}, and nested object fields such as
 * {@code "dependencies.lodash"}. Non-string leaves (numbers, booleans) are stringified;
 * objects, arrays, and {@code null} resolve to {@link Optional#empty()}.
 */
public interface NpmReader {

    /**
     * Returns the resolved value for the given dotted path, or {@link Optional#empty()}
     * if the path is missing or terminates at a non-leaf value.
     *
     * @param path dotted path from the JSON root (e.g. {@code "version"},
     *             {@code "dependencies.lodash"}); never {@code null}.
     * @return the resolved value, or {@link Optional#empty()} if absent / non-leaf.
     */
    Optional<String> read(String path);
}
