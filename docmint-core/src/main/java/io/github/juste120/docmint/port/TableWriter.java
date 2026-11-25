package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import java.io.OutputStream;
import java.util.Iterator;
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

  /**
   * Writes rows one by one to a stream (streaming export). Optional: the default implementation
   * refuses it.
   *
   * @param header the header cells
   * @param rows the data rows, read lazily
   * @param out where to write; not closed
   * @param options the document options
   * @return the number of data rows written
   * @throws UnsupportedOperationException if this format cannot be streamed
   */
  default long writeRows(
      List<TableData.Cell> header,
      Iterator<List<TableData.Cell>> rows,
      OutputStream out,
      DocumentOptions options) {
    throw new UnsupportedOperationException(format() + " cannot be streamed");
  }
}
