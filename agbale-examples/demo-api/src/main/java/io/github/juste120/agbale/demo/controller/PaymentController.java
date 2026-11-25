package io.github.juste120.agbale.demo.controller;

import io.github.juste120.agbale.demo.dto.response.PaymentSummaryResponse;
import io.github.juste120.agbale.demo.dto.response.ReceiptResponse;
import io.github.juste120.agbale.demo.service.PaymentService;
import io.github.juste120.agbale.spring.web.Report;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payments: JSON list and PDF receipt. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  @GetMapping
  public List<PaymentSummaryResponse> list() {
    return service.findAll();
  }

  @GetMapping("/{id}/receipt")
  @Report(value = "recu", filename = "recu-{number}.pdf")
  public ReceiptResponse receipt(@PathVariable Long id) {
    return service.receipt(id);
  }
}
