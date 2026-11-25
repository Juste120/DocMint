package io.github.juste120.docmint.infrastructure.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Number conversions shared by the filters. */
final class Numbers {

  private Numbers() {}

  /** Converts a number or a numeric string to an exact {@link BigDecimal}. */
  static BigDecimal toBigDecimal(Object value, String filter, int line, String template) {
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof BigInteger integer) {
      return new BigDecimal(integer);
    }
    if (value instanceof Double || value instanceof Float) {
      double d = ((Number) value).doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw error(value, filter, line, template);
      }
      return new BigDecimal(value.toString());
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.longValue());
    }
    if (value instanceof String text) {
      try {
        return new BigDecimal(text.strip().replace(',', '.'));
      } catch (NumberFormatException e) {
        throw error(value, filter, line, template);
      }
    }
    throw error(value, filter, line, template);
  }

  private static PebbleException error(Object value, String filter, int line, String template) {
    return new PebbleException(
        null,
        "the '"
            + filter
            + "' filter expects a number but received a value of type "
            + value.getClass().getSimpleName(),
        line,
        template);
  }
}
