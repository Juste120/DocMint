package io.github.juste120.agbale.infrastructure.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * {@code {{ amount | fcfa }}} formats an amount in CFA francs: {@code 1 250 000 FCFA}.
 *
 * <p>The CFA franc has no decimals: amounts are rounded half-up to the unit. Thousands are
 * separated by no-break spaces (U+00A0) so that an amount never wraps. The format does not depend
 * on the locale, and is identical for XOF (West Africa) and XAF (Central Africa).
 */
final class FcfaFilter implements Filter {

  static final String NAME = "fcfa";
  private static final char SPACE = ' ';

  @Override
  public List<String> getArgumentNames() {
    return List.of();
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
    BigDecimal amount =
        Numbers.toBigDecimal(input, NAME, lineNumber, self.getName())
            .setScale(0, RoundingMode.HALF_UP);
    return format(amount);
  }

  static String format(BigDecimal roundedAmount) {
    String digits = roundedAmount.abs().toPlainString();
    StringBuilder grouped = new StringBuilder();
    int firstGroup = digits.length() % 3 == 0 ? 3 : digits.length() % 3;
    grouped.append(digits, 0, firstGroup);
    for (int i = firstGroup; i < digits.length(); i += 3) {
      grouped.append(SPACE).append(digits, i, i + 3);
    }
    return (roundedAmount.signum() < 0 ? "-" : "") + grouped + SPACE + "FCFA";
  }
}
