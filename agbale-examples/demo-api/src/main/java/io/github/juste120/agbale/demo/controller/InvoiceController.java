package io.github.juste120.agbale.demo.controller;

import io.github.juste120.agbale.demo.dto.response.InvoiceResponse;
import io.github.juste120.agbale.demo.dto.response.InvoiceSummaryResponse;
import io.github.juste120.agbale.demo.service.InvoiceService;
import io.github.juste120.agbale.spring.web.Report;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Invoices: JSON list and PDF. */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

  private final InvoiceService service;

  public InvoiceController(InvoiceService service) {
    this.service = service;
  }

  @GetMapping
  public List<InvoiceSummaryResponse> list() {
    return service.findAll();
  }

  /** The whole PDF endpoint: one annotation on a method returning a response DTO. */
  @GetMapping("/{id}/pdf")
  @Report(value = "facture", filename = "facture-{number}.pdf")
  public InvoiceResponse pdf(@PathVariable Long id) {
    return service.findById(id);
  }
}
