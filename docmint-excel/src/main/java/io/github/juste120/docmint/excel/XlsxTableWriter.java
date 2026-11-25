package io.github.juste120.docmint.excel;

import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.port.TableData;
import io.github.juste120.docmint.port.TableData.Cell;
import io.github.juste120.docmint.port.TableWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes tables as an Excel workbook: one sheet per table, bold and frozen header rows, typed cells
 * (numbers, dates, booleans), merged cells for {@code colspan}, auto-sized columns.
 *
 * <p>Every cell is written as a value, never as a formula: text such as {@code =HYPERLINK(...)}
 * stays text, so opening the file cannot run anything.
 */
public final class XlsxTableWriter implements TableWriter {

  /** Above this number of rows, columns get a fixed width (auto-sizing is slow). */
  static final int AUTO_SIZE_LIMIT = 2000;

  private static final int MAX_SHEET_NAME = 31;

  @Override
  public OutputFormat format() {
    return OutputFormat.XLSX;
  }

  @Override
  public byte[] write(List<TableData> tables, DocumentOptions options) {
    try (Workbook workbook = new XSSFWorkbook()) {
      Styles styles = new Styles(workbook);
      Set<String> names = new HashSet<>();
      for (TableData table : tables) {
        sheet(workbook, styles, table, uniqueName(table.name(), names));
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RenderException("The Excel workbook could not be written.", e);
    }
  }

  private static void sheet(Workbook workbook, Styles styles, TableData table, String name) {
    Sheet sheet = workbook.createSheet(name);
    int maxColumns = 0;
    for (int r = 0; r < table.rows().size(); r++) {
      Row row = sheet.createRow(r);
      boolean header = r < table.headerRows();
      int column = 0;
      for (Cell cell : table.rows().get(r)) {
        String format =
            column < table.columnFormats().size() ? table.columnFormats().get(column) : null;
        write(
            row.createCell(column), cell, header ? styles.header() : styles.forValue(cell, format));
        if (cell.colspan() > 1) {
          sheet.addMergedRegion(new CellRangeAddress(r, r, column, column + cell.colspan() - 1));
        }
        column += cell.colspan();
      }
      maxColumns = Math.max(maxColumns, column);
    }
    if (table.headerRows() > 0) {
      sheet.createFreezePane(0, table.headerRows());
    }
    for (int c = 0; c < maxColumns; c++) {
      if (table.rows().size() <= AUTO_SIZE_LIMIT) {
        sheet.autoSizeColumn(c);
      } else {
        sheet.setColumnWidth(c, 18 * 256);
      }
    }
  }

  private static void write(org.apache.poi.ss.usermodel.Cell target, Cell cell, CellStyle style) {
    switch (cell.type()) {
      case NUMBER -> target.setCellValue(((BigDecimal) cell.value()).doubleValue());
      case BOOLEAN -> target.setCellValue((Boolean) cell.value());
      case DATE -> {
        if (cell.value() instanceof LocalDateTime dateTime) {
          target.setCellValue(dateTime);
        } else {
          target.setCellValue((LocalDate) cell.value());
        }
      }
      default -> target.setCellValue(cell.text());
    }
    if (style != null) {
      target.setCellStyle(style);
    }
  }

  static String uniqueName(String wanted, Set<String> used) {
    String base =
        wanted == null || wanted.isBlank()
            ? "Sheet" + (used.size() + 1)
            : WorkbookUtil.createSafeSheetName(wanted.strip(), '_');
    if (base.length() > MAX_SHEET_NAME) {
      base = base.substring(0, MAX_SHEET_NAME);
    }
    String name = base;
    for (int i = 2; !used.add(name.toLowerCase()); i++) {
      String suffix = " (" + i + ")";
      name = base.substring(0, Math.min(base.length(), MAX_SHEET_NAME - suffix.length())) + suffix;
    }
    return name;
  }

  /** Cell styles, created once per workbook (Excel limits the number of styles). */
  private static final class Styles {
    private final Workbook workbook;
    private final CellStyle header;
    private final CellStyle date;
    private final CellStyle dateTime;
    private final Map<String, CellStyle> formats = new HashMap<>();

    Styles(Workbook workbook) {
      this.workbook = workbook;
      Font bold = workbook.createFont();
      bold.setBold(true);
      header = workbook.createCellStyle();
      header.setFont(bold);
      date = format("dd/mm/yyyy");
      dateTime = format("dd/mm/yyyy hh:mm");
    }

    CellStyle header() {
      return header;
    }

    CellStyle forValue(Cell cell, String format) {
      if (format != null && !format.isBlank()) {
        return format(format);
      }
      return switch (cell.type()) {
        case DATE -> cell.value() instanceof LocalDateTime ? dateTime : date;
        default -> null;
      };
    }

    private CellStyle format(String pattern) {
      return formats.computeIfAbsent(
          pattern,
          p -> {
            CellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat(p));
            return style;
          });
    }
  }
}
