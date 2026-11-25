package io.github.juste120.docmint.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.ExcelColumn;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.ResourceLimits;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** V3 user story 2: Excel export (FR-204, FR-206, FR-207). */
class ExcelExportTest {

  private final DocMint docMint = DocMint.createDefault();

  record InvoiceRow(
      @ExcelColumn(name = "Montant", order = 3, format = "#,##0") BigDecimal amount,
      @ExcelColumn(name = "N°", order = 1) String number,
      @ExcelColumn(name = "Date", order = 2) LocalDate date,
      String internalNote) {}

  private static Workbook open(byte[] xlsx) throws Exception {
    return new XSSFWorkbook(new ByteArrayInputStream(xlsx));
  }

  @Test
  void templateTablesBecomeTypedSheets() throws Exception {
    RenderResponse response =
        docMint.render(
            new RenderRequest("sales", Map.of(), OutputFormat.XLSX, RenderOptions.defaults()));

    assertThat(response.mediaType()).isEqualTo(OutputFormat.XLSX.mediaType());
    assertThat(response.fileName()).isEqualTo("sales.xlsx");
    try (Workbook workbook = open(response.content())) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
      Sheet sales = workbook.getSheetAt(0);
      assertThat(sales.getSheetName()).isEqualTo("Ventes 2026 _ Lomé");
      assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("Résumé");
      assertThat(sales.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Mois");
      assertThat(
              workbook
                  .getFontAt(sales.getRow(0).getCell(0).getCellStyle().getFontIndex())
                  .getBold())
          .isTrue();
      assertThat(sales.getPaneInformation().isFreezePane()).isTrue();
      assertThat(sales.getRow(1).getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
      assertThat(sales.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(1_250_000d);
      assertThat(DateUtil.isCellDateFormatted(sales.getRow(1).getCell(2))).isTrue();
      assertThat(sales.getRow(1).getCell(2).getLocalDateTimeCellValue().toLocalDate())
          .isEqualTo(LocalDate.of(2026, 1, 31));
      // Formula injection: stays text, never a formula.
      assertThat(sales.getRow(2).getCell(0).getCellType()).isEqualTo(CellType.STRING);
      assertThat(sales.getRow(2).getCell(0).getStringCellValue()).startsWith("=HYPERLINK");
      assertThat(sales.getMergedRegions()).hasSize(1);
      assertThat(workbook.getSheetAt(1).getRow(1).getCell(1).getNumericCellValue()).isEqualTo(2d);
    }
  }

  @Test
  void annotatedRowsAreExportedInOrder() throws Exception {
    List<InvoiceRow> rows =
        List.of(
            new InvoiceRow(new BigDecimal("1250000"), "F-1", LocalDate.of(2026, 11, 24), "x"),
            new InvoiceRow(new BigDecimal("75500"), "F-2", LocalDate.of(2026, 11, 25), "y"));

    RenderResponse response =
        docMint.export(rows, OutputFormat.XLSX, RenderOptions.defaults().withFileName("factures"));

    assertThat(response.fileName()).isEqualTo("factures.xlsx");
    try (Workbook workbook = open(response.content())) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("N°");
      assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Date");
      assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Montant");
      assertThat(sheet.getRow(0).getLastCellNum())
          .isEqualTo((short) 3); // internalNote not exported
      assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(1_250_000d);
      assertThat(sheet.getRow(1).getCell(2).getCellStyle().getDataFormatString())
          .isEqualTo("#,##0");
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
    }
  }

  @Test
  void emptyListGivesAnEmptySheetAndLimitsApply() throws Exception {
    try (Workbook workbook = open(docMint.export(List.of(), OutputFormat.XLSX, null).content())) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
    }
    List<Map<String, Object>> many = new ArrayList<>();
    for (int i = 0; i < 11; i++) {
      many.add(Map.of("n", i));
    }
    DocMint small =
        DocMint.builder().limits(ResourceLimits.defaults().withMaxExportRows(10)).build();
    assertThatThrownBy(() -> small.export(many, OutputFormat.XLSX, null))
        .isInstanceOf(LimitExceededException.class);
  }

  @Test
  void sheetNamesAreSafeAndUnique() {
    Set<String> used = new HashSet<>();
    assertThat(XlsxTableWriter.uniqueName("Ventes", used)).isEqualTo("Ventes");
    assertThat(XlsxTableWriter.uniqueName("ventes", used)).isEqualTo("ventes (2)");
    assertThat(XlsxTableWriter.uniqueName("a[b]:c*d?e/f\\g", used))
        .doesNotContainAnyWhitespaces()
        .hasSizeLessThanOrEqualTo(31);
    assertThat(XlsxTableWriter.uniqueName("x".repeat(40), used)).hasSize(31);
    assertThat(XlsxTableWriter.uniqueName(null, used)).startsWith("Sheet");
  }
}
