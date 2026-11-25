package io.github.juste120.agbale.infrastructure.pdf;

import io.github.juste120.agbale.domain.PageSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Builds Agbale's default print stylesheet from {@link PageSettings}. */
final class DefaultStylesheet {

  private static final String RESOURCE = "/io/github/juste120/agbale/css/agbale-default.css";
  private static final String STATIC_CSS = load();

  private DefaultStylesheet() {}

  static String css(PageSettings settings) {
    String size = settings.size().name() + " " + settings.orientation().name().toLowerCase(Locale.ROOT);
    String page =
        String.format(
            Locale.ROOT,
            "@page { size: %s; margin: %smm %smm %smm %smm; }%n",
            size,
            mm(settings.marginTopMm()),
            mm(settings.marginRightMm()),
            mm(settings.marginBottomMm()),
            mm(settings.marginLeftMm()));
    return page + STATIC_CSS;
  }

  private static String mm(double value) {
    return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
  }

  private static String load() {
    try (InputStream in = DefaultStylesheet.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Agbale default stylesheet is missing: " + RESOURCE);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
