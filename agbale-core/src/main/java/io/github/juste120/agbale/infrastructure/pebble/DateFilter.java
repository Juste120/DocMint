package io.github.juste120.agbale.infrastructure.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code {{ issuedOn | date }}} formats a date in the long format of the render locale ({@code 24
 * novembre 2026} in French). {@code date("dd/MM/yyyy")} uses an explicit pattern, and an optional
 * second argument overrides the locale: {@code date("d MMMM yyyy", "en")}.
 *
 * <p>Accepts {@code LocalDate}, {@code LocalDateTime}, {@code OffsetDateTime}, {@code
 * ZonedDateTime}, {@code Instant} (formatted in UTC), {@code java.util.Date} and ISO-8601 strings.
 * Replaces Pebble's built-in {@code date} filter.
 */
final class DateFilter implements Filter {

  static final String NAME = "date";

  @Override
  public List<String> getArgumentNames() {
    return List.of("pattern", "locale");
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
    TemporalAccessor value = temporal(input, lineNumber, self);
    Locale locale = Locales.resolve(args.get("locale"), context.getLocale());
    Object pattern = args.get("pattern");
    try {
      DateTimeFormatter formatter;
      if (pattern != null && !pattern.toString().isBlank()) {
        formatter = DateTimeFormatter.ofPattern(pattern.toString(), locale);
      } else if (value instanceof LocalTime) {
        formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale);
      } else {
        formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);
      }
      return formatter.format(value).replace(' ', ' ');
    } catch (IllegalArgumentException | DateTimeException e) {
      throw new PebbleException(
          e,
          "the 'date' filter cannot format this value with pattern '"
              + pattern
              + "': "
              + e.getMessage(),
          lineNumber,
          self.getName());
    }
  }

  private static TemporalAccessor temporal(Object input, int line, PebbleTemplate self) {
    if (input instanceof Instant instant) {
      return instant.atOffset(ZoneOffset.UTC);
    }
    if (input instanceof java.util.Date date) {
      return Instant.ofEpochMilli(date.getTime()).atOffset(ZoneOffset.UTC);
    }
    if (input instanceof TemporalAccessor accessor) {
      return accessor;
    }
    if (input instanceof String text) {
      try {
        return text.length() <= 10 ? LocalDate.parse(text) : LocalDateTime.parse(text);
      } catch (DateTimeParseException e) {
        throw new PebbleException(
            e,
            "the 'date' filter cannot parse '" + text + "' as an ISO-8601 date",
            line,
            self.getName());
      }
    }
    throw new PebbleException(
        null,
        "the 'date' filter expects a date but received a " + input.getClass().getSimpleName(),
        line,
        self.getName());
  }
}
