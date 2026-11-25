package io.github.juste120.agbale.spring.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-031: file name placeholders and sanitising. */
class ReportFileNameResolverTest {

  record Customer(String code) {}

  record Invoice(String number, Customer customer) {}

  public static class Bean {
    public String getNumber() {
      return "B-1";
    }

    public boolean isPaid() {
      return true;
    }
  }

  @Test
  void resolvesPlaceholdersFromRecordsBeansAndMaps() {
    Invoice invoice = new Invoice("F-1", new Customer("C-9"));

    assertThat(
            ReportFileNameResolver.resolve(
                "facture-{number}-{customer.code}.pdf", "invoice", invoice))
        .isEqualTo("facture-F-1-C-9.pdf");
    assertThat(ReportFileNameResolver.resolve("{number}-{paid}", "invoice", new Bean()))
        .isEqualTo("B-1-true.pdf");
    assertThat(ReportFileNameResolver.resolve("{number}.PDF", "x", Map.of("number", 7)))
        .isEqualTo("7.PDF");
  }

  @Test
  void defaultsToTheTemplateName() {
    assertThat(ReportFileNameResolver.resolve("", "invoices/standard", Map.of()))
        .isEqualTo("standard.pdf");
    assertThat(ReportFileNameResolver.resolve(null, "invoice", Map.of())).isEqualTo("invoice.pdf");
    Map<String, Object> empty = new HashMap<>();
    empty.put("number", null);
    assertThat(ReportFileNameResolver.resolve("{number}", "invoice", empty))
        .isEqualTo("invoice.pdf");
  }

  @Test
  void sanitisesDangerousCharacters() {
    assertThat(ReportFileNameResolver.sanitise("a/b\\c\"d;e\r\nf\u0000g .pdf"))
        .isEqualTo("a_b_c_d_e__f_g_.pdf");
    assertThat(ReportFileNameResolver.sanitise("..hidden")).isEqualTo("hidden.pdf");
    String longName = ReportFileNameResolver.sanitise("x".repeat(500));
    assertThat(longName).hasSize(ReportFileNameResolver.MAX_LENGTH).endsWith(".pdf");
  }

  @Test
  void unknownPropertyIsAClearError() {
    assertThatThrownBy(
            () -> ReportFileNameResolver.resolve("{nope}", "invoice", new Invoice("1", null)))
        .isInstanceOf(InvalidReportMethodException.class)
        .hasMessageContaining("{nope}");
    assertThatThrownBy(() -> ReportFileNameResolver.resolve("{nope}", "invoice", Map.of()))
        .isInstanceOf(InvalidReportMethodException.class);
    assertThatThrownBy(() -> ReportFileNameResolver.resolve("{nope}", "invoice", new Bean()))
        .isInstanceOf(InvalidReportMethodException.class);
    assertThat(ReportFileNameResolver.resolve("{customer.code}", "i", new Invoice("1", null)))
        .isEqualTo("i.pdf");
  }
}
