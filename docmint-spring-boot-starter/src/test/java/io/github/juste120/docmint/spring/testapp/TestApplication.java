package io.github.juste120.docmint.spring.testapp;

import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.PreviewSettings;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.Watermark;
import io.github.juste120.docmint.spring.web.Disposition;
import io.github.juste120.docmint.spring.web.Report;
import io.github.juste120.docmint.spring.web.ReportContent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Minimal application used by the starter web tests. */
@SpringBootApplication
public class TestApplication {

  /** Customer DTO. */
  public record Customer(String name, String code) {}

  /** Invoice DTO. */
  public record InvoiceResponse(String number, Customer customer, BigDecimal total) {}

  static InvoiceResponse invoice(String number) {
    return new InvoiceResponse(
        number, new Customer("Kossi Mensah", "C-01"), new BigDecimal("1250000"));
  }

  /** Test endpoints. */
  @RestController
  public static class InvoiceController {

    @GetMapping("/invoices/{number}/inline")
    @Report("invoice")
    public InvoiceResponse inline(@PathVariable String number) {
      return invoice(number);
    }

    @GetMapping("/invoices/{number}/download")
    @Report(
        value = "invoice",
        filename = "facture-{number}-{customer.code}.pdf",
        disposition = Disposition.ATTACHMENT)
    public InvoiceResponse download(@PathVariable String number) {
      return invoice(number);
    }

    @GetMapping("/invoices/custom")
    @Report(value = "invoice", filename = "{number}")
    public InvoiceResponse custom(@RequestParam String number) {
      return invoice(number);
    }

    @GetMapping("/invoices/accents")
    @Report(value = "invoice", filename = "reçu-élève.pdf")
    public InvoiceResponse accents() {
      return invoice("1");
    }

    @GetMapping("/invoices/map")
    @Report(template = "invoice", filename = "{number}.pdf")
    public Map<String, Object> map() {
      return Map.of("number", "M-1", "customer", Map.of("name", "Ama"), "total", 5);
    }

    @GetMapping("/invoices/optional")
    @Report("invoice")
    public Optional<InvoiceResponse> optional() {
      return Optional.of(invoice("O-1"));
    }

    @GetMapping("/errors/null")
    @Report("invoice")
    public InvoiceResponse returnsNull() {
      return null;
    }

    @GetMapping("/errors/unknown-template")
    @Report("invoce")
    public InvoiceResponse unknownTemplate() {
      return invoice("1");
    }

    @GetMapping("/errors/syntax")
    @Report("broken")
    public Map<String, Object> syntax() {
      return Map.of("items", java.util.List.of(1));
    }

    @GetMapping("/errors/remote")
    @Report("remote")
    public Map<String, Object> remote() {
      return Map.of();
    }

    @GetMapping("/errors/bad-placeholder")
    @Report(value = "invoice", filename = "{nope}.pdf")
    public InvoiceResponse badPlaceholder() {
      return invoice("1");
    }

    @GetMapping("/errors/bytes")
    @Report("invoice")
    public byte[] bytes() {
      return new byte[0];
    }

    @GetMapping("/errors/no-template")
    @Report
    public InvoiceResponse noTemplate() {
      return invoice("1");
    }

    @GetMapping("/v2/watermark")
    @Report("invoice")
    public ReportContent watermarked() {
      return ReportContent.of(invoice("W-1")).withWatermark(Watermark.text("PAYÉE"));
    }

    @GetMapping("/v2/protected")
    @Report(value = "invoice", filename = "bulletin-{number}.pdf")
    public ReportContent protectedPdf() {
      return ReportContent.of(invoice("P-1"))
          .withProtection(Protection.openPassword("1234".toCharArray()))
          .withFileName("confidentiel.pdf");
    }

    @GetMapping("/v2/preview")
    @Report(value = "invoice", filename = "apercu-{number}", format = OutputFormat.PNG)
    public InvoiceResponse preview() {
      return invoice("I-1");
    }

    @GetMapping("/v2/preview-page-2")
    @Report(value = "invoice", format = OutputFormat.PNG)
    public ReportContent previewMissingPage() {
      return ReportContent.of(invoice("I-2")).withPreview(new PreviewSettings(2, 96));
    }

    @GetMapping("/v3/table.csv")
    @Report(value = "table", format = OutputFormat.CSV)
    public Map<String, Object> tableCsv() {
      return Map.of();
    }

    @GetMapping("/v3/list.csv")
    @Report(format = OutputFormat.CSV, filename = "clients.csv")
    public List<Customer> listCsv() {
      return List.of(new Customer("Kossi Mensah", "C-01"), new Customer("=1+1", "C-02"));
    }

    @GetMapping("/v3/list-default.csv")
    @Report(format = OutputFormat.CSV)
    public ReportContent listDefault() {
      return ReportContent.of(List.of(new Customer("Ama", "C-03"))).withCsv(CsvSettings.rfc4180());
    }

    @GetMapping("/v3/table.html")
    @Report(value = "table", format = OutputFormat.HTML)
    public Map<String, Object> tableHtml() {
      return Map.of();
    }

    @GetMapping("/v3/no-template")
    @Report(format = OutputFormat.CSV)
    public Customer notAList() {
      return new Customer("x", "y");
    }

    @GetMapping("/v4/stream.csv")
    @Report(format = OutputFormat.CSV, filename = "flux.csv")
    public java.util.stream.Stream<Customer> stream() {
      return java.util.stream.IntStream.range(0, 3)
          .mapToObj(i -> new Customer("Client " + i, "C-" + i));
    }

    @GetMapping("/v4/async")
    @Report("invoice")
    public java.util.concurrent.CompletableFuture<InvoiceResponse> async() {
      return java.util.concurrent.CompletableFuture.supplyAsync(() -> invoice("A-1"));
    }

    @GetMapping("/json")
    public InvoiceResponse json() {
      return invoice("J-1");
    }
  }
}
