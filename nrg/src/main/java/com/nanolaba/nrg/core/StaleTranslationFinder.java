package com.nanolaba.nrg.core;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Finds translations that may have gone stale: places where a template changed in one language
 * and not in another since an earlier version of it. Translation parity ({@code --validate})
 * catches a missing translation; it cannot catch an outdated one — the English sentence is
 * rewritten, the Russian line next to it is left as it was, every check stays green and the two
 * outputs now say different things. Backs {@code --stale-translations <git-ref>}.
 *
 * <p>The old and the new text of one file are compared line by line (a longest-common-subsequence
 * diff after the common head and tail are cut off). Inside every translation group — the runs
 * of language-tagged lines {@link TranslationParityChecker} works with — a language counts as
 * changed when one of its lines was added, edited or removed. A group where some languages
 * changed and others did not is reported, and so is an edited {@code ${en:'…', ru:'…'}}
 * construct whose entries changed for some languages only. A line carrying
 * {@code <!--nrg.translation.ok-->} acknowledges a one-sided edit (a typo fix that has no
 * counterpart) and silences its group or construct.
 */
public final class StaleTranslationFinder {

    /** One place where languages were edited unevenly. */
    public static final class Finding {
        private final File file;
        private final int firstLine;
        private final int lastLine;
        private final String kind;
        private final Set<String> changed;
        private final Set<String> unchanged;
        private final Map<String, String> oldText;
        private final Map<String, String> newText;

        Finding(File file, int firstLine, int lastLine, String kind, Set<String> changed, Set<String> unchanged,
                Map<String, String> oldText, Map<String, String> newText) {
            this.file = file;
            this.firstLine = firstLine;
            this.lastLine = lastLine;
            this.kind = kind;
            this.changed = changed;
            this.unchanged = unchanged;
            this.oldText = oldText;
            this.newText = newText;
        }

        public File getFile() {
            return file;
        }

        /** 1-based, in the current version of the file. */
        public int getFirstLine() {
            return firstLine;
        }

        public int getLastLine() {
            return lastLine;
        }

        /** {@code block} (a translation group) or {@code construct} (a {@code ${en:…}} construct). */
        public String getKind() {
            return kind;
        }

        public Set<String> getChanged() {
            return changed;
        }

        public Set<String> getUnchanged() {
            return unchanged;
        }

        /** The removed / previous text of each changed language (may be empty for added lines). */
        public Map<String, String> getOldText() {
            return oldText;
        }

        /** The added / current text of each changed language (may be empty for removed lines). */
        public Map<String, String> getNewText() {
            return newText;
        }
    }

    private StaleTranslationFinder() {/**/}

    /**
     * @param languages the languages the file is rendered in ({@code nrg.languages} of its root template)
     */
    public static List<Finding> find(File file, String oldBody, String newBody, List<String> languages) {
        List<Finding> findings = new ArrayList<>();
        if (languages == null || languages.size() < 2) {
            return findings;
        }
        String[] a = lines(oldBody);
        String[] b = lines(newBody);
        Diff diff = new Diff(a, b);

        for (List<Integer> group : TranslationParityChecker.groupLineNumbers(b, languages)) {
            int first = group.get(0);
            int last = group.get(group.size() - 1);
            boolean acknowledged = false;
            Set<String> present = new LinkedHashSet<>();
            Set<String> changed = new LinkedHashSet<>();
            Map<String, StringBuilder> oldText = new LinkedHashMap<>();
            Map<String, StringBuilder> newText = new LinkedHashMap<>();
            for (Integer n : group) {
                String line = b[n - 1];
                acknowledged |= line.contains(NRGConstants.TRANSLATION_OK_MARKER);
                Set<String> tags = TranslationParityChecker.languageTags(line, languages);
                present.addAll(tags);
                if (!diff.newMatched[n - 1]) {
                    changed.addAll(tags);
                    String shown = withoutMarkers(line, languages);
                    tags.forEach(l -> append(newText, l, shown));
                }
            }
            // Removed old lines whose position falls inside the group (or right after its end).
            for (int i = 0; i < a.length; i++) {
                if (diff.oldMatched[i]) {
                    continue;
                }
                int at = diff.oldPositionInNew[i] + 1; // 1-based line of b the removal sits before
                if (at >= first && at <= last + 1) {
                    Set<String> tags = TranslationParityChecker.languageTags(a[i], languages);
                    present.addAll(tags);
                    changed.addAll(tags);
                    String removed = withoutMarkers(a[i], languages);
                    tags.forEach(l -> append(oldText, l, removed));
                }
            }
            Set<String> unchanged = new LinkedHashSet<>(present);
            unchanged.removeAll(changed);
            if (!acknowledged && !changed.isEmpty() && !unchanged.isEmpty()) {
                findings.add(new Finding(file, first, last, "block", changed, unchanged,
                        texts(oldText, changed), texts(newText, changed)));
            }
        }

        for (int n = 1; n <= b.length; n++) {
            Integer old = diff.pairedOld.get(n - 1);
            if (old == null || b[n - 1].contains(NRGConstants.TRANSLATION_OK_MARKER)) {
                continue;
            }
            Set<String> tags = TranslationParityChecker.languageTags(b[n - 1], languages);
            findConstructChanges(file, n, a[old], b[n - 1], tags.isEmpty() ? languages : new ArrayList<>(tags), findings);
        }
        findings.sort(Comparator.comparingInt(Finding::getFirstLine));
        return findings;
    }

    private static void findConstructChanges(File file, int lineNo, String oldLine, String newLine,
                                             List<String> shownIn, List<Finding> findings) {
        List<Map<String, String>> before = constructs(oldLine);
        List<Map<String, String>> after = constructs(newLine);
        if (before.size() != after.size()) {
            return; // constructs added or removed: not a one-sided translation edit
        }
        for (int k = 0; k < after.size(); k++) {
            Set<String> changed = new LinkedHashSet<>();
            Set<String> unchanged = new LinkedHashSet<>();
            Map<String, String> oldText = new LinkedHashMap<>();
            Map<String, String> newText = new LinkedHashMap<>();
            for (String language : shownIn) {
                String was = before.get(k).get(language);
                String now = after.get(k).get(language);
                if (was == null || now == null) {
                    continue;
                }
                if (was.equals(now)) {
                    unchanged.add(language);
                } else {
                    changed.add(language);
                    oldText.put(language, was);
                    newText.put(language, now);
                }
            }
            if (!changed.isEmpty() && !unchanged.isEmpty()) {
                findings.add(new Finding(file, lineNo, lineNo, "construct", changed, unchanged, oldText, newText));
            }
        }
    }

    private static List<Map<String, String>> constructs(String line) {
        List<Map<String, String>> result = new ArrayList<>();
        int from = 0;
        while (true) {
            int start = line.indexOf("${", from);
            if (start < 0) {
                return result;
            }
            from = start + 2;
            if (start > 0 && line.charAt(start - 1) == '\\') {
                continue;
            }
            Map<String, String> values = TranslationParityChecker.parseConstruct(line, start + 2);
            if (values != null) {
                result.add(values);
            }
        }
    }

    /** The line as a reader sees it: language markers and the acknowledgement marker removed. */
    private static String withoutMarkers(String line, List<String> languages) {
        String result = line.replace(NRGConstants.TRANSLATION_OK_MARKER, "");
        for (String language : languages) {
            result = result.replaceAll("<!--\\s*" + Pattern.quote(language) + "\\s*-->", "");
        }
        return result.trim();
    }

    private static void append(Map<String, StringBuilder> texts, String language, String line) {
        StringBuilder sb = texts.computeIfAbsent(language, k -> new StringBuilder());
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(line);
    }

    private static Map<String, String> texts(Map<String, StringBuilder> texts, Set<String> languages) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String language : languages) {
            StringBuilder sb = texts.get(language);
            result.put(language, sb == null ? "" : sb.toString());
        }
        return result;
    }

    private static String[] lines(String body) {
        if (body == null || body.isEmpty()) {
            return new String[0];
        }
        String[] lines = body.split("\\R", -1);
        return body.endsWith("\n") || body.endsWith("\r") ? Arrays.copyOf(lines, lines.length - 1) : lines;
    }

    /**
     * Line diff of two versions: which lines survive unchanged, where each removed old line sits
     * in the new text, and which new line replaces which old one within a changed hunk (the k-th
     * added line pairs with the k-th removed one), so that an edited line can be compared with
     * its previous version.
     */
    private static final class Diff {
        final boolean[] oldMatched;
        final boolean[] newMatched;
        final int[] oldPositionInNew;
        final Map<Integer, Integer> pairedOld = new HashMap<>();

        Diff(String[] a, String[] b) {
            oldMatched = new boolean[a.length];
            newMatched = new boolean[b.length];
            oldPositionInNew = new int[a.length];
            int head = 0;
            while (head < a.length && head < b.length && a[head].equals(b[head])) {
                oldMatched[head] = newMatched[head] = true;
                head++;
            }
            int tail = 0;
            while (tail < a.length - head && tail < b.length - head
                    && a[a.length - 1 - tail].equals(b[b.length - 1 - tail])) {
                oldMatched[a.length - 1 - tail] = newMatched[b.length - 1 - tail] = true;
                tail++;
            }
            int n = a.length - head - tail;
            int m = b.length - head - tail;
            int[][] lcs = new int[n + 1][m + 1];
            for (int i = n - 1; i >= 0; i--) {
                for (int j = m - 1; j >= 0; j--) {
                    lcs[i][j] = a[head + i].equals(b[head + j])
                            ? lcs[i + 1][j + 1] + 1
                            : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
            int i = 0;
            int j = 0;
            List<Integer> removed = new ArrayList<>();
            List<Integer> added = new ArrayList<>();
            while (i < n || j < m) {
                if (i < n && j < m && a[head + i].equals(b[head + j])) {
                    pairHunk(removed, added);
                    oldMatched[head + i] = newMatched[head + j] = true;
                    i++;
                    j++;
                } else if (j < m && (i == n || lcs[i][j + 1] >= lcs[i + 1][j])) {
                    added.add(head + j);
                    j++;
                } else {
                    oldPositionInNew[head + i] = head + j;
                    removed.add(head + i);
                    i++;
                }
            }
            pairHunk(removed, added);
            for (int k = a.length - tail; k < a.length; k++) {
                oldPositionInNew[k] = k - a.length + b.length;
            }
            for (int k = 0; k < head; k++) {
                oldPositionInNew[k] = k;
            }
        }

        private void pairHunk(List<Integer> removed, List<Integer> added) {
            for (int k = 0; k < Math.min(removed.size(), added.size()); k++) {
                pairedOld.put(added.get(k), removed.get(k));
            }
            removed.clear();
            added.clear();
        }
    }
}
