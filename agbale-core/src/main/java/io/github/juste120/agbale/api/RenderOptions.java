package io.github.juste120.agbale.api;

import java.util.Locale;

/**
 * Per-render options of the advanced API.
 *
 * @param locale the locale for dates and amounts, or {@code null} for the configured default
 * @param fileName the suggested file name of the result, or {@code null} for {@code <template>.pdf}
 */
public record RenderOptions(Locale locale, String fileName) {

  /**
   * Returns options that keep every default.
   *
   * @return the default options
   */
  public static RenderOptions defaults() {
    return new RenderOptions(null, null);
  }

  /**
   * Returns a copy with another locale.
   *
   * @param newLocale the locale
   * @return the new options
   */
  public RenderOptions withLocale(Locale newLocale) {
    return new RenderOptions(newLocale, fileName);
  }

  /**
   * Returns a copy with another file name.
   *
   * @param newFileName the file name
   * @return the new options
   */
  public RenderOptions withFileName(String newFileName) {
    return new RenderOptions(locale, newFileName);
  }
}
