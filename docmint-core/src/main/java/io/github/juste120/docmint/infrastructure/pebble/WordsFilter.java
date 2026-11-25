package io.github.juste120.docmint.infrastructure.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code {{ total | words("XOF") }}} spells an amount out: {@code un million deux cent cinquante
 * mille francs CFA}. Arguments: {@code currency} ({@code XOF}, {@code XAF}, {@code fcfa}, {@code
 * EUR}, {@code USD}; none for a plain number), {@code locale} (French by default, English
 * supported), {@code spelling} ({@code "1990"} for the reformed French spelling).
 */
final class WordsFilter implements Filter {

  static final String NAME = "words";

  /** Spelling of a currency in one language. */
  private record Unit(String singular, String plural, String subSingular, String subPlural) {
    boolean hasSubunit() {
      return subSingular != null;
    }
  }

  private static final Map<String, Unit> FRENCH =
      Map.of(
          "XOF", new Unit("franc CFA", "francs CFA", null, null),
          "XAF", new Unit("franc CFA", "francs CFA", null, null),
          "EUR", new Unit("euro", "euros", "centime", "centimes"),
          "USD", new Unit("dollar", "dollars", "cent", "cents"));
  private static final Map<String, Unit> ENGLISH =
      Map.of(
          "XOF", new Unit("CFA franc", "CFA francs", null, null),
          "XAF", new Unit("CFA franc", "CFA francs", null, null),
          "EUR", new Unit("euro", "euros", "cent", "cents"),
          "USD", new Unit("dollar", "dollars", "cent", "cents"));

  @Override
  public List<String> getArgumentNames() {
    return List.of("currency", "locale", "spelling");
  }

  @Override
  public Object apply(
      Object input,
      Map<String, Object> args,
      PebbleTemplate self,
      EvaluationContext context,
      int line) {
    if (input == null) {
      return "";
    }
    BigDecimal amount = Numbers.toBigDecimal(input, NAME, line, self.getName());
    Locale locale = Locales.resolve(args.get("locale"), context.getLocale());
    boolean english = "en".equals(locale.getLanguage());
    boolean reformed = "1990".equals(String.valueOf(args.get("spelling")));
    Object currencyArg = args.get("currency");
    String currency =
        currencyArg == null ? null : currencyArg.toString().strip().toUpperCase(Locale.ROOT);
    if ("FCFA".equals(currency)) {
      currency = "XOF";
    }
    Unit unit = null;
    if (currency != null) {
      unit = (english ? ENGLISH : FRENCH).get(currency);
      if (unit == null) {
        throw new PebbleException(
            null,
            "the 'words' filter does not know currency '"
                + currency
                + "'; supported: XOF, XAF, fcfa, EUR, USD",
            line,
            self.getName());
      }
    }
    try {
      return spell(amount, unit, english, reformed);
    } catch (IllegalArgumentException e) {
      throw new PebbleException(
          null,
          "the 'words' filter supports amounts up to " + NumberSpeller.MAX,
          line,
          self.getName());
    }
  }

  static String spell(BigDecimal amount, Unit unit, boolean english, boolean reformed) {
    String minus = amount.signum() < 0 ? (english ? "minus " : "moins ") : "";
    BigDecimal value = amount.abs();
    if (unit != null) {
      value = value.setScale(unit.hasSubunit() ? 2 : 0, RoundingMode.HALF_UP);
    }
    long whole = value.toBigInteger().longValueExact();
    String words = number(whole, english, reformed);
    if (unit == null) {
      BigDecimal fraction = value.subtract(BigDecimal.valueOf(whole)).stripTrailingZeros();
      if (fraction.signum() == 0) {
        return minus + words;
      }
      String digits = fraction.toPlainString().substring(2);
      return minus
          + words
          + (english ? " point " : " virgule ")
          + decimals(digits, english, reformed);
    }
    int cents = value.subtract(BigDecimal.valueOf(whole)).movePointRight(2).intValue();
    StringBuilder text = new StringBuilder(minus);
    if (whole > 0 || cents == 0) {
      text.append(words).append(' ');
      if (!english && whole >= 1_000_000 && whole % 1_000_000 == 0) {
        text.append(startsWithVowel(unit.plural) ? "d'" : "de ");
      }
      text.append(whole > 1 ? unit.plural : unit.singular);
    }
    if (cents > 0) {
      if (whole > 0) {
        text.append(english ? " and " : " et ");
      }
      text.append(number(cents, english, reformed))
          .append(' ')
          .append(cents > 1 ? unit.subPlural : unit.subSingular);
    }
    return text.toString();
  }

  private static String number(long value, boolean english, boolean reformed) {
    return english ? NumberSpeller.english(value) : NumberSpeller.french(value, reformed);
  }

  private static String decimals(String digits, boolean english, boolean reformed) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < digits.length() - 1 && digits.charAt(i) == '0') {
      out.append(english ? "zero " : "zéro ");
      i++;
    }
    return out + number(Long.parseLong(digits.substring(i)), english, reformed);
  }

  private static boolean startsWithVowel(String word) {
    return "aeiouéè".indexOf(Character.toLowerCase(word.charAt(0))) >= 0;
  }
}
