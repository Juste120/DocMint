package io.github.juste120.docmint.api;

import io.github.juste120.docmint.application.ReportService;
import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.OutputFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

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
  private final Executor executor;

  DocMint(ReportService service, Executor executor) {
    this.service = service;
    this.executor = executor;
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
   * Renders the first page of a template as a PNG image at 96 DPI (preview, thumbnail).
   *
   * @param templateName the template name
   * @param data the data exposed to the template, not null
   * @return the PNG bytes
   * @throws DocMintException if the template cannot be found or rendered
   */
  public byte[] png(String templateName, Object data) {
    return render(new RenderRequest(templateName, data, OutputFormat.PNG, RenderOptions.defaults()))
        .content();
  }

  /**
   * Exports a collection of objects as CSV ({@code ;} separator and UTF-8 BOM by default). Columns
   * come from {@link ExcelColumn} annotations, or every property when none is annotated.
   *
   * @param rows the rows (records, objects with getters or maps)
   * @return the CSV bytes
   * @throws DocMintException if the rows cannot be read or exceed the row limit
   */
  public byte[] csv(Collection<?> rows) {
    return export(rows, OutputFormat.CSV, RenderOptions.defaults()).content();
  }

  /**
   * Exports a collection of objects in a tabular format: {@link OutputFormat#CSV}, or {@link
   * OutputFormat#XLSX} with the {@code docmint-excel} module.
   *
   * @param rows the rows
   * @param format the format
   * @param options file name and CSV dialect
   * @return the file with its media type and file name
   * @throws DocMintException if the format is not available or the rows cannot be read
   */
  public RenderResponse export(Collection<?> rows, OutputFormat format, RenderOptions options) {
    return service.export(rows, format, options);
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
   * Renders a document directly into a stream (e.g. an HTTP response or a file).
   *
   * @param request the request
   * @param out where to write; not closed
   * @return the metadata of the document (media type, file name, page count)
   * @throws DocMintException if the template cannot be found or rendered
   * @throws UncheckedIOException if writing to the stream fails
   */
  public RenderResponse render(RenderRequest request, OutputStream out) {
    RenderResponse response = render(request);
    try {
      out.write(response.content());
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return response;
  }

  /**
   * Streams a list of objects as CSV, row by row: memory does not grow with the number of rows (for
   * large exports from a database cursor). The row limit does not apply.
   *
   * @param rows the rows, e.g. a Spring Data {@code Stream} (closed after use)
   * @param out where to write; not closed
   * @param settings the CSV dialect, or {@code null} for {@code ;} and BOM
   * @return the number of data rows written
   * @throws DocMintException if a row cannot be read
   */
  public long exportCsv(Stream<?> rows, OutputStream out, CsvSettings settings) {
    return service.exportCsv(rows, out, settings);
  }

  /**
   * Renders a document without blocking the calling thread, on the configured executor.
   *
   * @param request the request
   * @return a future completed with the document, or exceptionally with a {@link DocMintException}
   */
  public CompletableFuture<RenderResponse> renderAsync(RenderRequest request) {
    return CompletableFuture.supplyAsync(() -> render(request), executor);
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
