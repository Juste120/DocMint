package io.github.juste120.docmint.infrastructure.export;

import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.port.TableData;
import io.github.juste120.docmint.port.TableData.Cell;
import io.github.juste120.docmint.port.TableWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    CsvSettings settings = options.csvOrDefault();
    StringBuilder csv = new StringBuilder();
    if (settings.byteOrderMark()) {
      csv.append('﻿');
    }
    for (List<Cell> row : tables.get(0).rows()) {
      boolean first = true;
      for (Cell cell : row) {
        for (int span = 0; span < cell.colspan(); span++) {
          if (!first) {
            csv.append(settings.separator());
          }
          first = false;
          if (span == 0) {
            csv.append(field(cell, settings.separator()));
          }
        }
      }
      csv.append("\r\n");
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
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
