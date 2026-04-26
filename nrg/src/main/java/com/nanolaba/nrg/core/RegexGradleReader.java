package com.nanolaba.nrg.core;

import com.nanolaba.logging.LOG;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default {@link GradleReader} that lazily reads {@code gradle.properties} and a Gradle
 * build script, then resolves keys via flat-property lookup with a regex-based fallback
 * for the well-known {@code version} and {@code group} fields.
 *
 * <p>Missing files degrade to "every read returns empty"; a single error is logged on
 * first access if the configured location does not yield any readable file.
 */
public final class RegexGradleReader implements GradleReader {

    private static final Pattern VERSION_OR_GROUP =
            Pattern.compile("^\\s*(version|group)\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);

    private final File propertiesFile;
    private final File buildScriptFile;
    private boolean attempted;
    private Properties properties;
    private String buildScriptText;

    public RegexGradleReader(File propertiesFile, File buildScriptFile) {
        this.propertiesFile = propertiesFile;
        this.buildScriptFile = buildScriptFile;
    }

    @Override
    public Optional<String> read(String path) {
        load();
        if (properties != null) {
            String fromProps = properties.getProperty(path);
            if (fromProps != null) {
                return Optional.of(fromProps);
            }
        }
        if (("version".equals(path) || "group".equals(path)) && buildScriptText != null) {
            Matcher m = VERSION_OR_GROUP.matcher(buildScriptText);
            while (m.find()) {
                if (path.equals(m.group(1))) {
                    return Optional.of(m.group(2));
                }
            }
        }
        return Optional.empty();
    }

    private void load() {
        if (attempted) {
            return;
        }
        attempted = true;
        boolean anyFound = false;
        if (propertiesFile != null && propertiesFile.isFile()) {
            anyFound = true;
            try {
                Properties p = new Properties();
                try (java.io.InputStream in = java.nio.file.Files.newInputStream(propertiesFile.toPath())) {
                    p.load(in);
                }
                properties = p;
            } catch (Exception e) {
                LOG.error(e, () -> "gradle reader: failed to parse " + propertiesFile.getAbsolutePath());
            }
        }
        if (buildScriptFile != null && buildScriptFile.isFile()) {
            anyFound = true;
            try {
                buildScriptText = FileUtils.readFileToString(buildScriptFile, NRGConstants.DEFAULT_CHARSET);
            } catch (Exception e) {
                LOG.error(e, () -> "gradle reader: failed to read " + buildScriptFile.getAbsolutePath());
            }
        }
        if (!anyFound) {
            LOG.error("gradle reader: no gradle.properties or build.gradle(.kts) found at configured location");
        }
    }
}
