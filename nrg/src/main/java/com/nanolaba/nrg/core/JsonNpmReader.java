package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.json.MinimalJsonParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link NpmReader} that lazily parses a {@code package.json} file once and
 * resolves dotted paths against the resulting JSON tree.
 *
 * <p>Missing file or parse error degrades to "every read returns empty" with a single
 * error logged on first access.
 */
public final class JsonNpmReader implements NpmReader {

    private final File packageJsonFile;
    private boolean attempted;
    private Object root;

    public JsonNpmReader(File packageJsonFile) {
        this.packageJsonFile = packageJsonFile;
    }

    @Override
    public Optional<String> read(String path) {
        Object node = root();
        if (node == null) {
            return Optional.empty();
        }
        for (String segment : path.split("\\.")) {
            if (!(node instanceof Map)) {
                return Optional.empty();
            }
            Map<?, ?> map = (Map<?, ?>) node;
            if (!map.containsKey(segment)) {
                return Optional.empty();
            }
            node = map.get(segment);
        }
        if (node == null) {
            return Optional.empty();
        }
        if (node instanceof String) {
            return Optional.of((String) node);
        }
        if (node instanceof Number || node instanceof Boolean) {
            return Optional.of(String.valueOf(node));
        }
        return Optional.empty();
    }

    private Object root() {
        if (!attempted) {
            attempted = true;
            if (packageJsonFile == null || !packageJsonFile.isFile()) {
                LOG.error("npm reader: file not found: {}",
                        packageJsonFile == null ? "<null>" : packageJsonFile.getAbsolutePath());
                return null;
            }
            try {
                String text = FileUtils.readFileToString(packageJsonFile, NRGConstants.DEFAULT_CHARSET);
                root = MinimalJsonParser.parse(text);
            } catch (Exception e) {
                LOG.error(e, () -> "npm reader: failed to parse " + packageJsonFile.getAbsolutePath());
                root = null;
            }
        }
        return root;
    }
}
