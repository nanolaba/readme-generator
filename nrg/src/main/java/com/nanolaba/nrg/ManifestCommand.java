package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.NRGConstants;
import com.nanolaba.nrg.core.TemplateManifest;
import com.nanolaba.nrg.widgets.NRGWidget;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * The read-only questions about a project that a hook or a coding agent asks before it edits
 * anything: {@code --manifest} (what each template writes and reads), {@code --is-generated}
 * (is this file an output, and of which template?) and {@code --affected} (which templates
 * must be regenerated after these files changed?). All three are answered from
 * {@link TemplateManifest}s, without rendering.
 *
 * <p>Without explicit sources the templates are discovered: every {@code *.src.md} below the
 * working directory (skipping hidden directories, {@code node_modules}, {@code target} and
 * {@code build}) that no other discovered template imports — fragments are inputs, not roots.
 */
final class ManifestCommand {

    private static final Set<String> SKIPPED_DIRECTORIES =
            new HashSet<>(Arrays.asList("node_modules", "target", "build"));

    private final Charset charset;
    private final List<NRGWidget> widgets;
    private final File workingDirectory;
    private final List<TemplateManifest> manifests = new ArrayList<>();
    private boolean loadFailed;

    ManifestCommand(Charset charset, List<NRGWidget> widgets) {
        this(charset, widgets, new File("."));
    }

    /** @param workingDirectory where templates are discovered and what printed paths are relative to */
    ManifestCommand(Charset charset, List<NRGWidget> widgets, File workingDirectory) {
        this.charset = charset;
        this.widgets = widgets;
        this.workingDirectory = workingDirectory.getAbsoluteFile();
    }

    /** Loads the manifests of {@code sources}, or of the discovered root templates when it is empty. */
    ManifestCommand load(List<File> sources) throws IOException {
        boolean discovered = sources.isEmpty();
        for (File source : discovered ? discover() : sources) {
            if (!source.isFile()) {
                System.err.println("Source file does not exist: " + source.getPath());
                loadFailed = true;
                continue;
            }
            try {
                manifests.add(TemplateManifest.of(new Generator(source, charset, widgets), charset));
            } catch (RuntimeException e) {
                System.err.println("Cannot read " + source.getPath() + ": " + e.getMessage());
                loadFailed = true;
            }
        }
        if (discovered) {
            dropFragments();
        }
        return this;
    }

    int printManifest(RunReport.Format format) {
        if (format == RunReport.Format.JSON) {
            StringBuilder sb = new StringBuilder("{\n  \"version\": ").append(RunReport.str(NRG.getVersion().trim()))
                    .append(",\n  \"mode\": \"manifest\",\n  \"templates\": [");
            for (int i = 0; i < manifests.size(); i++) {
                TemplateManifest m = manifests.get(i);
                sb.append(i == 0 ? "\n" : ",\n").append("    {\"source\": ").append(RunReport.str(path(m.getSource())));
                sb.append(",\n     \"outputs\": [");
                int k = 0;
                for (Map.Entry<String, File> e : m.getOutputs().entrySet()) {
                    sb.append(k++ == 0 ? "" : ", ").append("{\"language\": ").append(RunReport.str(e.getKey()))
                            .append(", \"path\": ").append(RunReport.str(path(e.getValue()))).append('}');
                }
                sb.append("],\n     \"inputs\": ").append(array(paths(m.getInputs())));
                sb.append(",\n     \"dynamic\": ").append(array(new ArrayList<>(m.getDynamic()))).append('}');
            }
            sb.append(manifests.isEmpty() ? "]\n}" : "\n  ]\n}");
            System.out.println(sb);
        } else {
            for (TemplateManifest m : manifests) {
                System.out.println(path(m.getSource()));
                m.getOutputs().forEach((lang, f) -> System.out.println("  output  " + path(f) + " (" + lang + ")"));
                m.getInputs().forEach(f -> System.out.println("  input   " + path(f)));
                m.getDynamic().forEach(d -> System.out.println("  dynamic " + d));
            }
        }
        return loadFailed ? NRG.EXIT_TEMPLATE_ERROR : NRG.EXIT_OK;
    }

    /** Exit status 0 and the producing template(s) when {@code file} is generated, 1 otherwise. */
    int printIsGenerated(File file, RunReport.Format format) {
        List<String> sources = new ArrayList<>();
        for (TemplateManifest m : manifests) {
            if (m.producesOutput(file)) {
                sources.add(path(m.getSource()));
            }
        }
        if (format == RunReport.Format.JSON) {
            System.out.println("{\"version\": " + RunReport.str(NRG.getVersion().trim()) + ", \"mode\": \"is-generated\", "
                    + "\"file\": " + RunReport.str(path(file)) + ", \"generated\": " + !sources.isEmpty()
                    + ", \"sources\": " + array(sources) + "}");
        } else {
            sources.forEach(System.out::println);
        }
        return sources.isEmpty() ? 1 : NRG.EXIT_OK;
    }

    /** The templates that read any of {@code changed}, one per line. */
    int printAffected(List<File> changed, RunReport.Format format) {
        List<String> affected = new ArrayList<>();
        for (TemplateManifest m : manifests) {
            for (File f : changed) {
                if (m.readsInput(f) || readsFromDirectory(m, f)) {
                    affected.add(path(m.getSource()));
                    break;
                }
            }
        }
        if (format == RunReport.Format.JSON) {
            List<String> changedPaths = new ArrayList<>();
            changed.forEach(f -> changedPaths.add(path(f)));
            System.out.println("{\"version\": " + RunReport.str(NRG.getVersion().trim()) + ", \"mode\": \"affected\", "
                    + "\"files\": " + array(changedPaths) + ", \"affected\": " + array(affected) + "}");
        } else {
            affected.forEach(System.out::println);
        }
        return loadFailed ? NRG.EXIT_TEMPLATE_ERROR : NRG.EXIT_OK;
    }

    /** A changed file inside a directory that {@code fileTree} lists changes that listing. */
    private static boolean readsFromDirectory(TemplateManifest m, File changed) {
        Path path = changed.getAbsoluteFile().toPath().normalize();
        for (File input : m.getInputs()) {
            if (input.isDirectory() && path.startsWith(input.toPath())) {
                return true;
            }
        }
        return false;
    }

    private List<File> discover() throws IOException {
        List<File> found = new ArrayList<>();
        Files.walkFileTree(workingDirectory.toPath().normalize(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                boolean skip = !dir.equals(workingDirectory.toPath().normalize())
                        && (name.startsWith(".") || SKIPPED_DIRECTORIES.contains(name));
                return skip ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith("." + NRGConstants.DEFAULT_SOURCE_EXTENSION)) {
                    found.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(found);
        return found;
    }

    private void dropFragments() {
        Set<File> imported = new HashSet<>();
        for (TemplateManifest m : manifests) {
            for (File input : m.getInputs()) {
                if (!input.equals(m.getSource())) {
                    imported.add(input);
                }
            }
        }
        manifests.removeIf(m -> imported.contains(m.getSource()));
    }

    private List<String> paths(Collection<File> files) {
        List<String> result = new ArrayList<>();
        files.forEach(f -> result.add(path(f)));
        return result;
    }

    private String path(File file) {
        Path absolute = file.getAbsoluteFile().toPath().normalize();
        Path base = workingDirectory.toPath().normalize();
        String result = absolute.startsWith(base) ? base.relativize(absolute).toString() : absolute.toString();
        return result.replace(File.separatorChar, '/');
    }

    private static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(RunReport.str(values.get(i)));
        }
        return sb.append(']').toString();
    }
}
