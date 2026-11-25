package io.github.juste120.docmint.infrastructure.pebble;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Spells whole numbers in French and English, from 0 to {@value #MAX}.
 *
 * <p>French follows the traditional spelling by default (<em>vingt et un</em>,
 * <em>quatre-vingts</em>, <em>deux cents</em>, <em>mille</em> invariable, <em>cent</em> and
 * <em>vingt</em> invariable before <em>mille</em>) with the long scale (10<sup>12</sup> =
 * <em>billion</em>). The 1990 reformed spelling joins every numeral with hyphens
 * (<em>deux-cent-cinquante-mille</em>); <em>million</em>, <em>milliard</em> and <em>billion</em>
 * are nouns and stay separated by spaces. English uses the short scale without "and".
 */
final class NumberSpeller {

  /** Largest supported number. */
  static final long MAX = 999_999_999_999_999L;

  private static final String[] FR_UNITS = {
    "zéro",
    "un",
    "deux",
    "trois",
    "quatre",
    "cinq",
    "six",
    "sept",
    "huit",
    "neuf",
    "dix",
    "onze",
    "douze",
    "treize",
    "quatorze",
    "quinze",
    "seize",
    "dix-sept",
    "dix-huit",
    "dix-neuf"
  };
  private static final String[] FR_TENS = {
    "", "", "vingt", "trente", "quarante", "cinquante", "soixante"
  };
  private static final String[] EN_UNITS = {
    "zero",
    "one",
    "two",
    "three",
    "four",
    "five",
    "six",
    "seven",
    "eight",
    "nine",
    "ten",
    "eleven",
    "twelve",
    "thirteen",
    "fourteen",
    "fifteen",
    "sixteen",
    "seventeen",
    "eighteen",
    "nineteen"
  };
  private static final String[] EN_TENS = {
    "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
  };
  private static final Set<String> FR_NOUNS =
      Set.of("million", "millions", "milliard", "milliards", "billion", "billions");

  private NumberSpeller() {}

  static String french(long number, boolean reformed) {
    check(number);
    if (number == 0) {
      return FR_UNITS[0];
    }
    List<String> parts = new ArrayList<>();
    long[] scales = {1_000_000_000_000L, 1_000_000_000L, 1_000_000L};
    String[] names = {"billion", "milliard", "million"};
    long rest = number;
    for (int i = 0; i < scales.length; i++) {
      int group = (int) (rest / scales[i]);
      rest %= scales[i];
      if (group > 0) {
        parts.add(frBelow1000(group, true) + " " + names[i] + (group > 1 ? "s" : ""));
      }
    }
    int thousands = (int) (rest / 1000);
    int units = (int) (rest % 1000);
    if (thousands == 1) {
      parts.add("mille");
    } else if (thousands > 1) {
      parts.add(frBelow1000(thousands, false) + " mille");
    }
    if (units > 0) {
      parts.add(frBelow1000(units, true));
    }
    String text = String.join(" ", parts);
    return reformed ? reform(text) : text;
  }

  /** Below 1000; {@code last} is false before "mille", where cent and vingt stay invariable. */
  private static String frBelow1000(int number, boolean last) {
    int hundreds = number / 100;
    int rest = number % 100;
    String tail = rest == 0 ? "" : frBelow100(rest, last);
    if (hundreds == 0) {
      return tail;
    }
    String head = hundreds == 1 ? "cent" : FR_UNITS[hundreds] + " cent";
    if (rest == 0) {
      return hundreds > 1 && last ? head + "s" : head;
    }
    return head + " " + tail;
  }

  private static String frBelow100(int number, boolean last) {
    if (number < 20) {
      return FR_UNITS[number];
    }
    if (number < 70) {
      int tens = number / 10;
      int unit = number % 10;
      if (unit == 0) {
        return FR_TENS[tens];
      }
      return FR_TENS[tens] + (unit == 1 ? " et un" : "-" + FR_UNITS[unit]);
    }
    if (number < 80) {
      return number == 71 ? "soixante et onze" : "soixante-" + FR_UNITS[number - 60];
    }
    if (number == 80) {
      return last ? "quatre-vingts" : "quatre-vingt";
    }
    return "quatre-vingt-" + FR_UNITS[number - 80];
  }

  private static String reform(String text) {
    String[] tokens = text.split(" ");
    StringBuilder out = new StringBuilder(tokens[0]);
    for (int i = 1; i < tokens.length; i++) {
      boolean noun = FR_NOUNS.contains(tokens[i]) || FR_NOUNS.contains(tokens[i - 1]);
      out.append(noun ? " " : "-").append(tokens[i]);
    }
    return out.toString();
  }

  static String english(long number) {
    check(number);
    if (number == 0) {
      return EN_UNITS[0];
    }
    long[] scales = {1_000_000_000_000L, 1_000_000_000L, 1_000_000L, 1000L, 1L};
    String[] names = {"trillion", "billion", "million", "thousand", ""};
    List<String> parts = new ArrayList<>();
    long rest = number;
    for (int i = 0; i < scales.length; i++) {
      int group = (int) (rest / scales[i]);
      rest %= scales[i];
      if (group > 0) {
        String words = enBelow1000(group);
        parts.add(names[i].isEmpty() ? words : words + " " + names[i]);
      }
    }
    return String.join(" ", parts);
  }

  private static String enBelow1000(int number) {
    int hundreds = number / 100;
    int rest = number % 100;
    String tail;
    if (rest == 0) {
      tail = "";
    } else if (rest < 20) {
      tail = EN_UNITS[rest];
    } else {
      tail = EN_TENS[rest / 10] + (rest % 10 == 0 ? "" : "-" + EN_UNITS[rest % 10]);
    }
    if (hundreds == 0) {
      return tail;
    }
    return EN_UNITS[hundreds] + " hundred" + (tail.isEmpty() ? "" : " " + tail);
  }

  private static void check(long number) {
    if (number < 0 || number > MAX) {
      throw new IllegalArgumentException("number out of range: 0 to " + MAX);
    }
  }
}
