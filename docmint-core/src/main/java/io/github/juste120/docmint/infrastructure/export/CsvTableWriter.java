package io.github.juste120.docmint.infrastructure.export;

import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.port.TableData;
import io.github.juste120.docmint.port.TableData.Cell;
import io.github.juste120.docmint.port.TableWriter;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * Writes the first table as CSV (RFC 4180 quoting, configurable separator and BOM).
 *
 * <p>Spreadsheet formula injection is neutralised: a text cell starting with {@code =}, {@code +},
 * {@code -}, {@code @}, tab or carriage return is prefixed with {@code '} (OWASP recommendation),
 * so that opening the file never runs a formula. Numbers are written without grouping, dates in
 * ISO-8601.
 */
public final class CsvTableWriter implements TableWriter {

  @Override
  public OutputFormat format() {
    return OutputFormat.CSV;
  }

  @Override
  public byte[] write(List<TableData> tables, DocumentOptions options) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    TableData table = tables.get(0);
    write(table.rows().iterator(), out, options.csvOrDefault());
    return out.toByteArray();
  }

  @Override
  public long writeRows(
      List<Cell> header, Iterator<List<Cell>> rows, OutputStream out, DocumentOptions options) {
    Iterator<List<Cell>> all =
        new Iterator<>() {
          private boolean headerDone = header.isEmpty();

          @Override
          public boolean hasNext() {
            return !headerDone || rows.hasNext();
          }

          @Override
          public List<Cell> next() {
            if (!headerDone) {
              headerDone = true;
              return header;
            }
            return rows.next();
          }
        };
    return write(all, out, options.csvOrDefault()) - (header.isEmpty() ? 0 : 1);
  }

  private static long write(Iterator<List<Cell>> rows, OutputStream out, CsvSettings settings) {
    Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    long count = 0;
    try {
      if (settings.byteOrderMark()) {
        writer.write('\uFEFF');
      }
      while (rows.hasNext()) {
        writer.write(line(rows.next(), settings.separator()));
        count++;
      }
      writer.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return count;
  }

  private static String line(List<Cell> row, char separator) {
    StringBuilder csv = new StringBuilder();
    boolean first = true;
    for (Cell cell : row) {
      for (int span = 0; span < cell.colspan(); span++) {
        if (!first) {
          csv.append(separator);
        }
        first = false;
        if (span == 0) {
          csv.append(field(cell, separator));
        }
      }
    }
    return csv.append("\r\n").toString();
  }

  private static String field(Cell cell, char separator) {
    String value;
    switch (cell.type()) {
      case NUMBER -> value = ((BigDecimal) cell.value()).toPlainString();
      case DATE, BOOLEAN -> value = String.valueOf(cell.value());
      default -> value = neutralise(cell.text());
    }
    boolean quote =
        value.indexOf(separator) >= 0
            || value.indexOf('"') >= 0
            || value.indexOf('\n') >= 0
            || value.indexOf('\r') >= 0;
    return quote ? '"' + value.replace("\"", "\"\"") + '"' : value;
  }

  /** Prefixes values that a spreadsheet would execute as formulas. */
  static String neutralise(String text) {
    if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
      return "'" + text;
    }
    return text;
  }
}
