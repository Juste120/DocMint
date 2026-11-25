package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.OutputFormat;
import java.util.Collection;
import java.util.List;

/** Use cases of DocMint: render a document, list templates, clear the cache. */
public interface ReportService {

  /**
   * Renders a document.
   *
   * @param request the request
   * @return the rendered document
   */
  RenderResponse render(RenderRequest request);

  /**
   * Exports a collection of objects as a table (CSV, Excel...), columns from {@code ExcelColumn}
   * annotations.
   *
   * @param rows the rows
   * @param format a tabular format
   * @param options the options (file name, CSV dialect)
   * @return the file
   */
  RenderResponse export(Collection<?> rows, OutputFormat format, RenderOptions options);

  /**
   * Lists the available templates.
   *
   * @return the template names, sorted
   */
  List<String> availableTemplates();

  /** Clears the template cache. */
  void clearCache();
}
