package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.domain.PreviewSettings;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.Watermark;
import java.util.Objects;

/**
 * What a {@link Report @Report} method may return instead of the bare data object, to decide per
 * request the watermark, the protection, the preview page or the file name.
 *
 * <pre>{@code
 * @Report(value = "invoice", filename = "invoice-{number}.pdf")
 * ReportContent invoice(@PathVariable long id) {
 *   InvoiceResponse invoice = service.find(id);
 *   return ReportContent.of(invoice)
 *       .withWatermark(invoice.paid() ? Watermark.text("PAYÉE") : null);
 * }
 * }</pre>
 *
 * @param data the data object exposed to the template
 * @param options the render options; the file name, if set, replaces the annotation's one
 */
public record ReportContent(Object data, RenderOptions options) {

  /**
   * Creates the content.
   *
   * @throws NullPointerException if data is null
   */
  public ReportContent {
    Objects.requireNonNull(data, "data");
    options = options == null ? RenderOptions.defaults() : options;
  }

  /**
   * Wraps a data object with default options.
   *
   * @param data the data object
   * @return the content
   */
  public static ReportContent of(Object data) {
    return new ReportContent(data, RenderOptions.defaults());
  }

  /**
   * Returns a copy with a watermark.
   *
   * @param watermark the watermark, or {@code null} for none (or the configured default)
   * @return the new content
   */
  public ReportContent withWatermark(Watermark watermark) {
    return new ReportContent(data, options.withWatermark(watermark));
  }

  /**
   * Returns a copy protected by password and/or permissions.
   *
   * @param protection the protection, or {@code null}
   * @return the new content
   */
  public ReportContent withProtection(Protection protection) {
    return new ReportContent(data, options.withProtection(protection));
  }

  /**
   * Returns a copy with preview settings (for {@code format = PNG}).
   *
   * @param preview the page and resolution
   * @return the new content
   */
  public ReportContent withPreview(PreviewSettings preview) {
    return new ReportContent(data, options.withPreview(preview));
  }

  /**
   * Returns a copy with an explicit file name (sanitised like the annotation's).
   *
   * @param fileName the file name
   * @return the new content
   */
  public ReportContent withFileName(String fileName) {
    return new ReportContent(data, options.withFileName(fileName));
  }
}
