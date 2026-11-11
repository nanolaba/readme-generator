package com.nanolaba.nrg.core;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translation-parity rules of {@code --validate}: finds content that exists in one language of
 * a template and is missing from another. Keeping languages together is what NRG is for, and a
 * missing translation is the mistake nothing else catches — the paragraph tagged
 * {@code <!--en-->} is added, its {@code <!--ru-->} twin is forgotten, and every other check
 * stays green.
 *
 * <p>The template is cut into <em>groups</em>: runs of language-tagged lines, where blank lines
 * (and lines that render nothing, such as property definitions) do not end a group but any
 * other untagged line does. Shared lines are identical in every output, so all divergence
 * lives inside groups. Three rules apply:
 * <ol>
 *   <li>a group must have lines for every declared language (ERROR);</li>
 *   <li>a {@code ${en:'…', ru:'…'}} construct must name every language the line is shown in,
 *       and only declared ones (ERROR — a missing entry renders empty, an undeclared one leaves
 *       the whole construct in the output as written);</li>
 *   <li>inside a group every language must have the same skeleton — the same sequence of
 *       headings (by level), list items, table rows, code fences, quotes and paragraphs
 *       (WARNING, since translations legitimately differ in how they wrap text).</li>
 * </ol>
 * A line carrying {@code <!--nrg.parity.ignore-->} is left out of all three; the property
 * {@code nrg.validate.parity=false} turns the rules off for a template.
 */
final class TranslationParityChecker {

    private static final Pattern MARKER = Pattern.compile("<!--\\s*([A-Za-z][A-Za-z0-9_-]*)\\s*-->");
    private static final Pattern IGNORE_INLINE = Pattern.compile("<!--\\s*nrg\\.ignore\\s*-->");
    private static final Pattern IGNORE_BEGIN = Pattern.compile("<!--\\s*nrg\\.ignore\\.begin\\s*-->");
    private static final Pattern IGNORE_END = Pattern.compile("<!--\\s*nrg\\.ignore\\.end\\s*-->");
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s");
    private static final Pattern LIST_ITEM = Pattern.compile("^([-*+]|\\d+[.)])\\s");
    private static final Pattern LANGUAGE_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

    private final File file;
    private final List<String> languages;
    private final List<Validator.Diagnostic> diagnostics = new ArrayList<>();

    private TranslationParityChecker(File file, List<String> languages) {
        this.file = file;
        this.languages = languages;
    }

    static List<Validator.Diagnostic> check(File file, String[] lines, List<String> languages) {
        if (languages == null || languages.size() < 2) {
            return Collections.emptyList();
        }
        TranslationParityChecker checker = new TranslationParityChecker(file, languages);
        checker.run(lines);
        return checker.diagnostics;
    }

    private void run(String[] lines) {
        List<TaggedLine> group = new ArrayList<>();
        boolean inIgnoreBlock = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean ignoreMarker = IGNORE_BEGIN.matcher(line).find() || IGNORE_END.matcher(line).find();
            if (IGNORE_BEGIN.matcher(line).find()) {
                inIgnoreBlock = true;
            }
            boolean skipped = inIgnoreBlock || ignoreMarker || IGNORE_INLINE.matcher(line).find()
                    || line.contains(NRGConstants.PARITY_IGNORE_MARKER);
            if (IGNORE_END.matcher(line).find()) {
                inIgnoreBlock = false;
            }
            if (skipped) {
                continue; // neutral: neither part of a group nor ending one
            }
            Set<String> tags = tagsOf(line);
            checkLanguageConstructs(line, i + 1, tags.isEmpty() ? languages : tags);
            if (!tags.isEmpty()) {
                group.add(new TaggedLine(i + 1, tags, line));
            } else if (!rendersNothing(line)) {
                closeGroup(group);
                group = new ArrayList<>();
            }
        }
        closeGroup(group);
    }

    private Set<String> tagsOf(String line) {
        Set<String> tags = new LinkedHashSet<>();
        Matcher m = MARKER.matcher(line);
        while (m.find()) {
            if (languages.contains(m.group(1))) {
                tags.add(m.group(1));
            }
        }
        return tags;
    }

    /** Blank lines and lines holding only property definitions: they do not end a group. */
    private static boolean rendersNothing(String line) {
        String visible = NRGUtil.stripPropertyComments(line);
        for (int i = 0; i < visible.length(); i++) {
            if (!Character.isWhitespace(visible.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void closeGroup(List<TaggedLine> group) {
        if (group.isEmpty()) {
            return;
        }
        Set<String> present = new LinkedHashSet<>();
        for (TaggedLine t : group) {
            present.addAll(t.tags);
        }
        int first = group.get(0).lineNo;
        int last = group.get(group.size() - 1).lineNo;
        String range = first == last ? "line " + first : "lines " + first + "-" + last;
        List<String> missing = new ArrayList<>(languages);
        missing.removeAll(present);
        if (!missing.isEmpty()) {
            diagnostics.add(new Validator.Diagnostic(file, first, Validator.Severity.ERROR, "missing-translation",
                    range + (first == last ? " exists" : " exist") + " for " + quoteAll(present)
                            + " but not for " + quoteAll(missing),
                    "add the translation next to them, or mark a line that is meant for one language only with "
                            + NRGConstants.PARITY_IGNORE_MARKER));
            return;
        }
        compareSkeletons(group, range);
    }

    /** Rule 3: report the first place where two languages' skeletons diverge. */
    private void compareSkeletons(List<TaggedLine> group, String range) {
        Map<String, List<Element>> skeletons = new LinkedHashMap<>();
        for (String language : languages) {
            skeletons.put(language, skeletonOf(group, language));
        }
        String reference = languages.get(0);
        List<Element> expected = skeletons.get(reference);
        for (String language : languages.subList(1, languages.size())) {
            List<Element> actual = skeletons.get(language);
            int n = Math.max(expected.size(), actual.size());
            for (int k = 0; k < n; k++) {
                Element a = k < expected.size() ? expected.get(k) : null;
                Element b = k < actual.size() ? actual.get(k) : null;
                if (a != null && b != null && a.kind.equals(b.kind)) {
                    continue;
                }
                int at = b != null ? b.lineNo : a != null ? a.lineNo : group.get(0).lineNo;
                diagnostics.add(new Validator.Diagnostic(file, at, Validator.Severity.WARNING, "structure-mismatch",
                        "translations in " + range + " differ in structure: '" + reference + "' has " + describe(a)
                                + " where '" + language + "' has " + describe(b),
                        "check that nothing was added to or left out of one language"));
                break;
            }
        }
    }

    private static List<Element> skeletonOf(List<TaggedLine> group, String language) {
        List<Element> skeleton = new ArrayList<>();
        int previousLine = -2;
        boolean inFence = false;
        for (TaggedLine t : group) {
            if (!t.tags.contains(language)) {
                continue;
            }
            boolean startsBlock = t.lineNo != previousLine + 1;
            previousLine = t.lineNo;
            String text = MARKER.matcher(t.text).replaceAll("").trim();
            if (text.startsWith("```") || text.startsWith("~~~")) {
                skeleton.add(new Element("a code fence", t.lineNo));
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                continue; // code is compared as a whole by its fences, not line by line
            }
            Matcher heading = HEADING.matcher(text);
            if (heading.find()) {
                skeleton.add(new Element("a level-" + heading.group(1).length() + " heading", t.lineNo));
            } else if (LIST_ITEM.matcher(text).find()) {
                skeleton.add(new Element("a list item", t.lineNo));
            } else if (text.startsWith("|")) {
                skeleton.add(new Element("a table row", t.lineNo));
            } else if (startsBlock) {
                skeleton.add(new Element(text.startsWith(">") ? "a quote" : "a paragraph", t.lineNo));
            }
        }
        return skeleton;
    }

    private static String describe(Element e) {
        return e == null ? "nothing more" : e.kind + " (line " + e.lineNo + ")";
    }

    /**
     * Rule 2. Parses every unescaped {@code ${key:'…', key:"…"}} construct on the line; text
     * that does not have that shape ({@code ${widget:…}}, {@code ${var}}) is skipped.
     */
    private void checkLanguageConstructs(String line, int lineNo, Collection<String> shownIn) {
        int from = 0;
        while (true) {
            int start = line.indexOf("${", from);
            if (start < 0) {
                return;
            }
            from = start + 2;
            if (start > 0 && line.charAt(start - 1) == '\\') {
                continue;
            }
            List<String> keys = parseConstructKeys(line, start + 2);
            if (keys == null) {
                continue;
            }
            List<String> undeclared = new ArrayList<>();
            for (String key : keys) {
                if (!languages.contains(key)) {
                    undeclared.add(key);
                }
            }
            if (!undeclared.isEmpty()) {
                diagnostics.add(new Validator.Diagnostic(file, lineNo, Validator.Severity.ERROR, "language-construct",
                        "language construct names " + quoteAll(undeclared) + ", which "
                                + (undeclared.size() == 1 ? "is" : "are")
                                + " not in nrg.languages, so the whole construct is left in the output as written",
                        "declare the language or remove the entry"));
                continue;
            }
            List<String> missing = new ArrayList<>();
            for (String language : shownIn) {
                if (!keys.contains(language)) {
                    missing.add(language);
                }
            }
            if (!missing.isEmpty()) {
                diagnostics.add(new Validator.Diagnostic(file, lineNo, Validator.Severity.ERROR, "language-construct",
                        "language construct has no text for " + quoteAll(missing) + " and renders empty there",
                        "add " + missing.get(0) + ":'...' (or " + missing.get(0) + ":'' if empty is intended)"));
            }
        }
    }

    /**
     * Keys of a language construct whose body starts at {@code pos} (just after {@code ${}),
     * or {@code null} when the text there is not a language construct.
     */
    static List<String> parseConstructKeys(String s, int pos) {
        List<String> keys = new ArrayList<>();
        int i = pos;
        while (true) {
            i = skipSpaces(s, i);
            Matcher key = LANGUAGE_KEY.matcher(s).region(i, s.length());
            if (!key.lookingAt()) {
                return null;
            }
            i = skipSpaces(s, key.end());
            if (i >= s.length() || s.charAt(i) != ':') {
                return null;
            }
            i = skipSpaces(s, i + 1);
            if (i >= s.length() || (s.charAt(i) != '\'' && s.charAt(i) != '"')) {
                return null;
            }
            char quote = s.charAt(i);
            i++;
            while (true) {
                if (i >= s.length()) {
                    return null;
                }
                if (s.charAt(i) == quote) {
                    if (i + 1 < s.length() && s.charAt(i + 1) == quote) {
                        i += 2; // doubled quote is a literal one
                        continue;
                    }
                    i++;
                    break;
                }
                i++;
            }
            keys.add(key.group());
            i = skipSpaces(s, i);
            if (i < s.length() && s.charAt(i) == ',') {
                i++;
                continue;
            }
            return i < s.length() && s.charAt(i) == '}' ? keys : null;
        }
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static String quoteAll(Collection<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            if (sb.length() > 0) sb.append(", ");
            sb.append('\'').append(n).append('\'');
        }
        return sb.toString();
    }

    private static final class TaggedLine {
        final int lineNo;
        final Set<String> tags;
        final String text;

        TaggedLine(int lineNo, Set<String> tags, String text) {
            this.lineNo = lineNo;
            this.tags = tags;
            this.text = text;
        }
    }

    private static final class Element {
        final String kind;
        final int lineNo;

        Element(String kind, int lineNo) {
            this.kind = kind;
            this.lineNo = lineNo;
        }
    }
}
