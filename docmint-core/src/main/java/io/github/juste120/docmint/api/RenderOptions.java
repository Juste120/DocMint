package io.github.juste120.docmint.api;

import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.PreviewSettings;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.Watermark;
import java.util.Locale;

/**
 * Per-render options of the advanced API.
 *
 * @param locale the locale for dates and amounts, or {@code null} for the configured default
 * @param fileName the suggested file name of the result, or {@code null} for {@code
 *     <template>.<extension>}
 * @param watermark the watermark, or {@code null} for the configured default (none by default)
 * @param protection the password protection (PDF only), or {@code null}
 * @param preview the page and resolution of an image preview, or {@code null} for page 1 at 96 DPI
 */
public record RenderOptions(
    Locale locale,
    String fileName,
    Watermark watermark,
    Protection protection,
    PreviewSettings preview) {

  /**
   * Creates options with a locale and a file name only (V1 constructor).
   *
   * @param locale the locale, or {@code null}
   * @param fileName the file name, or {@code null}
   */
  public RenderOptions(Locale locale, String fileName) {
    this(locale, fileName, null, null, null);
  }

  /**
   * Returns options that keep every default.
   *
   * @return the default options
   */
  public static RenderOptions defaults() {
    return new RenderOptions(null, null, null, null, null);
  }

  /**
   * Returns a copy with another locale.
   *
   * @param newLocale the locale
   * @return the new options
   */
  public RenderOptions withLocale(Locale newLocale) {
    return new RenderOptions(newLocale, fileName, watermark, protection, preview);
  }

  /**
   * Returns a copy with another file name.
   *
   * @param newFileName the file name
   * @return the new options
   */
  public RenderOptions withFileName(String newFileName) {
    return new RenderOptions(locale, newFileName, watermark, protection, preview);
  }

  /**
   * Returns a copy with a watermark on every page.
   *
   * @param newWatermark the watermark, or {@code null} for the configured default
   * @return the new options
   */
  public RenderOptions withWatermark(Watermark newWatermark) {
    return new RenderOptions(locale, fileName, newWatermark, protection, preview);
  }

  /**
   * Returns a copy protected by password and/or permissions (PDF only).
   *
   * @param newProtection the protection, or {@code null}
   * @return the new options
   */
  public RenderOptions withProtection(Protection newProtection) {
    return new RenderOptions(locale, fileName, watermark, newProtection, preview);
  }

  /**
   * Returns a copy with preview settings (image formats only).
   *
   * @param newPreview the page and resolution
   * @return the new options
   */
  public RenderOptions withPreview(PreviewSettings newPreview) {
    return new RenderOptions(locale, fileName, watermark, protection, newPreview);
  }

  /**
   * Returns the options handled by post-processors.
   *
   * @param defaultWatermark the watermark used when none is set, may be {@code null}
   * @return the document options
   */
  public DocumentOptions documentOptions(Watermark defaultWatermark) {
    return new DocumentOptions(
        watermark != null ? watermark : defaultWatermark, protection, preview);
  }
}
