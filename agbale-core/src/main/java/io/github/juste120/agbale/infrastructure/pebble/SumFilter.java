package io.github.juste120.agbale.infrastructure.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * {@code {{ lines | sum("amount") }}} sums a list of numbers, or a numeric property of a list of
 * objects ({@code "price.total"} for nested properties). The sum is exact (decimal arithmetic);
 * {@code null} items and values are ignored; an empty or {@code null} list sums to 0.
 */
final class SumFilter implements Filter {

  static final String NAME = "sum";

  @Override
  public List<String> getArgumentNames() {
    return List.of("property");
  }

  @Override
  public Object apply(
      Object input,
      Map<String, Object> args,
      PebbleTemplate self,
      EvaluationContext context,
      int lineNumber) {
    if (input == null) {
      return BigDecimal.ZERO;
    }
    if (!(input instanceof Collection<?> items)) {
      throw new PebbleException(
          null, "the 'sum' filter expects a list", lineNumber, self.getName());
    }
    Object property = args.get("property");
    BigDecimal total = BigDecimal.ZERO;
    for (Object item : items) {
      Object value = property == null ? item : read(item, property.toString(), lineNumber, self);
      if (value != null) {
        total = total.add(Numbers.toBigDecimal(value, NAME, lineNumber, self.getName()));
      }
    }
    return total;
  }

  private static Object read(Object item, String path, int line, PebbleTemplate self) {
    Object current = item;
    for (String key : path.split("\\.")) {
      if (current == null) {
        return null;
      }
      if (!(current instanceof Map<?, ?> map)) {
        throw new PebbleException(
            null,
            "the 'sum' filter cannot read property '" + path + "' of a " + current.getClass().getSimpleName(),
            line,
            self.getName());
      }
      if (!map.containsKey(key)) {
        throw new PebbleException(
            null, "the 'sum' filter found no property '" + path + "' in a list item", line, self.getName());
      }
      current = map.get(key);
    }
    return current;
  }
}
