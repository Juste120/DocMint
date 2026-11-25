package io.github.juste120.docmint.infrastructure.pebble;

import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.PivotTable;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Template function {@code pivot(items, rows="city", columns="month", value="amount",
 * aggregate="sum", sort=false)}. Returns a map that templates can read: {@code rowKeys}, {@code
 * columnKeys}, {@code cells}, {@code rowTotals}, {@code columnTotals}, {@code total}.
 */
final class PivotFunction implements Function {

  static final String NAME = "pivot";

  @Override
  public List<String> getArgumentNames() {
    return List.of("items", "rows", "columns", "value", "aggregate", "sort");
  }

  @Override
  public Object execute(
      Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int line) {
    String template = self.getName();
    if (!(args.get("items") instanceof Collection<?> collection)) {
      throw error(template, line, "'items' must be a list");
    }
    String rows = required(args, "rows", template, line);
    String columns = required(args, "columns", template, line);
    Object valueArg = args.get("value");
    String aggregateName = String.valueOf(args.getOrDefault("aggregate", "sum"));
    PivotTable.Aggregate aggregate;
    try {
      aggregate = PivotTable.Aggregate.parse(aggregateName);
    } catch (IllegalArgumentException e) {
      throw error(
          template,
          line,
          "unknown aggregate '"
              + CodeFunctions.escape(aggregateName)
              + "'; use sum, count, avg, min or max");
    }
    if (valueArg == null && aggregate != PivotTable.Aggregate.COUNT) {
      throw error(template, line, "'value' is required, except with aggregate=\"count\"");
    }
    boolean sort = Boolean.parseBoolean(String.valueOf(args.getOrDefault("sort", "false")));
    List<Object> items = new ArrayList<>(collection);
    try {
      PivotTable table =
          PivotTable.compute(
              items,
              item -> read(item, rows, template, line),
              item -> read(item, columns, template, line),
              item ->
                  valueArg == null
                      ? null
                      : number(read(item, valueArg.toString(), template, line), template, line),
              aggregate,
              sort);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("rowKeys", table.rowKeys());
      result.put("columnKeys", table.columnKeys());
      result.put("cells", table.cells());
      result.put("rowTotals", table.rowTotals());
      result.put("columnTotals", table.columnTotals());
      result.put("total", table.total());
      return result;
    } catch (IllegalArgumentException e) {
      throw error(template, line, e.getMessage());
    }
  }

  private static String required(Map<String, Object> args, String name, String template, int line) {
    Object value = args.get(name);
    if (value == null || value.toString().isBlank()) {
      throw error(template, line, "'" + name + "' is required");
    }
    return value.toString();
  }

  private static Object read(Object item, String path, String template, int line) {
    Object current = item;
    for (String key : path.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        throw error(
            template, line, "cannot read '" + CodeFunctions.escape(path) + "' of a list item");
      }
      current = map.get(key);
    }
    return current;
  }

  private static BigDecimal number(Object value, String template, int line) {
    if (value == null) {
      return null;
    }
    try {
      return new BigDecimal(value.toString().strip());
    } catch (NumberFormatException e) {
      throw error(
          template, line, "value '" + CodeFunctions.escape(value.toString()) + "' is not a number");
    }
  }

  private static RenderException error(String template, int line, String detail) {
    return RenderException.evaluation(template, line, "pivot: " + detail, null);
  }
}
