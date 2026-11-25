package io.github.juste120.agbale.spring.testapp;

import io.github.juste120.agbale.spring.web.Disposition;
import io.github.juste120.agbale.spring.web.Report;
import java.math.BigDecimal;
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

    @GetMapping("/json")
    public InvoiceResponse json() {
      return invoice("J-1");
    }
  }
}
