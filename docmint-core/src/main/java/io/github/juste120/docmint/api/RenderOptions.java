package io.github.juste120.docmint.api;

import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.PdfStandard;
import io.github.juste120.docmint.domain.PreviewSettings;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.SignatureSettings;
import io.github.juste120.docmint.domain.Watermark;
import java.util.Locale;

/**
 * Per-render options of the advanced API. Every option is optional; {@code null} means the
 * configured default.
 *
 * @param locale the locale for dates and amounts
 * @param fileName the suggested file name of the result ({@code <template>.<extension>} by default)
 * @param watermark the watermark
 * @param protection the password protection (PDF only)
 * @param preview the page and resolution of an image preview (page 1 at 96 DPI by default)
 * @param csv the CSV dialect ({@code ;} and UTF-8 BOM by default)
 * @param pdfStandard the PDF/A level (regular PDF by default)
 * @param accessible whether to produce a tagged, accessible PDF (PDF/UA)
 * @param signature the electronic signature (module {@code docmint-signature})
 */
public record RenderOptions(
    Locale locale,
    String fileName,
    Watermark watermark,
    Protection protection,
    PreviewSettings preview,
    CsvSettings csv,
    PdfStandard pdfStandard,
    Boolean accessible,
    SignatureSettings signature) {

  /**
   * Creates options without PDF/A, accessibility or signature (V3 constructor).
   *
   * @param locale the locale, or {@code null}
   * @param fileName the file name, or {@code null}
   * @param watermark the watermark, or {@code null}
   * @param protection the protection, or {@code null}
   * @param preview the preview settings, or {@code null}
   * @param csv the CSV settings, or {@code null}
   */
  public RenderOptions(
      Locale locale,
      String fileName,
      Watermark watermark,
      Protection protection,
      PreviewSettings preview,
      CsvSettings csv) {
    this(locale, fileName, watermark, protection, preview, csv, null, null, null);
  }

  /**
   * Creates options without CSV settings (V2 constructor).
   *
   * @param locale the locale, or {@code null}
   * @param fileName the file name, or {@code null}
   * @param watermark the watermark, or {@code null}
   * @param protection the protection, or {@code null}
   * @param preview the preview settings, or {@code null}
   */
  public RenderOptions(
      Locale locale,
      String fileName,
      Watermark watermark,
      Protection protection,
      PreviewSettings preview) {
    this(locale, fileName, watermark, protection, preview, null);
  }

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
    return new RenderOptions(null, null);
  }

  /**
   * Returns a copy with another locale.
   *
   * @param value the locale
   * @return the new options
   */
  public RenderOptions withLocale(Locale value) {
    return new RenderOptions(
        value, fileName, watermark, protection, preview, csv, pdfStandard, accessible, signature);
  }

  /**
   * Returns a copy with another file name.
   *
   * @param value the file name
   * @return the new options
   */
  public RenderOptions withFileName(String value) {
    return new RenderOptions(
        locale, value, watermark, protection, preview, csv, pdfStandard, accessible, signature);
  }

  /**
   * Returns a copy with a watermark on every page.
   *
   * @param value the watermark, or {@code null} for the configured default
   * @return the new options
   */
  public RenderOptions withWatermark(Watermark value) {
    return new RenderOptions(
        locale, fileName, value, protection, preview, csv, pdfStandard, accessible, signature);
  }

  /**
   * Returns a copy protected by password and/or permissions (PDF only, not with PDF/A or a
   * signature).
   *
   * @param value the protection, or {@code null}
   * @return the new options
   */
  public RenderOptions withProtection(Protection value) {
    return new RenderOptions(
        locale, fileName, watermark, value, preview, csv, pdfStandard, accessible, signature);
  }

  /**
   * Returns a copy with preview settings (image formats only).
   *
   * @param value the page and resolution
   * @return the new options
   */
  public RenderOptions withPreview(PreviewSettings value) {
    return new RenderOptions(
        locale, fileName, watermark, protection, value, csv, pdfStandard, accessible, signature);
  }

  /**
   * Returns a copy with another CSV dialect.
   *
   * @param value the separator and BOM settings
   * @return the new options
   */
  public RenderOptions withCsv(CsvSettings value) {
    return new RenderOptions(
        locale,
        fileName,
        watermark,
        protection,
        preview,
        value,
        pdfStandard,
        accessible,
        signature);
  }

  /**
   * Returns a copy producing PDF/A.
   *
   * @param value the PDF/A level, or {@code null} for the configured default
   * @return the new options
   */
  public RenderOptions withPdfStandard(PdfStandard value) {
    return new RenderOptions(
        locale, fileName, watermark, protection, preview, csv, value, accessible, signature);
  }

  /**
   * Returns a copy producing (or not) a tagged, accessible PDF.
   *
   * @param value {@code true} for PDF/UA, {@code null} for the configured default
   * @return the new options
   */
  public RenderOptions withAccessible(Boolean value) {
    return new RenderOptions(
        locale, fileName, watermark, protection, preview, csv, pdfStandard, value, signature);
  }

  /**
   * Returns a copy signed electronically (module {@code docmint-signature}).
   *
   * @param value the signature settings, or {@code null} for the configured default
   * @return the new options
   */
  public RenderOptions withSignature(SignatureSettings value) {
    return new RenderOptions(
        locale, fileName, watermark, protection, preview, csv, pdfStandard, accessible, value);
  }

  /**
   * Returns the options handled by renderers and post-processors, with a default watermark.
   *
   * @param defaultWatermark the watermark used when none is set, may be {@code null}
   * @return the document options
   */
  public DocumentOptions documentOptions(Watermark defaultWatermark) {
    return documentOptions(defaultWatermark, null, false, null);
  }

  /**
   * Returns the options handled by renderers and post-processors, with defaults.
   *
   * @param defaultWatermark the watermark used when none is set, may be {@code null}
   * @param defaultStandard the PDF/A level used when none is set, may be {@code null}
   * @param defaultAccessible whether PDFs are accessible when not set
   * @param defaultSignature the signature used when none is set, may be {@code null}
   * @return the document options
   */
  public DocumentOptions documentOptions(
      Watermark defaultWatermark,
      PdfStandard defaultStandard,
      boolean defaultAccessible,
      SignatureSettings defaultSignature) {
    return new DocumentOptions(
        watermark != null ? watermark : defaultWatermark,
        protection,
        preview,
        csv,
        pdfStandard != null ? pdfStandard : defaultStandard,
        accessible != null ? accessible : defaultAccessible,
        signature != null ? signature : defaultSignature);
  }
}
