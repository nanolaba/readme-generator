package com.nanolaba.nrg;

import com.nanolaba.nrg.core.GenerationResult;
import com.nanolaba.nrg.core.Generator;
import com.nanolaba.nrg.core.NRGConstants;
import com.nanolaba.nrg.core.SourceOrigin;
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
 * (is this file an output, and of which template?), {@code --affected} (which templates
 * must be regenerated after these files changed?) and {@code --locate} (which template line
 * produced this line of a generated file?). The first three are answered from
 * {@link TemplateManifest}s without rendering; {@code --locate} renders the one template
 * concerned with source-map collection on.
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

    /**
     * {@code --locate FILE:LINE[-END]}: renders the template that produces {@code FILE} with
     * source-map collection on and prints, for each requested line, the template line it comes
     * from and the chain of imports that led there.
     *
     * @return 0 when located, 1 when no template produces the file, 64 for a malformed or
     *         out-of-range position
     */
    int printLocate(String position, RunReport.Format format) {
        int colon = position.lastIndexOf(':');
        int first;
        int last;
        try {
            String range = position.substring(colon + 1);
            int dash = range.indexOf('-');
            first = Integer.parseInt(dash < 0 ? range : range.substring(0, dash));
            last = dash < 0 ? first : Integer.parseInt(range.substring(dash + 1));
        } catch (RuntimeException e) {
            System.err.println("--locate expects FILE:LINE or FILE:FIRST-LAST, got '" + position + "'");
            return NRG.EXIT_USAGE;
        }
        if (colon <= 0 || first < 1 || last < first) {
            System.err.println("--locate expects FILE:LINE or FILE:FIRST-LAST, got '" + position + "'");
            return NRG.EXIT_USAGE;
        }
        File output = new File(position.substring(0, colon));
        for (TemplateManifest m : manifests) {
            for (Map.Entry<String, File> e : m.getOutputs().entrySet()) {
                if (m.producesOutput(output) && e.getValue().equals(output.getAbsoluteFile().toPath().normalize().toFile())) {
                    return locate(m, e.getKey(), output, first, last, format);
                }
            }
        }
        System.err.println(position.substring(0, colon) + " is not generated by any of the templates");
        return 1;
    }

    private int locate(TemplateManifest m, String language, File output, int first, int last, RunReport.Format format) {
        GenerationResult result;
        try {
            Generator generator = new Generator(m.getSource(), charset, widgets);
            generator.getConfig().setCollectSourceMap(true);
            result = generator.getResult(language);
        } catch (IOException | RuntimeException e) {
            System.err.println("Cannot render " + path(m.getSource()) + ": " + e.getMessage());
            return NRG.EXIT_TEMPLATE_ERROR;
        }
        String[] lines = result.getContent().toString().split("\\R", -1);
        List<SourceOrigin> map = result.getSourceMap();
        int available = Math.min(map.size(), lines.length);
        if (first > available) {
            System.err.println(path(output) + " has " + available + " lines when generated now; line " + first + " does not exist");
            return NRG.EXIT_USAGE;
        }
        int end = Math.min(last, available);
        if (format == RunReport.Format.JSON) {
            StringBuilder sb = new StringBuilder("{\"version\": ").append(RunReport.str(NRG.getVersion().trim()))
                    .append(", \"mode\": \"locate\", \"file\": ").append(RunReport.str(path(output)))
                    .append(", \"language\": ").append(RunReport.str(language))
                    .append(", \"source\": ").append(RunReport.str(path(m.getSource())))
                    .append(", \"lines\": [");
            for (int n = first; n <= end; n++) {
                sb.append(n == first ? "\n  " : ",\n  ").append("{\"line\": ").append(n)
                        .append(", \"text\": ").append(RunReport.str(lines[n - 1]))
                        .append(", \"origin\": ").append(originJson(map.get(n - 1))).append('}');
            }
            System.out.println(sb.append("\n]}"));
        } else {
            for (int n = first; n <= end; n++) {
                System.out.println(path(output) + ":" + n + ": " + describe(map.get(n - 1)));
            }
        }
        return NRG.EXIT_OK;
    }

    private String describe(SourceOrigin o) {
        if (o.getFile() == null) {
            return o.getNote() == null ? "generated" : o.getNote();
        }
        StringBuilder sb = new StringBuilder(path(o.getFile())).append(':').append(o.getLine());
        if (o.getWidget() != null) {
            sb.append(" (widget ").append(o.getWidget()).append(')');
        }
        if (o.getNote() != null) {
            sb.append(" (").append(o.getNote()).append(')');
        }
        for (SourceOrigin via = o.getImportedFrom(); via != null; via = via.getImportedFrom()) {
            sb.append(" <- imported at ").append(path(via.getFile())).append(':').append(via.getLine());
        }
        return sb.toString();
    }

    private String originJson(SourceOrigin o) {
        StringBuilder sb = new StringBuilder("{\"file\": ")
                .append(o.getFile() == null ? "null" : RunReport.str(path(o.getFile())))
                .append(", \"line\": ").append(o.getLine())
                .append(", \"widget\": ").append(o.getWidget() == null ? "null" : RunReport.str(o.getWidget()))
                .append(", \"note\": ").append(o.getNote() == null ? "null" : RunReport.str(o.getNote()))
                .append(", \"importChain\": [");
        boolean firstLink = true;
        for (SourceOrigin via = o.getImportedFrom(); via != null; via = via.getImportedFrom()) {
            sb.append(firstLink ? "" : ", ").append("{\"file\": ").append(RunReport.str(path(via.getFile())))
                    .append(", \"line\": ").append(via.getLine()).append('}');
            firstLink = false;
        }
        return sb.append("]}").toString();
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
