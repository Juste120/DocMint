package io.github.juste120.agbale.infrastructure.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code {{ amount | money("EUR") }}} formats an amount in a currency, following the locale of the
 * render (French by default: {@code 1 234,50 €}). An optional second argument overrides the locale:
 * {@code money("USD", "en-US")}.
 */
final class MoneyFilter implements Filter {

  static final String NAME = "money";

  @Override
  public List<String> getArgumentNames() {
    return List.of("currency", "locale");
  }

  @Override
  public Object apply(
      Object input,
      Map<String, Object> args,
      PebbleTemplate self,
      EvaluationContext context,
      int lineNumber) {
    if (input == null) {
      return "";
    }
    Object code = args.get("currency");
    if (code == null) {
      throw new PebbleException(
          null,
          "the 'money' filter needs a currency code, for example money(\"XOF\") or"
              + " money(\"EUR\")",
          lineNumber,
          self.getName());
    }
    Currency currency;
    try {
      currency = Currency.getInstance(code.toString().strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new PebbleException(
          e,
          "unknown ISO 4217 currency code '" + code + "' in the 'money' filter",
          lineNumber,
          self.getName());
    }
    Locale locale = Locales.resolve(args.get("locale"), context.getLocale());
    NumberFormat format = NumberFormat.getCurrencyInstance(locale);
    format.setCurrency(currency);
    format.setMinimumFractionDigits(currency.getDefaultFractionDigits());
    format.setMaximumFractionDigits(currency.getDefaultFractionDigits());
    format.setRoundingMode(RoundingMode.HALF_UP);
    String text = format.format(Numbers.toBigDecimal(input, NAME, lineNumber, self.getName()));
    return text.replace(' ', ' ');
  }
}
