package io.github.juste120.docmint.api;

import io.github.juste120.docmint.application.ReportService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Entry point of DocMint: generates business documents from HTML/CSS templates.
 *
 * <pre>{@code
 * DocMint docMint = DocMint.createDefault();          // zero configuration
 * byte[] pdf = docMint.pdf("invoice", invoice);     // reports/invoice.html + your data
 * }</pre>
 *
 * <p>Templates are HTML files found on the class path below {@code reports/} (configurable). The
 * data can be a record, an object with getters or a {@code Map}; its properties become template
 * variables ({@code {{ customer.name }}}).
 *
 * <p>An instance is immutable and thread-safe: create it once and share it.
 */
public final class DocMint {

  private final ReportService service;

  DocMint(ReportService service) {
    this.service = service;
  }

  /**
   * Returns a builder to configure an instance.
   *
   * @return a new builder with every default
   */
  public static DocMintBuilder builder() {
    return new DocMintBuilder();
  }

  /**
   * Creates an instance with every default: templates in {@code reports/}, French locale, cache
   * enabled, no remote resources.
   *
   * @return a new instance
   */
  public static DocMint createDefault() {
    return builder().build();
  }

  /**
   * Renders a template as PDF.
   *
   * @param templateName the template name, relative to the templates folder, e.g. {@code invoice}
   *     for {@code reports/invoice.html}
   * @param data the data exposed to the template, not null (use an empty map if none)
   * @return the PDF bytes
   * @throws DocMintException if the template cannot be found or rendered
   */
  public byte[] pdf(String templateName, Object data) {
    return render(RenderRequest.of(templateName, data)).content();
  }

  /**
   * Renders a template as PDF with a specific locale for dates and amounts.
   *
   * @param templateName the template name
   * @param data the data exposed to the template, not null
   * @param locale the locale, e.g. {@code Locale.ENGLISH}
   * @return the PDF bytes
   * @throws DocMintException if the template cannot be found or rendered
   */
  public byte[] pdf(String templateName, Object data, Locale locale) {
    Objects.requireNonNull(locale, "locale");
    RenderRequest request =
        RenderRequest.of(templateName, data)
            .withOptions(RenderOptions.defaults().withLocale(locale));
    return render(request).content();
  }

  /**
   * Renders a document with full control over the request (advanced API).
   *
   * @param request the request
   * @return the document with its media type, suggested file name and page count
   * @throws DocMintException if the template cannot be found or rendered
   */
  public RenderResponse render(RenderRequest request) {
    return service.render(request);
  }

  /**
   * Lists the templates found in the templates folder.
   *
   * @return the template names without extension, sorted
   */
  public List<String> availableTemplates() {
    return service.availableTemplates();
  }

  /** Removes every compiled template from the cache, so that changed files are read again. */
  public void clearCache() {
    service.clearCache();
  }
}
