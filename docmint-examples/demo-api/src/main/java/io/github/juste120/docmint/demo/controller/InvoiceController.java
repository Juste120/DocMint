package io.github.juste120.docmint.demo.controller;

import io.github.juste120.docmint.demo.dto.response.InvoiceResponse;
import io.github.juste120.docmint.demo.dto.response.InvoiceSummaryResponse;
import io.github.juste120.docmint.demo.service.InvoiceService;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.Watermark;
import io.github.juste120.docmint.spring.web.Disposition;
import io.github.juste120.docmint.spring.web.Report;
import io.github.juste120.docmint.spring.web.ReportContent;
import java.util.List;
import java.util.stream.Stream;
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

  /** The same list as an Excel workbook: columns from @ExcelColumn, no template needed. */
  @GetMapping("/export.xlsx")
  @Report(
      format = OutputFormat.XLSX,
      filename = "factures.xlsx",
      disposition = Disposition.ATTACHMENT)
  public List<InvoiceSummaryResponse> exportExcel() {
    return service.findAll();
  }

  /** Streamed CSV: rows are written one by one (use a repository Stream for large tables). */
  @GetMapping("/stream.csv")
  @Report(format = OutputFormat.CSV, filename = "factures-flux.csv")
  public Stream<InvoiceSummaryResponse> exportStream() {
    return service.findAll().stream();
  }

  /** The same list as CSV (';' and BOM: opens directly in Excel in French). */
  @GetMapping("/export.csv")
  @Report(
      format = OutputFormat.CSV,
      filename = "factures.csv",
      disposition = Disposition.ATTACHMENT)
  public List<InvoiceSummaryResponse> exportCsv() {
    return service.findAll();
  }

  /**
   * The PDF endpoint: one annotation. Returning a {@link ReportContent} instead of the DTO adds a
   * watermark on unpaid invoices.
   */
  @GetMapping("/{id}/pdf")
  @Report(value = "facture", filename = "facture-{number}.pdf")
  public ReportContent pdf(@PathVariable Long id) {
    InvoiceResponse invoice = service.findById(id);
    return ReportContent.of(invoice)
        .withWatermark(invoice.paid() ? null : Watermark.text("NON PAYÉE").withColor("#C00000"));
  }

  /** PNG preview of the first page, e.g. for a thumbnail. */
  @GetMapping("/{id}/preview")
  @Report(value = "facture", filename = "apercu-{number}", format = OutputFormat.PNG)
  public InvoiceResponse preview(@PathVariable Long id) {
    return service.findById(id);
  }
}
