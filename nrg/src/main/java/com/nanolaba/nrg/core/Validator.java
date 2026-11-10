package com.nanolaba.nrg.core;

import com.nanolaba.nrg.core.freeze.FreezeValidator;
import com.nanolaba.nrg.widgets.BlockWidget;
import com.nanolaba.nrg.widgets.NRGWidget;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static validator for NRG source templates. Scans a root template (and every reachable
 * {@code ${widget:import(path='...')}}-imported template) and emits diagnostics for common
 * authoring mistakes without producing any output.
 *
 * <p>Errors: unregistered widget names, parameters a built-in widget does not have, language
 * markers not in {@code nrg.languages}, missing or escaping {@code import} paths, unbalanced
 * {@code <!--nrg.ignore.begin-->} / {@code <!--nrg.ignore.end-->} pairs, unbalanced block
 * widgets ({@code if} / {@code details}) and {@code ${name}} references that are undefined but
 * one edit away from a declared property (a typo). Warnings: other undefined {@code ${name}}
 * references (they stay in the output as written; legitimate for shell examples, so not an
 * error), {@code pom.} / {@code npm.} / {@code gradle.} references that render empty, and
 * {@code <!--\@nrg.…-->} examples, which declare the property for real.
 *
 * <p>Diagnostics name the fix where there is one ("did you mean …?"); they are read by people
 * and, more and more, by coding agents that act on the last clause. An import with
 * {@code run-generator='false'} is only checked for existence: its content is not a template.
 */
public final class Validator {

    private static final Pattern IMPORT_PATH_PATTERN = Pattern.compile("path\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern LANG_MARKER_PATTERN = Pattern.compile("<!--\\s*([A-Za-z][A-Za-z0-9_-]*)\\s*-->");
    private static final Pattern IGNORE_BEGIN_PATTERN = Pattern.compile("<!--\\s*nrg\\.ignore\\.begin\\s*-->");
    private static final Pattern IGNORE_END_PATTERN = Pattern.compile("<!--\\s*nrg\\.ignore\\.end\\s*-->");
    private static final Pattern IGNORE_LINE_PATTERN = Pattern.compile("<!--\\s*nrg\\.ignore\\s*-->");
    /** Same reference shape {@code TemplateLine.renderProperties} substitutes. */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{\\s*([\\p{Alnum}_.\\-]+)\\s*}");
    private static final Pattern COSMETIC_NRG_PROPERTY_PATTERN = Pattern.compile("<!--\\s*\\\\@\\s*(nrg\\.[\\w.\\-]+)\\s*=");
    private static final List<String> SOURCE_PREFIXES = Arrays.asList("env.", "pom.", "npm.", "gradle.");

    private final File rootSourceFile;
    private final File projectRoot;
    private final List<NRGWidget> extraWidgets;

    public Validator(File rootSourceFile) {
        this(rootSourceFile, null);
    }

    /**
     * @param projectRoot directory imports must stay inside (see {@link ProjectRoot});
     *                    {@code null} detects it from {@code rootSourceFile}.
     */
    public Validator(File rootSourceFile, File projectRoot) {
        this(rootSourceFile, projectRoot, null);
    }

    /**
     * @param extraWidgets widgets registered by the caller ({@code --widgets}, {@code NRG.addWidget}),
     *                     so that their names are known and not reported; may be {@code null}.
     */
    public Validator(File rootSourceFile, File projectRoot, List<NRGWidget> extraWidgets) {
        this.rootSourceFile = rootSourceFile;
        this.projectRoot = projectRoot != null ? projectRoot : ProjectRoot.detect(rootSourceFile);
        this.extraWidgets = extraWidgets;
    }

    public List<Diagnostic> validate() {
        if (rootSourceFile == null || !rootSourceFile.isFile()) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            diagnostics.add(new Diagnostic(rootSourceFile, 0,
                    "source file does not exist: "
                            + (rootSourceFile == null ? "<null>" : rootSourceFile.getAbsolutePath()),
                    Severity.ERROR));
            return diagnostics;
        }
        return new Run().validate();
    }

    /** State of one {@link #validate()} call; the walk over the import graph fills it. */
    private final class Run {
        private final List<Diagnostic> diagnostics = new ArrayList<>();
        private final Set<File> visited = new HashSet<>();
        private final List<File> fileOrder = new ArrayList<>();
        private final Set<String> knownWidgetNames = new HashSet<>();
        private final List<BlockWidget> blockWidgets = new ArrayList<>();
        private final Set<String> declaredProperties = new HashSet<>();
        private final List<VariableReference> variableReferences = new ArrayList<>();
        private List<String> rootLanguages;
        private GeneratorConfig rootConfig;

        List<Diagnostic> validate() {
            String body = readSafe(rootSourceFile);
            try {
                rootConfig = new GeneratorConfig(rootSourceFile, body, extraWidgets);
                rootConfig.setProjectRoot(projectRoot);
            } catch (RuntimeException e) {
                diagnostics.add(new Diagnostic(rootSourceFile, 0,
                        "cannot read the template configuration: " + e.getMessage(), Severity.ERROR));
                return diagnostics;
            }
            for (NRGWidget w : rootConfig.getWidgets()) {
                knownWidgetNames.add(w.getName());
                knownWidgetNames.addAll(w.getAliases());
                if (w instanceof BlockWidget) {
                    blockWidgets.add((BlockWidget) w);
                }
            }
            rootLanguages = readDeclaredLanguages(body);
            validateFile(rootSourceFile, rootConfig, rootLanguages);
            checkVariableReferences();
            sortByFileAndLine();
            return diagnostics;
        }

        private void validateFile(File file, GeneratorConfig config, List<String> inheritedLanguages) {
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
            fileOrder.add(file);

            // A file declaring its own nrg.languages overrides what was inherited; otherwise the
            // imported file uses the parent's declaration (imports don't have to repeat it).
            List<String> ownDeclaration = readOwnDeclaredLanguages(body);
            List<String> declaredLanguages = ownDeclaration != null ? ownDeclaration : inheritedLanguages;
            String[] lines = body.split("\\R", -1);

            Map<BlockWidget, Deque<Integer>> openBlocks = new LinkedHashMap<>();
            int ignoreBeginLine = -1;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNo = i + 1;
                // Lines that IgnoreBlockStripper drops before generation: they declare and
                // reference nothing, so only the structural rules look at them.
                boolean ignored = ignoreBeginLine >= 0
                        || IGNORE_BEGIN_PATTERN.matcher(line).find()
                        || IGNORE_END_PATTERN.matcher(line).find()
                        || IGNORE_LINE_PATTERN.matcher(line).find();

                checkWidgets(file, config, line, lineNo, ignored, declaredLanguages);
                checkLanguageMarkers(file, line, lineNo, declaredLanguages);

                if (IGNORE_BEGIN_PATTERN.matcher(line).find()) {
                    if (ignoreBeginLine >= 0) {
                        error(file, lineNo, "nested <!--nrg.ignore.begin--> without preceding <!--nrg.ignore.end-->");
                    } else {
                        ignoreBeginLine = lineNo;
                    }
                }
                if (IGNORE_END_PATTERN.matcher(line).find()) {
                    if (ignoreBeginLine < 0) {
                        error(file, lineNo, "stray <!--nrg.ignore.end--> without matching <!--nrg.ignore.begin-->");
                    } else {
                        ignoreBeginLine = -1;
                    }
                }

                if (!ignored) {
                    checkBlocks(file, line, lineNo, openBlocks);
                    collectDeclaredProperties(line);
                    collectVariableReferences(file, line, lineNo);
                    checkCosmeticPropertyEscape(file, line, lineNo);
                    checkDescriptorReferences(file, config, line, lineNo);
                }
            }

            diagnostics.addAll(FreezeValidator.validate(file, body, declaredLanguages));

            if (ignoreBeginLine >= 0) {
                error(file, ignoreBeginLine, "unclosed <!--nrg.ignore.begin--> (missing <!--nrg.ignore.end-->)");
            }
            for (Map.Entry<BlockWidget, Deque<Integer>> e : openBlocks.entrySet()) {
                for (Integer openerLine : e.getValue()) {
                    error(file, openerLine, "unclosed ${widget:" + e.getKey().getName() + "} block; add ${widget:"
                            + e.getKey().getCloserName() + "} on its own line after the block");
                }
            }
        }

        private void checkWidgets(File file, GeneratorConfig config, String line, int lineNo, boolean ignored,
                                  List<String> declaredLanguages) {
            Matcher widgetMatcher = TemplateLine.WIDGET_TAG_PATTERN.matcher(line);
            while (widgetMatcher.find()) {
                int start = widgetMatcher.start();
                if (start > 0 && line.charAt(start - 1) == '\\') {
                    continue; // escaped, not a real widget call
                }
                String name = widgetMatcher.group(1);
                String params = widgetMatcher.group(3);
                if (!knownWidgetNames.contains(name)) {
                    String suggestion = Suggestions.closest(name, knownWidgetNames);
                    error(file, lineNo, "unknown widget '" + name + "'" + (suggestion != null
                            ? "; did you mean '" + suggestion + "'?"
                            : "; register it with nrg.widgets / --widgets, or fix the name"));
                    continue;
                }
                if (!ignored) {
                    checkWidgetParameters(file, name, params, lineNo);
                }
                if ("import".equals(name)) {
                    checkImport(file, config, params, lineNo, declaredLanguages);
                }
            }
        }

        private void checkWidgetParameters(File file, String name, String params, int lineNo) {
            NRGWidget widget = rootConfig.getWidget(name);
            Set<String> allowed = widget == null ? null : widget.getParameterNames();
            if (allowed == null) {
                return;
            }
            for (String key : NRGUtil.parseParametersLine(params).keySet()) {
                if (allowed.contains(key)) {
                    continue;
                }
                String suggestion = Suggestions.closest(key, allowed);
                String hint;
                if (suggestion != null) {
                    hint = "; did you mean '" + suggestion + "'?";
                } else if (allowed.isEmpty()) {
                    hint = "; it takes no parameters";
                } else {
                    hint = "; known parameters: " + String.join(", ", allowed);
                }
                error(file, lineNo, "widget '" + name + "' has no parameter '" + key + "'" + hint);
            }
        }

        private void checkImport(File file, GeneratorConfig config, String params, int lineNo,
                                 List<String> declaredLanguages) {
            String path = extractImportPath(params);
            if (path == null) {
                return;
            }
            File importedFile = new File(file.getAbsoluteFile().getParentFile(), path);
            if (!ProjectRoot.contains(projectRoot, importedFile)) {
                // Reported without touching the file: following it would let a
                // template probe and parse files outside the project.
                error(file, lineNo, "import path is outside the project root: '" + path + "'");
            } else if (!importedFile.isFile()) {
                error(file, lineNo, "import path not found: '" + path + "'");
            } else if (!"false".equals(NRGUtil.parseParametersLine(params).get("run-generator"))) {
                validateFile(importedFile, childConfig(importedFile, config), declaredLanguages);
            }
        }

        /**
         * Mirrors how {@code ImportWidget} sets up the generator of an imported template, so that
         * descriptor references resolve against the same files: its own directory, the parent's
         * run context, and the parent's properties where the child declares none.
         */
        private GeneratorConfig childConfig(File importedFile, GeneratorConfig parent) {
            try {
                GeneratorConfig child = new GeneratorConfig(importedFile, readSafe(importedFile), extraWidgets);
                parent.passRunContextTo(child);
                parent.getProperties().forEach((key, value) -> {
                    if (!child.getProperties().containsKey(key)) {
                        child.getProperties().setProperty(key.toString(), String.valueOf(value));
                    }
                });
                return child;
            } catch (RuntimeException e) {
                return parent;
            }
        }

        private void checkLanguageMarkers(File file, String line, int lineNo, List<String> declaredLanguages) {
            Matcher langMatcher = LANG_MARKER_PATTERN.matcher(line);
            while (langMatcher.find()) {
                String marker = langMatcher.group(1);
                if (isReservedMarker(marker)) continue;
                if (declaredLanguages != null && !declaredLanguages.contains(marker)) {
                    String suggestion = Suggestions.closest(marker, declaredLanguages);
                    error(file, lineNo, "language marker '" + marker + "' is not declared in nrg.languages"
                            + (suggestion != null
                            ? "; did you mean '" + suggestion + "'?"
                            : "; add it to nrg.languages or remove the marker"));
                }
            }
        }

        private void checkBlocks(File file, String line, int lineNo, Map<BlockWidget, Deque<Integer>> openBlocks) {
            for (BlockWidget w : blockWidgets) {
                if (w.isBlockOpenerLine(line)) {
                    openBlocks.computeIfAbsent(w, k -> new ArrayDeque<>()).push(lineNo);
                } else if (w.isBlockCloserLine(line)) {
                    Deque<Integer> stack = openBlocks.get(w);
                    if (stack == null || stack.isEmpty()) {
                        error(file, lineNo, "${widget:" + w.getCloserName() + "} without a matching ${widget:"
                                + w.getName() + "}; remove it or add the opener above it");
                    } else {
                        stack.pop();
                    }
                }
            }
        }

        private void collectDeclaredProperties(String line) {
            for (String key : NRGUtil.extractRawPropertyMarkers(line).keySet()) {
                declaredProperties.add(key);
                // name.<lang> defines ${name} for that language.
                int dot = key.lastIndexOf('.');
                if (dot > 0 && rootLanguages.contains(key.substring(dot + 1))) {
                    declaredProperties.add(key.substring(0, dot));
                }
            }
        }

        private void collectVariableReferences(File file, String line, int lineNo) {
            if (line.indexOf("${") < 0) {
                return;
            }
            Matcher m = VARIABLE_PATTERN.matcher(line);
            while (m.find()) {
                if (m.start() > 0 && line.charAt(m.start() - 1) == '\\') {
                    continue;
                }
                String name = m.group(1);
                boolean sourceReference = false;
                for (String prefix : SOURCE_PREFIXES) {
                    sourceReference |= name.startsWith(prefix);
                }
                if (!sourceReference) {
                    variableReferences.add(new VariableReference(file, lineNo, name));
                }
            }
        }

        /**
         * Deferred until the whole import graph is read: a fragment may use a property that the
         * root template (or another fragment) declares further down.
         */
        private void checkVariableReferences() {
            for (VariableReference ref : variableReferences) {
                if (declaredProperties.contains(ref.name)) {
                    continue;
                }
                String suggestion = Suggestions.closest(ref.name, declaredProperties);
                if (suggestion != null) {
                    error(ref.file, ref.line, "undefined variable '" + ref.name + "'; did you mean '" + suggestion + "'?");
                } else {
                    diagnostics.add(new Diagnostic(ref.file, ref.line, "undefined variable '" + ref.name
                            + "' is left in the output as written; declare it with <!--@" + ref.name
                            + "=...--> or write \\${" + ref.name + "} if the text is meant literally",
                            Severity.WARNING));
                }
            }
        }

        private void checkCosmeticPropertyEscape(File file, String line, int lineNo) {
            Matcher m = COSMETIC_NRG_PROPERTY_PATTERN.matcher(line);
            while (m.find()) {
                String key = m.group(1);
                diagnostics.add(new Diagnostic(file, lineNo, "<!--\\@" + key + "=...--> sets " + key
                        + " for this template even inside an example; write <\\!--@" + key
                        + "=...--> to show it without declaring it", Severity.WARNING));
            }
        }

        private void checkDescriptorReferences(File file, GeneratorConfig config, String line, int lineNo) {
            if (!line.contains("pom.") && !line.contains("npm.") && !line.contains("gradle.")) {
                return;
            }
            List<String> unresolved;
            try {
                unresolved = new TemplateLine(config, line, lineNo - 1).findUnresolvedDescriptorReferences();
            } catch (RuntimeException e) {
                error(file, lineNo, e.getMessage());
                return;
            }
            for (String ref : unresolved) {
                diagnostics.add(new Diagnostic(file, lineNo, "${" + ref + "} is not defined in the descriptor"
                        + " and renders empty; fix the path or give a fallback: ${" + ref + ":...}",
                        Severity.WARNING));
            }
        }

        private void error(File file, int line, String message) {
            diagnostics.add(new Diagnostic(file, line, message, Severity.ERROR));
        }

        /** Groups diagnostics by file in the order the files were read, then by line. */
        private void sortByFileAndLine() {
            Map<File, Integer> order = new HashMap<>();
            for (File f : fileOrder) {
                order.put(f, order.size());
            }
            diagnostics.sort(Comparator
                    .comparingInt((Diagnostic d) -> order.getOrDefault(d.getFile(), Integer.MAX_VALUE))
                    .thenComparingInt(Diagnostic::getLine));
        }
    }

    private static final class VariableReference {
        final File file;
        final int line;
        final String name;

        VariableReference(File file, int line, String name) {
            this.file = file;
            this.line = line;
            this.name = name;
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
}
