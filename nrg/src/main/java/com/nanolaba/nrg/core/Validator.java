package com.nanolaba.nrg.core;

import com.nanolaba.nrg.core.freeze.FreezeValidator;
import com.nanolaba.nrg.widgets.NRGWidget;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static validator for NRG source templates. Scans a root template (and any reachable
 * {@code ${widget:import(path='...')}}-imported files) and emits diagnostics for common
 * authoring mistakes without producing any output.
 *
 * <p>v1 covers: unregistered widget names, language markers not in {@code nrg.languages},
 * missing {@code import} paths, and unbalanced {@code <!--nrg.ignore.begin-->} /
 * {@code <!--nrg.ignore.end-->} pairs.
 */
public final class Validator {

    private static final Pattern IMPORT_PATH_PATTERN = Pattern.compile("path\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern LANG_MARKER_PATTERN = Pattern.compile("<!--\\s*([A-Za-z][A-Za-z0-9_-]*)\\s*-->");
    private static final Pattern IGNORE_BEGIN_PATTERN = Pattern.compile("<!--\\s*nrg\\.ignore\\.begin\\s*-->");
    private static final Pattern IGNORE_END_PATTERN = Pattern.compile("<!--\\s*nrg\\.ignore\\.end\\s*-->");

    private final File rootSourceFile;

    public Validator(File rootSourceFile) {
        this.rootSourceFile = rootSourceFile;
    }

    public List<Diagnostic> validate() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Set<File> visited = new HashSet<>();
        if (rootSourceFile == null || !rootSourceFile.isFile()) {
            diagnostics.add(new Diagnostic(rootSourceFile, 0,
                    "source file does not exist: "
                            + (rootSourceFile == null ? "<null>" : rootSourceFile.getAbsolutePath()),
                    Severity.ERROR));
            return diagnostics;
        }
        Set<String> knownWidgets = collectKnownWidgets();
        List<String> rootLanguages = readDeclaredLanguages(readSafe(rootSourceFile));
        validateFile(rootSourceFile, diagnostics, knownWidgets, visited, rootLanguages);
        return diagnostics;
    }

    private Set<String> collectKnownWidgets() {
        // Use a throwaway GeneratorConfig instance to discover the registered widgets, including
        // those declared via the nrg.widgets template property.
        GeneratorConfig probe = new GeneratorConfig(rootSourceFile, readSafe(rootSourceFile), null);
        Set<String> names = new HashSet<>();
        for (NRGWidget w : probe.getWidgets()) {
            names.add(w.getName());
        }
        // 'endIf' / 'endDetails' are block-widget closer pseudo-names handled by their
        // respective BlockWidget subclasses (IfWidget, DetailsWidget) — accept them so the
        // validator doesn't flag closers as unknown widgets. The opener names ('if', 'details')
        // are already registered via probe.getWidgets() above.
        names.add("endIf");
        names.add("endDetails");
        return names;
    }

    private void validateFile(File file, List<Diagnostic> diagnostics, Set<String> knownWidgets,
                              Set<File> visited, List<String> inheritedLanguages) {
        File canonical;
        try {
            canonical = file.getCanonicalFile();
        } catch (IOException e) {
            canonical = file.getAbsoluteFile();
        }
        if (!visited.add(canonical)) {
            return;
        }
        String body = readSafe(file);
        if (body == null) return;

        // A file declaring its own nrg.languages overrides what was inherited; otherwise the
        // imported file uses the parent's declaration (imports don't have to repeat it).
        List<String> ownDeclaration = readOwnDeclaredLanguages(body);
        List<String> declaredLanguages = ownDeclaration != null ? ownDeclaration : inheritedLanguages;
        String[] lines = body.split("\\R", -1);

        int ignoreBeginLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNo = i + 1;

            // 1. Unregistered widgets.
            Matcher widgetMatcher = TemplateLine.WIDGET_TAG_PATTERN.matcher(line);
            while (widgetMatcher.find()) {
                int start = widgetMatcher.start();
                if (start > 0 && line.charAt(start - 1) == '\\') {
                    continue; // escaped, not a real widget call
                }
                String name = widgetMatcher.group(1);
                if (!knownWidgets.contains(name)) {
                    diagnostics.add(new Diagnostic(file, lineNo,
                            "unknown widget '" + name + "'", Severity.ERROR));
                }
                if ("import".equals(name)) {
                    String params = widgetMatcher.group(3);
                    String path = extractImportPath(params);
                    if (path != null) {
                        File importedFile = new File(file.getAbsoluteFile().getParentFile(), path);
                        if (!importedFile.isFile()) {
                            diagnostics.add(new Diagnostic(file, lineNo,
                                    "import path not found: '" + path + "'", Severity.ERROR));
                        } else {
                            validateFile(importedFile, diagnostics, knownWidgets, visited, declaredLanguages);
                        }
                    }
                }
            }

            // 2. Language markers <!--xx-->.
            Matcher langMatcher = LANG_MARKER_PATTERN.matcher(line);
            while (langMatcher.find()) {
                String marker = langMatcher.group(1);
                if (isReservedMarker(marker)) continue;
                if (declaredLanguages != null && !declaredLanguages.contains(marker)) {
                    diagnostics.add(new Diagnostic(file, lineNo,
                            "language marker '" + marker + "' is not declared in nrg.languages",
                            Severity.ERROR));
                }
            }

            // 3. nrg.ignore balance.
            if (IGNORE_BEGIN_PATTERN.matcher(line).find()) {
                if (ignoreBeginLine >= 0) {
                    diagnostics.add(new Diagnostic(file, lineNo,
                            "nested <!--nrg.ignore.begin--> without preceding <!--nrg.ignore.end-->",
                            Severity.ERROR));
                } else {
                    ignoreBeginLine = lineNo;
                }
            }
            if (IGNORE_END_PATTERN.matcher(line).find()) {
                if (ignoreBeginLine < 0) {
                    diagnostics.add(new Diagnostic(file, lineNo,
                            "stray <!--nrg.ignore.end--> without matching <!--nrg.ignore.begin-->",
                            Severity.ERROR));
                } else {
                    ignoreBeginLine = -1;
                }
            }
        }

        diagnostics.addAll(FreezeValidator.validate(file, body, declaredLanguages));

        if (ignoreBeginLine >= 0) {
            diagnostics.add(new Diagnostic(file, ignoreBeginLine,
                    "unclosed <!--nrg.ignore.begin--> (missing <!--nrg.ignore.end-->)",
                    Severity.ERROR));
        }
    }

    /** Root-level lookup: returns the declared languages or the {@code ["en"]} default if none found. */
    private static List<String> readDeclaredLanguages(String body) {
        List<String> own = readOwnDeclaredLanguages(body);
        return own != null ? own : Collections.singletonList("en");
    }

    /** Returns the {@code nrg.languages} declared in this file, or {@code null} if absent. */
    private static List<String> readOwnDeclaredLanguages(String body) {
        if (body == null) return null;
        for (String line : body.split("\\R", -1)) {
            Map<String, String> raw = NRGUtil.extractRawPropertyMarkers(line);
            String langs = raw.get(NRGConstants.PROPERTY_LANGUAGES);
            if (langs != null && !langs.isEmpty()) {
                List<String> result = new ArrayList<>();
                for (String s : langs.split(",")) {
                    result.add(s.trim());
                }
                return result;
            }
        }
        return null;
    }

    private static String extractImportPath(String params) {
        if (params == null) return null;
        Matcher m = IMPORT_PATH_PATTERN.matcher(params);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isReservedMarker(String marker) {
        // markers that look like comments but aren't language tags
        return marker.startsWith("nrg.") || marker.startsWith("toc.")
                || "@".equals(marker) || marker.startsWith("@");
    }

    private static String readSafe(File file) {
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public enum Severity {ERROR, WARNING}

    public static final class Diagnostic {
        private final File file;
        private final int line;
        private final String message;
        private final Severity severity;

        public Diagnostic(File file, int line, String message, Severity severity) {
            this.file = file;
            this.line = line;
            this.message = message;
            this.severity = severity;
        }

        public File getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public String getMessage() {
            return message;
        }

        public Severity getSeverity() {
            return severity;
        }

        public boolean isError() {
            return severity == Severity.ERROR;
        }

        public boolean isWarning() {
            return severity == Severity.WARNING;
        }

        @Override
        public String toString() {
            return (severity == Severity.ERROR ? "ERROR" : "WARNING") + ": "
                    + (file == null ? "<unknown>" : file.getName()) + ":" + line + ": " + message;
        }
    }

    /**
     * Format diagnostics into a single multi-line string, one per line.
     *
     * @param diagnostics the diagnostics to format; never {@code null}.
     * @return a newline-joined rendering of each diagnostic's {@link Diagnostic#toString()};
     *         empty string if the list is empty.
     */
    public static String format(List<Diagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            if (sb.length() > 0) sb.append(System.lineSeparator());
            sb.append(d.toString());
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    private static String unused() {
        // Suppress unused-import warning for StringUtils if anything else gets stripped.
        return StringUtils.EMPTY;
    }
}
