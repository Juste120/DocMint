package io.github.juste120.docmint.infrastructure.export;

import io.github.juste120.docmint.port.TableData;
import io.github.juste120.docmint.port.TableData.Cell;
import io.github.juste120.docmint.port.TableData.CellType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Reads the tables of an HTML document as typed rows.
 *
 * <p>Only tables with class {@code docmint-export} are read when there is at least one; otherwise
 * every table. Rows come from {@code thead} (headers), {@code tbody} and {@code tfoot}. A cell
 * value can be given explicitly with {@code data-value} and typed with {@code data-type} ({@code
 * number}, {@code date}, {@code text}); plain numbers such as {@code 1250000} or {@code 12,5} are
 * detected. The sheet name comes from {@code data-docmint-sheet} or the {@code caption}.
 */
public final class TableExtractor {

  private static final Pattern NUMBER = Pattern.compile("-?\\d+([.,]\\d+)?");

  /**
   * Extracts the tables.
   *
   * @param html the HTML document
   * @return the tables, possibly empty
   */
  public List<TableData> extract(String html) {
    Document document = Jsoup.parse(html);
    Elements tables = document.select("table.docmint-export");
    if (tables.isEmpty()) {
      tables = document.select("table");
    }
    List<TableData> result = new ArrayList<>();
    for (Element table : tables) {
      result.add(table(table));
    }
    return result;
  }

  private static TableData table(Element table) {
    String name = table.attr("data-docmint-sheet");
    if (name.isBlank()) {
      Element caption = table.selectFirst("> caption");
      name = caption == null ? null : caption.text();
    }
    List<List<Cell>> rows = new ArrayList<>();
    int headerRows = 0;
    for (Element row : table.select("> thead > tr")) {
      rows.add(cells(row));
      headerRows++;
    }
    List<Element> body = new ArrayList<>(table.select("> tbody > tr"));
    body.addAll(table.select("> tr"));
    if (headerRows == 0 && !body.isEmpty() && body.get(0).select("> td").isEmpty()) {
      rows.add(cells(body.remove(0)));
      headerRows = 1;
    }
    for (Element row : body) {
      rows.add(cells(row));
    }
    for (Element row : table.select("> tfoot > tr")) {
      rows.add(cells(row));
    }
    return new TableData(name, headerRows, rows, List.of());
  }

  private static List<Cell> cells(Element row) {
    List<Cell> cells = new ArrayList<>();
    for (Element cell : row.select("> th, > td")) {
      cells.add(cell(cell));
    }
    return cells;
  }

  private static Cell cell(Element cell) {
    String text = cell.text().replace(' ', ' ').replace(' ', ' ').strip();
    String raw = cell.hasAttr("data-value") ? cell.attr("data-value").strip() : null;
    String type = cell.attr("data-type").strip();
    int colspan = 1;
    try {
      colspan = Math.max(1, Math.min(100, Integer.parseInt(cell.attr("colspan").strip())));
    } catch (NumberFormatException e) {
      // no or invalid colspan
    }
    String source = raw != null ? raw : text;
    if ("text".equals(type)) {
      return new Cell(text, source, CellType.TEXT, colspan);
    }
    if ("date".equals(type)) {
      try {
        Object date = source.length() <= 10 ? LocalDate.parse(source) : LocalDateTime.parse(source);
        return new Cell(text, date, CellType.DATE, colspan);
      } catch (DateTimeParseException e) {
        return new Cell(text, text, CellType.TEXT, colspan);
      }
    }
    String compact = source.replace(" ", "");
    if ("number".equals(type) || NUMBER.matcher(compact).matches()) {
      try {
        return new Cell(text, new BigDecimal(compact.replace(',', '.')), CellType.NUMBER, colspan);
      } catch (NumberFormatException e) {
        return new Cell(text, text, CellType.TEXT, colspan);
      }
    }
    return new Cell(text, text, CellType.TEXT, colspan);
  }
}
