package io.github.juste120.agbale.application;

import java.util.Collection;
import java.util.Locale;

/** Suggests the closest existing name for a misspelt one (Levenshtein distance). */
final class NameSuggester {

  private NameSuggester() {}

  /**
   * Returns the candidate closest to {@code wanted}, or {@code null} if none is close enough.
   *
   * @param wanted the misspelt name
   * @param candidates the existing names
   * @return the suggestion or {@code null}
   */
  static String suggest(String wanted, Collection<String> candidates) {
    String target = wanted.toLowerCase(Locale.ROOT);
    int threshold = Math.max(2, target.length() / 3);
    String best = null;
    int bestDistance = Integer.MAX_VALUE;
    for (String candidate : candidates) {
      int distance = distance(target, candidate.toLowerCase(Locale.ROOT));
      if (distance < bestDistance) {
        bestDistance = distance;
        best = candidate;
      }
    }
    return bestDistance <= threshold ? best : null;
  }

  static int distance(String a, String b) {
    int[] previous = new int[b.length() + 1];
    int[] current = new int[b.length() + 1];
    for (int j = 0; j <= b.length(); j++) {
      previous[j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      current[0] = i;
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[b.length()];
  }
}
