package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import java.util.List;

/**
 * Writes tables in a tabular format (CSV, Excel...). Implementations are found with {@link
 * java.util.ServiceLoader} ({@code META-INF/services/io.github.juste120.docmint.port.TableWriter})
 * and must be thread-safe.
 */
public interface TableWriter {

  /**
   * Returns the format this writer produces.
   *
   * @return the format
   */
  OutputFormat format();

  /**
   * Writes the tables.
   *
   * @param tables the tables (at least one)
   * @param options the document options (e.g. CSV dialect)
   * @return the file content
   */
  byte[] write(List<TableData> tables, DocumentOptions options);
}
