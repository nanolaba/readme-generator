package com.nanolaba.nrg.core;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What one root template produces and what it reads, computed without rendering it: the
 * output file of every language, and the inputs — the template itself, every file it imports
 * (transitively), the directories {@code fileTree} lists and the descriptors behind
 * {@code ${pom.…}} / {@code ${npm.…}} / {@code ${gradle.…}}. Backs {@code --manifest},
 * {@code --is-generated} and {@code --affected}: a hook that guards generated files, or an
 * agent that changed a source file, needs these answers without running a generation.
 *
 * <p>Imports are followed the way {@code ImportWidget} follows them — relative to the
 * importing file, into templates unless {@code run-generator='false'} — and a fragment's
 * descriptors resolve against its own directory with the parent's properties, as in
 * generation. What cannot be known statically is listed in {@link #getDynamic()} instead of
 * being evaluated: remote imports, {@code exec} commands, environment variables and the
 * current date.
 */
public final class TemplateManifest {

    private static final Pattern ENV_REFERENCE = Pattern.compile("\\$\\{\\s*env\\.([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern DESCRIPTOR_REFERENCE = Pattern.compile("\\$\\{\\s*(pom|npm|gradle)\\.");

    private final File source;
    private final Map<String, File> outputs = new LinkedHashMap<>();
    private final Set<File> inputs = new LinkedHashSet<>();
    private final Set<String> dynamic = new LinkedHashSet<>();
    private final Charset charset;
    private final String variant;

    private TemplateManifest(File source, Charset charset, String variant) {
        this.source = normalise(source);
        this.charset = charset;
        this.variant = variant;
    }

    /** Builds the manifest of the template behind {@code generator} (nothing is rendered). */
    public static TemplateManifest of(Generator generator, Charset charset) {
        GeneratorConfig config = generator.getConfig();
        TemplateManifest manifest = new TemplateManifest(config.getSourceFile(), charset, config.getVariant());
        for (String language : config.getLanguages()) {
            manifest.outputs.put(language, normalise(OutputFileNameResolver.resolve(
                    config.getSourceFile(), config.getDefaultLanguage(), language, config.getProperties())));
        }
        manifest.walk(config.getSourceFile(), config, config.getSourceFileBody(), new HashSet<>());
        return manifest;
    }

    private void walk(File file, GeneratorConfig config, String body, Set<File> visited) {
        File normalised = normalise(file);
        inputs.add(normalised);
        if (!visited.add(normalised) || body == null) {
            return;
        }
        Set<String> descriptorKinds = new LinkedHashSet<>();
        File directory = normalised.getParentFile();
        for (String line : IgnoreBlockStripper.strip(body).split("\\R", -1)) {
            Matcher widget = TemplateLine.WIDGET_TAG_PATTERN.matcher(line);
            while (widget.find()) {
                if (escaped(line, widget.start())) {
                    continue;
                }
                Map<String, String> params = NRGUtil.parseParametersLine(widget.group(3));
                switch (widget.group(1)) {
                    case "import":
                        followImport(directory, config, params, visited);
                        break;
                    case "fileTree":
                        if (params.get("path") != null) {
                            inputs.add(normalise(new File(directory, params.get("path"))));
                        }
                        break;
                    case "exec":
                        dynamic.add("exec:" + params.getOrDefault("cmd", ""));
                        break;
                    case "date":
                        dynamic.add("date");
                        break;
                    default:
                        break;
                }
            }
            Matcher env = ENV_REFERENCE.matcher(line);
            while (env.find()) {
                if (!escaped(line, env.start())) {
                    dynamic.add("env:" + env.group(1));
                }
            }
            Matcher descriptor = DESCRIPTOR_REFERENCE.matcher(line);
            while (descriptor.find()) {
                if (!escaped(line, descriptor.start())) {
                    descriptorKinds.add(descriptor.group(1));
                }
            }
        }
        for (String kind : descriptorKinds) {
            try {
                for (File f : config.getDescriptorFiles(kind)) {
                    inputs.add(normalise(f));
                }
            } catch (RuntimeException e) {
                // Outside the project root: generation refuses it, so it is no input either.
            }
        }
    }

    private void followImport(File directory, GeneratorConfig config, Map<String, String> params, Set<File> visited) {
        if (params.get("url") != null) {
            dynamic.add("url:" + params.get("url"));
            return;
        }
        String path = params.get("path");
        if (path == null) {
            return;
        }
        File imported = new File(directory, path);
        if ("false".equals(params.get("run-generator")) || !imported.isFile()) {
            inputs.add(normalise(imported));
            return;
        }
        String body;
        try {
            body = FileUtils.readFileToString(imported, charset);
        } catch (IOException e) {
            inputs.add(normalise(imported));
            return;
        }
        GeneratorConfig child;
        try {
            child = config.forImportedTemplate(imported, body, null);
        } catch (RuntimeException e) {
            child = config;
        }
        walk(imported, child, body, visited);
    }

    private static boolean escaped(String line, int start) {
        return start > 0 && line.charAt(start - 1) == '\\';
    }

    private static File normalise(File file) {
        return file.getAbsoluteFile().toPath().normalize().toFile();
    }

    /** The output variant this manifest describes ({@code nrg.variants}), or {@code null}. */
    public String getVariant() {
        return variant;
    }

    /** The root template, as an absolute normalised path. */
    public File getSource() {
        return source;
    }

    /** Output file of each declared language, in declaration order (absolute, normalised). */
    public Map<String, File> getOutputs() {
        return Collections.unmodifiableMap(outputs);
    }

    /** The template first, then every file and directory it reads (absolute, normalised). */
    public Set<File> getInputs() {
        return Collections.unmodifiableSet(inputs);
    }

    /**
     * Inputs that cannot be listed as files: {@code url:…} (remote import), {@code exec:…},
     * {@code env:NAME} and {@code date}. A template with any of these can change without any of
     * its {@link #getInputs() inputs} changing.
     */
    public Set<String> getDynamic() {
        return Collections.unmodifiableSet(dynamic);
    }

    public boolean producesOutput(File file) {
        return outputs.containsValue(normalise(file));
    }

    public boolean readsInput(File file) {
        return inputs.contains(normalise(file));
    }
}
