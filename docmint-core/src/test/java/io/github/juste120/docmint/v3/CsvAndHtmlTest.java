package io.github.juste120.docmint.v3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.ExcelColumn;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.ResourceLimits;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** V3 user story 3 (CSV), HTML output and list exports (FR-203 to FR-208). */
class CsvAndHtmlTest {

  private final DocMint docMint = DocMint.createDefault();

  private RenderResponse render(String template, OutputFormat format, RenderOptions options) {
    return docMint.render(new RenderRequest(template, Map.of(), format, options));
  }

  private static String text(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Test
  void templateTableAsFrenchExcelCsv() {
    RenderResponse response = render("v3/table", OutputFormat.CSV, RenderOptions.defaults());

    assertThat(response.mediaType()).isEqualTo("text/csv");
    assertThat(response.fileName()).isEqualTo("table.csv");
    String csv = text(response.content());
    assertThat(csv).startsWith("﻿").doesNotContain("Ignored");
    assertThat(csv.substring(1).split("\r\n"))
        .containsExactly(
            "Client;Montant;Date;Note",
            "\"Kossi; Mensah\";1250000;2026-11-24;\"\"\"Urgent\"\"\"",
            "'=1+1;12.5;'-;'+33 6 00",
            "Total;;'@SUM;ligne suite");
  }

  @Test
  void rfc4180Dialect() {
    String csv =
        text(
            render(
                    "v3/table",
                    OutputFormat.CSV,
                    RenderOptions.defaults().withCsv(CsvSettings.rfc4180()))
                .content());

    assertThat(csv).startsWith("Client,Montant,Date,Note\r\n").contains("Kossi; Mensah,1250000");
    assertThatThrownBy(() -> new CsvSettings('x', true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void noTableIsAClearErrorAndExcelNeedsTheModule() {
    assertThatThrownBy(() -> render("v3/no-table", OutputFormat.CSV, RenderOptions.defaults()))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("has no <table>");
    assertThatThrownBy(() -> render("v3/table", OutputFormat.XLSX, RenderOptions.defaults()))
        .hasMessageContaining("io.github.juste120:docmint-excel");
    assertThatThrownBy(() -> docMint.export(List.of(Map.of("a", 1)), OutputFormat.XLSX, null))
        .hasMessageContaining("docmint-excel");
  }

  @Test
  void htmlOutputIsTheTemplateResult() {
    RenderResponse html =
        docMint.render(new RenderRequest("simple", Map.of("name", "Ama"), OutputFormat.HTML, null));

    assertThat(html.mediaType()).isEqualTo("text/html");
    assertThat(text(html.content())).contains("<p>Bonjour");
  }

  record Payment(
      @ExcelColumn(name = "Payeur", order = 1) String payer,
      @ExcelColumn(name = "Montant", order = 2, format = "#,##0") BigDecimal amount,
      @ExcelColumn(order = 3) LocalDate date,
      String secret) {}

  /** A JavaBean with an annotated private field and a getter. */
  public static class Bean {
    @ExcelColumn(name = "Nom")
    private final String name = "Ama";

    public String getName() {
      return name;
    }

    public boolean isActive() {
      return true;
    }
  }

  /** A bean without annotations: every property is exported. */
  public static class Plain {
    public String getCity() {
      return "Lomé";
    }

    public LocalDateTime getAt() {
      return LocalDateTime.of(2026, 1, 2, 3, 4);
    }
  }

  @Test
  void annotatedRecordsAsCsv() {
    String csv =
        text(
            docMint.csv(
                List.of(
                    new Payment(
                        "=cmd|' /C calc'!A0",
                        new BigDecimal("150000"),
                        LocalDate.of(2026, 11, 24),
                        "x"),
                    new Payment("Ama", new BigDecimal("75500.50"), null, "y"))));

    assertThat(csv.substring(1).split("\r\n"))
        .containsExactly(
            "Payeur;Montant;date", "'=cmd|' /C calc'!A0;150000;2026-11-24", "Ama;75500.50;");
  }

  @Test
  void beansMapsAndEmptyLists() {
    assertThat(text(docMint.csv(List.of(new Bean()))).substring(1)).isEqualTo("Nom\r\nAma\r\n");
    assertThat(text(docMint.csv(List.of(new Plain()))).substring(1))
        .isEqualTo("at;city\r\n2026-01-02T03:04;Lomé\r\n");
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("b", 2);
    row.put("a", true);
    assertThat(text(docMint.csv(List.of(row))).substring(1)).isEqualTo("b;a\r\n2;true\r\n");
    assertThat(text(docMint.csv(List.of())).substring(1)).isEqualTo("\r\n");
  }

  @Test
  void exportRowLimit() {
    DocMint small =
        DocMint.builder().limits(ResourceLimits.defaults().withMaxExportRows(1)).build();

    assertThatThrownBy(() -> small.csv(List.of(Map.of("a", 1), Map.of("a", 2))))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.EXPORT_ROWS));
    assertThatThrownBy(
            () ->
                small.render(
                    new RenderRequest(
                        "v3/table", Map.of(), OutputFormat.CSV, RenderOptions.defaults())))
        .isInstanceOf(LimitExceededException.class);
  }
}
