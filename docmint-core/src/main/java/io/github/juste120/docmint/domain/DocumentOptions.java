package io.github.juste120.docmint.domain;

/**
 * Options applied when rendering and post-processing a document.
 *
 * @param watermark the watermark, or {@code null}
 * @param protection the password protection, or {@code null}
 * @param preview the preview settings for image formats, or {@code null} for the defaults
 * @param csv the CSV settings, or {@code null} for the defaults
 * @param pdfStandard the PDF/A level, or {@code null} for a regular PDF
 * @param accessible whether to produce a tagged, accessible PDF (PDF/UA)
 * @param signature the electronic signature, or {@code null}
 */
public record DocumentOptions(
    Watermark watermark,
    Protection protection,
    PreviewSettings preview,
    CsvSettings csv,
    PdfStandard pdfStandard,
    boolean accessible,
    SignatureSettings signature) {

  /**
   * Creates options without PDF/A, accessibility or signature (V3 constructor).
   *
   * @param watermark the watermark, or {@code null}
   * @param protection the protection, or {@code null}
   * @param preview the preview settings, or {@code null}
   * @param csv the CSV settings, or {@code null}
   */
  public DocumentOptions(
      Watermark watermark, Protection protection, PreviewSettings preview, CsvSettings csv) {
    this(watermark, protection, preview, csv, null, false, null);
  }

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
