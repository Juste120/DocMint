package io.github.juste120.docmint.infrastructure.pebble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** V2 user story 3: amounts in words (FR-110 to FR-113). */
class NumberSpellerTest {

  @ParameterizedTest(name = "{0} = {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "0|zéro",
        "1|un",
        "2|deux",
        "10|dix",
        "11|onze",
        "16|seize",
        "17|dix-sept",
        "19|dix-neuf",
        "20|vingt",
        "21|vingt et un",
        "22|vingt-deux",
        "30|trente",
        "31|trente et un",
        "41|quarante et un",
        "51|cinquante et un",
        "61|soixante et un",
        "69|soixante-neuf",
        "70|soixante-dix",
        "71|soixante et onze",
        "72|soixante-douze",
        "77|soixante-dix-sept",
        "79|soixante-dix-neuf",
        "80|quatre-vingts",
        "81|quatre-vingt-un",
        "85|quatre-vingt-cinq",
        "89|quatre-vingt-neuf",
        "90|quatre-vingt-dix",
        "91|quatre-vingt-onze",
        "99|quatre-vingt-dix-neuf",
        "100|cent",
        "101|cent un",
        "111|cent onze",
        "180|cent quatre-vingts",
        "200|deux cents",
        "201|deux cent un",
        "280|deux cent quatre-vingts",
        "300|trois cents",
        "999|neuf cent quatre-vingt-dix-neuf",
        "1000|mille",
        "1001|mille un",
        "1080|mille quatre-vingts",
        "1100|mille cent",
        "1200|mille deux cents",
        "1234|mille deux cent trente-quatre",
        "2000|deux mille",
        "21000|vingt et un mille",
        "80000|quatre-vingt mille",
        "81000|quatre-vingt-un mille",
        "100000|cent mille",
        "200000|deux cent mille",
        "280000|deux cent quatre-vingt mille",
        "250000|deux cent cinquante mille",
        "999999|neuf cent quatre-vingt-dix-neuf mille neuf cent quatre-vingt-dix-neuf",
        "1000000|un million",
        "1000001|un million un",
        "1250000|un million deux cent cinquante mille",
        "2000000|deux millions",
        "80000000|quatre-vingts millions",
        "200000000|deux cents millions",
        "1000000000|un milliard",
        "3000000000|trois milliards",
        "1000000000000|un billion",
        "2500000000000|deux billions cinq cents milliards",
        "999999999999999|neuf cent quatre-vingt-dix-neuf billions neuf cent quatre-vingt-dix-neuf milliards neuf cent quatre-vingt-dix-neuf millions neuf cent quatre-vingt-dix-neuf mille neuf cent quatre-vingt-dix-neuf"
      })
  void frenchTraditionalSpelling(long number, String expected) {
    assertThat(NumberSpeller.french(number, false)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0} = {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "21|vingt-et-un",
        "71|soixante-et-onze",
        "250000|deux-cent-cinquante-mille",
        "1250000|un million deux-cent-cinquante-mille",
        "2000000|deux millions",
        "1234|mille-deux-cent-trente-quatre",
        "80|quatre-vingts",
        "3200000000|trois milliards deux-cents millions"
      })
  void frenchReformedSpelling(long number, String expected) {
    assertThat(NumberSpeller.french(number, true)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0} = {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "0|zero",
        "7|seven",
        "13|thirteen",
        "20|twenty",
        "21|twenty-one",
        "99|ninety-nine",
        "100|one hundred",
        "101|one hundred one",
        "999|nine hundred ninety-nine",
        "1000|one thousand",
        "1234|one thousand two hundred thirty-four",
        "1250000|one million two hundred fifty thousand",
        "1000000000|one billion",
        "2000000000000|two trillion",
        "999999999999999|nine hundred ninety-nine trillion nine hundred ninety-nine billion nine hundred ninety-nine million nine hundred ninety-nine thousand nine hundred ninety-nine"
      })
  void english(long number, String expected) {
    assertThat(NumberSpeller.english(number)).isEqualTo(expected);
  }

  @Test
  void frenchInvariantsHoldForTheFirstMillion() {
    for (long n = 0; n <= 1_000_000; n++) {
      String words = NumberSpeller.french(n, false);
      assertThat(words).doesNotContain("  ").doesNotStartWith(" ").doesNotEndWith(" ");
      assertThat(words).doesNotStartWith("un cent").doesNotStartWith("un mille");
      // "cents" and "vingts" only at the very end of a number below one million.
      if (words.contains("cents") || words.contains("vingts")) {
        assertThat(words).as("%d -> %s", n, words).matches(".*(cents|vingts)$");
      }
      String english = NumberSpeller.english(n);
      assertThat(english).doesNotContain("  ").doesNotContain(" and ");
    }
  }

  @Test
  void rangeIsChecked() {
    assertThatThrownBy(() -> NumberSpeller.french(-1, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NumberSpeller.english(NumberSpeller.MAX + 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- filter -----------------------------------------------------------------------------

  private static String words(Object value, String currency, String locale, String spelling) {
    Map<String, Object> args = new HashMap<>();
    args.put("currency", currency);
    args.put("locale", locale);
    args.put("spelling", spelling);
    return FiltersHarness.apply(new WordsFilter(), value, args, Locale.FRENCH);
  }

  @ParameterizedTest(name = "{0} {1} {2} = {3}")
  @CsvSource(
      delimiter = '|',
      nullValues = "null",
      value = {
        "1250000|XOF|null|un million deux cent cinquante mille francs CFA",
        "1250000|fcfa|null|un million deux cent cinquante mille francs CFA",
        "1000000|XAF|null|un million de francs CFA",
        "3000000|EUR|null|trois millions d'euros",
        "1|XOF|null|un franc CFA",
        "0|XOF|null|zéro franc CFA",
        "1250.4|XOF|null|mille deux cent cinquante francs CFA",
        "1250.5|XOF|null|mille deux cent cinquante et un francs CFA",
        "1234.50|EUR|null|mille deux cent trente-quatre euros et cinquante centimes",
        "1.01|EUR|null|un euro et un centime",
        "0.75|EUR|null|soixante-quinze centimes",
        "-15000|XOF|null|moins quinze mille francs CFA",
        "1234.5|null|null|mille deux cent trente-quatre virgule cinq",
        "3.05|null|null|trois virgule zéro cinq",
        "1250000|null|null|un million deux cent cinquante mille",
        "1250000|XOF|en|one million two hundred fifty thousand CFA francs",
        "1234.50|EUR|en|one thousand two hundred thirty-four euros and fifty cents",
        "1.5|null|en|one point five",
        "-2|USD|en|minus two dollars"
      })
  void currencies(String value, String currency, String locale, String expected) {
    assertThat(words(new BigDecimal(value), currency, locale, null)).isEqualTo(expected);
  }

  @Test
  void reformedSpellingAndNulls() {
    assertThat(words(new BigDecimal("21"), "XOF", null, "1990"))
        .isEqualTo("vingt-et-un francs CFA");
    assertThat(words(null, "XOF", null, null)).isEmpty();
    assertThatThrownBy(() -> words(BigDecimal.ONE, "GBP", null, null))
        .hasMessageContaining("does not know currency");
    assertThatThrownBy(() -> words(new BigDecimal("1e15"), null, null, null))
        .hasMessageContaining("supports amounts up to");
  }
}
