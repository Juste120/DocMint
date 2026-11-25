package io.github.juste120.docmint.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A table to export: rows of typed cells, the first {@code headerRows} being headers.
 *
 * @param name the sheet name, or {@code null}
 * @param headerRows the number of header rows
 * @param rows the rows
 * @param columnFormats the Excel format of each column ({@code null} or empty for default)
 */
public record TableData(
    String name, int headerRows, List<List<Cell>> rows, List<String> columnFormats) {

  /** Type of a cell value. */
  public enum CellType {
    /** Text. */
    TEXT,
    /** A number: {@link Cell#value()} is a {@link java.math.BigDecimal}. */
    NUMBER,
    /** A date or date-time: {@link Cell#value()} is a {@code java.time} value. */
    DATE,
    /** A boolean. */
    BOOLEAN
  }

  /**
   * A cell.
   *
   * @param text the displayed text
   * @param value the typed value ({@code BigDecimal}, {@code LocalDate}, {@code LocalDateTime},
   *     {@code Boolean}) or the text
   * @param type the type
   * @param colspan the number of columns covered (≥ 1)
   */
  public record Cell(String text, Object value, CellType type, int colspan) {

    /**
     * Creates a cell.
     *
     * @throws IllegalArgumentException if colspan is less than 1
     */
    public Cell {
      text = text == null ? "" : text;
      type = type == null ? CellType.TEXT : type;
      value = value == null ? text : value;
      if (colspan < 1) {
        throw new IllegalArgumentException("colspan must be >= 1");
      }
    }

    /**
     * Creates a text cell.
     *
     * @param text the text
     * @return the cell
     */
    public static Cell text(String text) {
      return new Cell(text, text, CellType.TEXT, 1);
    }
  }

  /**
   * Creates a table with immutable rows.
   *
   * @throws NullPointerException if rows is null
   */
  public TableData {
    Objects.requireNonNull(rows, "rows");
    List<List<Cell>> copy = new ArrayList<>(rows.size());
    for (List<Cell> row : rows) {
      copy.add(List.copyOf(row));
    }
    rows = Collections.unmodifiableList(copy);
    List<String> formats = new ArrayList<>();
    if (columnFormats != null) {
      for (String format : columnFormats) {
        formats.add(format == null ? "" : format);
      }
    }
    columnFormats = Collections.unmodifiableList(formats);
    if (headerRows < 0 || headerRows > rows.size()) {
      throw new IllegalArgumentException("invalid number of header rows");
    }
  }

  /**
   * Returns the number of data rows (rows that are not headers).
   *
   * @return the count
   */
  public int dataRowCount() {
    return rows.size() - headerRows;
  }
}
