package com.nanolaba.nrg.core;

import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;

/**
 * "Did you mean …?" lookup for {@code --validate} diagnostics: finds the known name closest to
 * a misspelt one. Templates are increasingly written by coding agents from memory of the docs,
 * and their mistakes are near-misses ({@code tocc}, {@code maxDepth}, {@code stableVerison}) —
 * naming the intended spelling turns a diagnostic into a one-step fix.
 *
 * <p>Closeness is the Levenshtein distance between the lower-cased names, so a difference in
 * case alone always matches. The allowed distance grows with the length of the misspelt name
 * (1 up to four characters, 2 above) to keep short names from matching everything. Ties go to
 * the alphabetically first candidate, which keeps the output deterministic.
 */
public final class Suggestions {

    private Suggestions() {/**/}

    /**
     * @return the candidate closest to {@code name} within the allowed distance, or {@code null}
     *         when there is none or when {@code name} itself is a candidate.
     */
    public static String closest(String name, Collection<String> candidates) {
        if (name == null || candidates == null || candidates.contains(name)) {
            return null;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        int limit = lowerName.length() <= 4 ? 1 : 2;
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : new TreeSet<>(candidates)) {
            int d = distance(lowerName, candidate.toLowerCase(Locale.ROOT));
            if (d <= limit && d < bestDistance) {
                best = candidate;
                bestDistance = d;
            }
        }
        return best;
    }

    /** Levenshtein distance with a two-row table. */
    static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
