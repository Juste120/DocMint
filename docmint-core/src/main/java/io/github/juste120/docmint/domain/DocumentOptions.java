package io.github.juste120.docmint.domain;

/**
 * Options applied to a rendered document by post-processors.
 *
 * @param watermark the watermark, or {@code null}
 * @param protection the password protection, or {@code null}
 * @param preview the preview settings for image formats, or {@code null} for the defaults
 * @param csv the CSV settings, or {@code null} for the defaults
 */
public record DocumentOptions(
    Watermark watermark, Protection protection, PreviewSettings preview, CsvSettings csv) {

  /**
   * Creates options without CSV settings (V2 constructor).
   *
   * @param watermark the watermark, or {@code null}
   * @param protection the protection, or {@code null}
   * @param preview the preview settings, or {@code null}
   */
  public DocumentOptions(Watermark watermark, Protection protection, PreviewSettings preview) {
    this(watermark, protection, preview, null);
  }

  /**
   * Returns options without watermark, protection or preview settings.
   *
   * @return empty options
   */
  public static DocumentOptions none() {
    return new DocumentOptions(null, null, null, null);
  }

  /**
   * Returns the preview settings, or the defaults.
   *
   * @return never null
   */
  public PreviewSettings previewOrDefault() {
    return preview == null ? PreviewSettings.defaults() : preview;
  }

  /**
   * Returns the CSV settings, or the defaults.
   *
   * @return never null
   */
  public CsvSettings csvOrDefault() {
    return csv == null ? CsvSettings.defaults() : csv;
  }
}
