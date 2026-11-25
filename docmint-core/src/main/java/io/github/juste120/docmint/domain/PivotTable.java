package io.github.juste120.docmint.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A cross table (pivot): values aggregated by a row key and a column key, with totals.
 *
 * @param rowKeys the distinct row keys
 * @param columnKeys the distinct column keys
 * @param cells {@code cells[row][column]}, {@code null} when no value falls in the cell
 * @param rowTotals the aggregate of each row
 * @param columnTotals the aggregate of each column
 * @param total the aggregate of every value
 */
public record PivotTable(
    List<String> rowKeys,
    List<String> columnKeys,
    List<List<BigDecimal>> cells,
    List<BigDecimal> rowTotals,
    List<BigDecimal> columnTotals,
    BigDecimal total) {

  /** Maximum number of input rows. */
  public static final int MAX_ROWS = 10_000;

  /** Maximum number of distinct row keys and of distinct column keys. */
  public static final int MAX_KEYS = 500;

  /** How values of a cell are combined. */
  public enum Aggregate {
    /** Sum of the values. */
    SUM,
    /** Number of values. */
    COUNT,
    /** Arithmetic mean (2 decimals, half-up). */
    AVG,
    /** Smallest value. */
    MIN,
    /** Largest value. */
    MAX;

    /**
     * Parses an aggregate name, case-insensitively.
     *
     * @param name e.g. {@code sum}
     * @return the aggregate
     * @throws IllegalArgumentException if unknown
     */
    public static Aggregate parse(String name) {
      return valueOf(name.strip().toUpperCase(Locale.ROOT));
    }

    BigDecimal apply(List<BigDecimal> values) {
      if (values.isEmpty()) {
        return this == COUNT ? BigDecimal.ZERO : null;
      }
      return switch (this) {
        case SUM -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        case COUNT -> BigDecimal.valueOf(values.size());
        case AVG ->
            values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);
        case MIN -> Collections.min(values);
        case MAX -> Collections.max(values);
      };
    }
  }

  /**
   * Creates a pivot table with immutable lists.
   *
   * @throws NullPointerException if a list is null
   */
  public PivotTable {
    rowKeys = List.copyOf(rowKeys);
    columnKeys = List.copyOf(columnKeys);
    List<List<BigDecimal>> copy = new ArrayList<>();
    for (List<BigDecimal> row : cells) {
      copy.add(Collections.unmodifiableList(new ArrayList<>(row)));
    }
    cells = Collections.unmodifiableList(copy);
    rowTotals = Collections.unmodifiableList(new ArrayList<>(rowTotals));
    columnTotals = Collections.unmodifiableList(new ArrayList<>(columnTotals));
  }

  /**
   * Computes a pivot table.
   *
   * @param items the input rows
   * @param rowKey reads the row key of an item
   * @param columnKey reads the column key of an item
   * @param value reads the value of an item ({@code null} values are ignored, except by COUNT)
   * @param aggregate how to combine values
   * @param sorted whether to sort keys (otherwise first-appearance order)
   * @param <T> the item type
   * @return the pivot table
   * @throws IllegalArgumentException if the input exceeds the limits
   */
  public static <T> PivotTable compute(
      List<T> items,
      Function<T, Object> rowKey,
      Function<T, Object> columnKey,
      Function<T, BigDecimal> value,
      Aggregate aggregate,
      boolean sorted) {
    Objects.requireNonNull(aggregate, "aggregate");
    if (items.size() > MAX_ROWS) {
      throw new IllegalArgumentException("a pivot table accepts at most " + MAX_ROWS + " rows");
    }
    Map<String, Map<String, List<BigDecimal>>> groups = new LinkedHashMap<>();
    Map<String, Boolean> columns = new LinkedHashMap<>();
    for (T item : items) {
      String row = String.valueOf(rowKey.apply(item));
      String column = String.valueOf(columnKey.apply(item));
      columns.put(column, Boolean.TRUE);
      List<BigDecimal> values =
          groups
              .computeIfAbsent(row, k -> new LinkedHashMap<>())
              .computeIfAbsent(column, k -> new ArrayList<>());
      BigDecimal v = value.apply(item);
      if (v != null || aggregate == Aggregate.COUNT) {
        values.add(v == null ? BigDecimal.ZERO : v);
      }
    }
    List<String> rowKeys = new ArrayList<>(groups.keySet());
    List<String> columnKeys = new ArrayList<>(columns.keySet());
    if (rowKeys.size() > MAX_KEYS || columnKeys.size() > MAX_KEYS) {
      throw new IllegalArgumentException(
          "a pivot table has at most " + MAX_KEYS + " rows and columns");
    }
    if (sorted) {
      Collections.sort(rowKeys);
      Collections.sort(columnKeys);
    }
    List<List<BigDecimal>> cells = new ArrayList<>();
    List<BigDecimal> rowTotals = new ArrayList<>();
    List<List<BigDecimal>> byColumn = new ArrayList<>();
    columnKeys.forEach(c -> byColumn.add(new ArrayList<>()));
    List<BigDecimal> all = new ArrayList<>();
    for (String row : rowKeys) {
      List<BigDecimal> rowCells = new ArrayList<>();
      List<BigDecimal> rowValues = new ArrayList<>();
      for (int c = 0; c < columnKeys.size(); c++) {
        List<BigDecimal> values = groups.get(row).getOrDefault(columnKeys.get(c), List.of());
        rowCells.add(values.isEmpty() ? null : aggregate.apply(values));
        rowValues.addAll(values);
        byColumn.get(c).addAll(values);
      }
      all.addAll(rowValues);
      cells.add(rowCells);
      rowTotals.add(aggregate.apply(rowValues));
    }
    List<BigDecimal> columnTotals = byColumn.stream().map(aggregate::apply).toList();
    return new PivotTable(
        rowKeys, columnKeys, cells, rowTotals, new ArrayList<>(columnTotals), aggregate.apply(all));
  }
}
