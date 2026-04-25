package com.nanolaba.nrg.widgets;

import com.nanolaba.logging.LOG;
import com.nanolaba.nrg.core.GeneratorConfig;
import com.nanolaba.nrg.core.NRGUtil;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Renders a {@code tree -L}-style directory listing with Unicode box-drawing characters.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code path} (required) — directory to list (relative to the source file's directory).
 *   <li>{@code depth} (default {@code 2}) — recursion limit; {@code 1} means only direct children.
 *   <li>{@code exclude} — comma-separated glob patterns matched against both the entry name and
 *       its path relative to {@code path}. Standard glob syntax (e.g. {@code *.class},
 *       {@code target}, {@code sub/drop.txt}).
 *   <li>{@code dirsOnly} (default {@code false}) — list directories only, hide files.
 *   <li>{@code codeblock} (default {@code true}) — wrap output in a fenced code block.
 * </ul>
 *
 * <p>Entries are sorted directories-first, then alphabetically within each group, for stable
 * reproducible output.
 */
public class FileTreeWidget extends DefaultWidget {

    private static final String CONNECTOR_BRANCH = "├── ";
    private static final String CONNECTOR_LAST = "└── ";
    private static final String INDENT_VERT = "│   ";
    private static final String INDENT_BLANK = "    ";

    @Override
    public String getName() {
        return "fileTree";
    }

    @Override
    public String getBody(WidgetTag widgetTag, GeneratorConfig config, String language) {
        Map<String, String> params = NRGUtil.parseParametersLine(widgetTag.getParameters());

        String pathParam = params.get("path");
        if (pathParam == null || pathParam.trim().isEmpty()) {
            LOG.error("fileTree widget: missing required 'path' parameter");
            return "";
        }

        int depth = 2;
        String depthRaw = params.get("depth");
        if (depthRaw != null) {
            try {
                depth = Integer.parseInt(depthRaw.trim());
            } catch (NumberFormatException e) {
                LOG.error("fileTree widget: invalid 'depth' value '{}' (expected positive integer)", depthRaw);
                return "";
            }
            if (depth <= 0) {
                LOG.error("fileTree widget: invalid 'depth' value '{}' (expected positive integer)", depthRaw);
                return "";
            }
        }

        boolean dirsOnly = "true".equalsIgnoreCase(params.getOrDefault("dirsOnly", "false"));
        String codeblockRaw = params.getOrDefault("codeblock", "true");
        boolean codeblock = !"false".equalsIgnoreCase(codeblockRaw);

        List<PathMatcher> excludeMatchers = parseExcludes(params.get("exclude"));

        File root = resolveRoot(config, pathParam);
        if (!root.isDirectory()) {
            LOG.error("fileTree widget: path is not a directory: {}", root.getAbsolutePath());
            return "";
        }

        StringBuilder body = new StringBuilder();
        body.append(root.getName()).append(System.lineSeparator());
        renderChildren(root, "", depth, 1, dirsOnly, excludeMatchers, "", body);

        String content = stripTrailingNewline(body.toString());
        if (codeblock) {
            return "```" + System.lineSeparator() + content + System.lineSeparator() + "```";
        }
        return content;
    }

    private static File resolveRoot(GeneratorConfig config, String raw) {
        File requested = new File(raw);
        if (requested.isAbsolute()) {
            return requested;
        }
        File base = config.getSourceFile().getAbsoluteFile().getParentFile();
        return new File(base, raw);
    }

    private void renderChildren(File dir, String indent, int maxDepth, int currentLevel,
                                boolean dirsOnly, List<PathMatcher> excludes, String relativePathPrefix,
                                StringBuilder out) {
        File[] childArray = dir.listFiles();
        if (childArray == null) {
            return;
        }
        List<File> entries = new ArrayList<>(Arrays.asList(childArray));
        // directories first, alphabetical within groups
        entries.sort((a, b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.getName().compareTo(b.getName());
        });

        List<File> kept = new ArrayList<>(entries.size());
        for (File entry : entries) {
            String relativePath = relativePathPrefix.isEmpty()
                    ? entry.getName()
                    : relativePathPrefix + "/" + entry.getName();
            if (isExcluded(entry, relativePath, excludes)) continue;
            if (dirsOnly && entry.isFile()) continue;
            kept.add(entry);
        }

        for (int i = 0; i < kept.size(); i++) {
            File entry = kept.get(i);
            boolean isLast = i == kept.size() - 1;
            String connector = isLast ? CONNECTOR_LAST : CONNECTOR_BRANCH;
            out.append(indent).append(connector).append(entry.getName()).append(System.lineSeparator());

            if (entry.isDirectory() && currentLevel < maxDepth) {
                String childIndent = indent + (isLast ? INDENT_BLANK : INDENT_VERT);
                String childRel = relativePathPrefix.isEmpty()
                        ? entry.getName()
                        : relativePathPrefix + "/" + entry.getName();
                renderChildren(entry, childIndent, maxDepth, currentLevel + 1, dirsOnly, excludes, childRel, out);
            }
        }
    }

    private static boolean isExcluded(File entry, String relativePath, List<PathMatcher> excludes) {
        if (excludes.isEmpty()) return false;
        String name = entry.getName();
        java.nio.file.Path namePath = Paths.get(name);
        java.nio.file.Path relPath = Paths.get(relativePath.replace('/', java.io.File.separatorChar));
        for (PathMatcher m : excludes) {
            if (m.matches(namePath) || m.matches(relPath)) return true;
        }
        return false;
    }

    private static List<PathMatcher> parseExcludes(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        List<PathMatcher> result = new ArrayList<>();
        for (String pattern : raw.split(",")) {
            String trimmed = pattern.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed));
            } catch (IllegalArgumentException e) {
                LOG.error("fileTree widget: invalid exclude pattern '{}': {}", trimmed, e.getMessage());
            }
        }
        return result;
    }

    private static String stripTrailingNewline(String s) {
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) end--;
        return s.substring(0, end);
    }
}
