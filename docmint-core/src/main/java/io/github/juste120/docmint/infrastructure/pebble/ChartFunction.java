package io.github.juste120.docmint.infrastructure.pebble;

import io.github.juste120.docmint.api.RenderException;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template function {@code chart(...)}:
 *
 * <pre>
 * {{ chart(type="bar", labels=months, values=sales, title="Ventes 2026") }}
 * {{ chart(type="line", labels=months, series=[{"name": "Lomé", "values": lome}, …]) }}
 * {{ chart(type="pie", labels=["Espèces", "Mobile Money"], values=[40, 60], width="120mm") }}
 * </pre>
 *
 * Types: {@code bar}, {@code line}, {@code pie}, {@code doughnut}. Sizes in {@code mm}, {@code cm},
 * {@code in}, {@code pt} or {@code px} (default 160mm × 80mm); optional {@code colors} list of
 * {@code #RRGGBB}.
 */
final class ChartFunction implements Function {

  static final String NAME = "chart";
  static final int MAX_SERIES = 10;
  static final int MAX_POINTS = 1000;
  private static final double SCALE = 3;
  private static final Pattern SIZE = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,2})?)(mm|cm|in|pt|px)");
  private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

  @Override
  public List<String> getArgumentNames() {
    return List.of("type", "labels", "values", "series", "title", "width", "height", "colors");
  }

  @Override
  public Object execute(
      Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int line) {
    String template = self.getName();
    ChartPainter.Type type = type(args.getOrDefault("type", "bar"), template, line);
    List<String> labels = strings(args.get("labels"), "labels", template, line);
    List<ChartPainter.Series> series = series(args, template, line);
    if (labels.isEmpty() || labels.size() > MAX_POINTS) {
      throw error(template, line, "'labels' must contain 1 to " + MAX_POINTS + " items");
    }
    for (ChartPainter.Series s : series) {
      if (s.values().size() != labels.size()) {
        throw error(
            template,
            line,
            "series '"
                + s.name()
                + "' has "
                + s.values().size()
                + " values for "
                + labels.size()
                + " labels");
      }
    }
    boolean round = type == ChartPainter.Type.PIE || type == ChartPainter.Type.DOUGHNUT;
    if (round && series.size() != 1) {
      throw error(
          template, line, "a " + type.name().toLowerCase(Locale.ROOT) + " chart has one series");
    }
    if (round && series.get(0).values().stream().anyMatch(v -> v < 0)) {
      throw error(template, line, "a pie or doughnut chart cannot show negative values");
    }
    String width = size(args.getOrDefault("width", "160mm"), template, line);
    String height = size(args.getOrDefault("height", "80mm"), template, line);
    ChartPainter.Spec spec =
        new ChartPainter.Spec(
            type,
            args.get("title") == null ? null : args.get("title").toString(),
            labels,
            series,
            colors(args.get("colors"), template, line),
            pixels(width),
            pixels(height),
            context.getLocale() == null ? Locale.FRENCH : context.getLocale());
    String png = Base64.getEncoder().encodeToString(ChartPainter.png(spec));
    return new SafeString(
        "<img class=\"docmint-chart\" alt=\""
            + CodeFunctions.escape(spec.title() == null ? "Chart" : spec.title())
            + "\" src=\"data:image/png;base64,"
            + png
            + "\" style=\"width: "
            + width
            + "; height: "
            + height
            + ";\"/>");
  }

  private static ChartPainter.Type type(Object value, String template, int line) {
    try {
      return ChartPainter.Type.valueOf(String.valueOf(value).strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw error(
          template,
          line,
          "unknown chart type '"
              + CodeFunctions.escape(String.valueOf(value))
              + "'; use bar, line, pie or doughnut");
    }
  }

  private static List<ChartPainter.Series> series(
      Map<String, Object> args, String template, int line) {
    Object values = args.get("values");
    Object series = args.get("series");
    if ((values == null) == (series == null)) {
      throw error(template, line, "give either 'values' (one series) or 'series'");
    }
    if (values != null) {
      return List.of(new ChartPainter.Series("", numbers(values, template, line)));
    }
    if (!(series instanceof Collection<?> list) || list.isEmpty() || list.size() > MAX_SERIES) {
      throw error(template, line, "'series' must be a list of 1 to " + MAX_SERIES + " series");
    }
    List<ChartPainter.Series> result = new ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> map) || !map.containsKey("values")) {
        throw error(template, line, "each series needs 'name' and 'values'");
      }
      Object name = map.get("name");
      result.add(
          new ChartPainter.Series(
              name == null ? "Série " + (result.size() + 1) : name.toString(),
              numbers(map.get("values"), template, line)));
    }
    return result;
  }

  private static List<Double> numbers(Object value, String template, int line) {
    if (!(value instanceof Collection<?> list)) {
      throw error(template, line, "chart values must be a list of numbers");
    }
    List<Double> numbers = new ArrayList<>();
    for (Object item : list) {
      if (item == null) {
        numbers.add(0.0);
      } else if (item instanceof Number n) {
        numbers.add(n.doubleValue());
      } else {
        try {
          numbers.add(new BigDecimal(item.toString().strip()).doubleValue());
        } catch (NumberFormatException e) {
          throw error(
              template,
              line,
              "chart value '" + CodeFunctions.escape(item.toString()) + "' is not a number");
        }
      }
    }
    return numbers;
  }

  private static List<String> strings(Object value, String name, String template, int line) {
    if (!(value instanceof Collection<?> list)) {
      throw error(template, line, "'" + name + "' must be a list");
    }
    return list.stream().map(item -> item == null ? "" : item.toString()).toList();
  }

  private static List<Color> colors(Object value, String template, int line) {
    if (value == null) {
      return List.of();
    }
    List<Color> colors = new ArrayList<>();
    for (String text : strings(value, "colors", template, line)) {
      if (!COLOR.matcher(text).matches()) {
        throw error(
            template, line, "invalid colour '" + CodeFunctions.escape(text) + "'; use #RRGGBB");
      }
      colors.add(Color.decode(text));
    }
    return colors;
  }

  private static String size(Object value, String template, int line) {
    Matcher matcher = SIZE.matcher(String.valueOf(value).strip());
    if (!matcher.matches() || pixels(matcher.group(0)) < 60 || pixels(matcher.group(0)) > 4000) {
      throw error(
          template,
          line,
          "invalid chart size '"
              + CodeFunctions.escape(String.valueOf(value))
              + "'; use e.g. 160mm (between about 7mm and 350mm)");
    }
    return matcher.group(0);
  }

  /** Image pixels for a CSS length (CSS px at 96 DPI, times the drawing scale). */
  static int pixels(String css) {
    Matcher matcher = SIZE.matcher(css);
    if (!matcher.matches()) {
      return 0;
    }
    double value = Double.parseDouble(matcher.group(1));
    double px =
        switch (matcher.group(2)) {
          case "mm" -> value * 96 / 25.4;
          case "cm" -> value * 96 / 2.54;
          case "in" -> value * 96;
          case "pt" -> value * 96 / 72;
          default -> value;
        };
    return (int) Math.round(px * SCALE);
  }

  private static RenderException error(String template, int line, String detail) {
    return RenderException.evaluation(template, line, "chart: " + detail, null);
  }
}
