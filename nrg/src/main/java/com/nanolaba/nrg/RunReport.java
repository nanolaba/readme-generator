package com.nanolaba.nrg;

import com.nanolaba.nrg.core.Validator;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Machine-readable account of one CLI run, behind {@code --format json} and
 * {@code --format github}. The text output of NRG is written for a person reading a terminal;
 * a coding agent, a hook or a CI step needs to know which files were written or are stale and
 * which diagnostics were raised without parsing log lines, and this class collects exactly that
 * while the run proceeds.
 *
 * <p>Paths are reported relative to the working directory with {@code /} separators (absolute
 * when outside it), so the same document reads the same on every platform. The JSON is written
 * by hand — the shape is small and fixed, and NRG has no JSON library to lean on.
 */
final class RunReport {

    /** Output style selected with {@code --format}. */
    enum Format {
        TEXT, JSON, GITHUB;

        static Format parse(String raw) {
            if (raw == null || raw.isEmpty()) {
                return TEXT;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private final String mode;
    private final File workingDirectory;
    private final List<FileEntry> files = new ArrayList<>();
    private final List<Validator.Diagnostic> diagnostics = new ArrayList<>();

    RunReport(String mode, File workingDirectory) {
        this.mode = mode;
        this.workingDirectory = workingDirectory.getAbsoluteFile();
    }

    void addFile(File source, File output, String language, String status) {
        files.add(new FileEntry(source, output, language, status));
    }

    void addDiagnostic(Validator.Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    void addDiagnostics(List<Validator.Diagnostic> list) {
        diagnostics.addAll(list);
    }

    /** One JSON document describing the run; {@code ok} is true exactly when the exit code is 0. */
    String toJson(int exitCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": ").append(str(NRG.getVersion().trim())).append(",\n");
        sb.append("  \"mode\": ").append(str(mode)).append(",\n");
        sb.append("  \"ok\": ").append(exitCode == 0).append(",\n");
        sb.append("  \"exitCode\": ").append(exitCode).append(",\n");
        sb.append("  \"files\": [");
        for (int i = 0; i < files.size(); i++) {
            FileEntry f = files.get(i);
            sb.append(i == 0 ? "\n" : ",\n").append("    {");
            sb.append("\"source\": ").append(str(path(f.source)));
            if (f.output != null) {
                sb.append(", \"output\": ").append(str(path(f.output)));
            }
            if (f.language != null) {
                sb.append(", \"language\": ").append(str(f.language));
            }
            sb.append(", \"status\": ").append(str(f.status)).append('}');
        }
        sb.append(files.isEmpty() ? "],\n" : "\n  ],\n");
        sb.append("  \"diagnostics\": [");
        for (int i = 0; i < diagnostics.size(); i++) {
            Validator.Diagnostic d = diagnostics.get(i);
            sb.append(i == 0 ? "\n" : ",\n").append("    {");
            sb.append("\"severity\": ").append(str(d.isError() ? "error" : "warning"));
            sb.append(", \"code\": ").append(str(d.getCode()));
            sb.append(", \"file\": ").append(d.getFile() == null ? "null" : str(path(d.getFile())));
            sb.append(", \"line\": ").append(d.getLine());
            sb.append(", \"message\": ").append(str(d.getMessage()));
            sb.append(", \"hint\": ").append(d.getHint() == null ? "null" : str(d.getHint()));
            sb.append('}');
        }
        sb.append(diagnostics.isEmpty() ? "]\n" : "\n  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * GitHub Actions workflow commands — one {@code ::error} / {@code ::warning} line per
     * diagnostic and per stale or missing output — so that a CI step shows the findings next to
     * the offending lines of the pull request.
     */
    List<String> toGithubAnnotations() {
        List<String> lines = new ArrayList<>();
        for (Validator.Diagnostic d : diagnostics) {
            StringBuilder sb = new StringBuilder(d.isError() ? "::error " : "::warning ");
            if (d.getFile() != null) {
                sb.append("file=").append(property(path(d.getFile())));
                if (d.getLine() > 0) {
                    sb.append(",line=").append(d.getLine());
                }
                sb.append(',');
            }
            sb.append("title=").append(property("NRG " + d.getCode())).append("::")
                    .append(data(d.getMessage() + (d.getHint() == null ? "" : "; " + d.getHint())));
            lines.add(sb.toString());
        }
        for (FileEntry f : files) {
            if ("stale".equals(f.status) || "missing".equals(f.status)) {
                String output = path(f.output);
                String what = "stale".equals(f.status)
                        ? output + " is out of date with " + path(f.source)
                        : output + " does not exist";
                lines.add("::error file=" + property(output) + ",title=" + property("NRG " + f.status + " output")
                        + "::" + data(what + "; regenerate with: nrg -f " + path(f.source)));
            }
        }
        return lines;
    }

    private String path(File file) {
        Path absolute = file.getAbsoluteFile().toPath().normalize();
        Path base = workingDirectory.toPath().normalize();
        String result = absolute.startsWith(base) ? base.relativize(absolute).toString() : absolute.toString();
        return result.replace(File.separatorChar, '/');
    }

    static String str(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /** Escaping of the message part of a workflow command. */
    private static String data(String value) {
        return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A");
    }

    /** Escaping of a property value ({@code file=}, {@code title=}) of a workflow command. */
    private static String property(String value) {
        return data(value).replace(":", "%3A").replace(",", "%2C");
    }

    private static final class FileEntry {
        final File source;
        final File output;
        final String language;
        final String status;

        FileEntry(File source, File output, String language, String status) {
            this.source = source;
            this.output = output;
            this.language = language;
            this.status = status;
        }
    }
}
