package io.github.juste120.docmint.demo.controller;

import io.github.juste120.docmint.demo.dto.response.SalesReportResponse;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.spring.web.Report;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sales statement with charts, as PDF or as Excel (one sheet per table of the template). */
@RestController
@RequestMapping("/api/reports/sales")
public class SalesReportController {

  @GetMapping("/pdf")
  @Report(value = "releve-ventes", filename = "releve-ventes-{year}.pdf")
  public SalesReportResponse pdf() {
    return report();
  }

  @GetMapping("/xlsx")
  @Report(
      value = "releve-ventes",
      filename = "releve-ventes-{year}.xlsx",
      format = OutputFormat.XLSX)
  public SalesReportResponse excel() {
    return report();
  }

  private static SalesReportResponse report() {
    List<BigDecimal> lome = amounts(1_250_000, 980_000, 1_500_000, 1_320_000, 1_710_000, 1_890_000);
    List<BigDecimal> kara = amounts(410_000, 520_000, 480_000, 630_000, 700_000, 760_000);
    BigDecimal total =
        Stream.concat(lome.stream(), kara.stream()).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new SalesReportResponse(
        2026,
        List.of("Janvier", "Février", "Mars", "Avril", "Mai", "Juin"),
        lome,
        kara,
        List.of("Mobile Money", "Espèces", "Virement"),
        amounts(58, 30, 12),
        total);
  }

  private static List<BigDecimal> amounts(long... values) {
    return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
  }
}
