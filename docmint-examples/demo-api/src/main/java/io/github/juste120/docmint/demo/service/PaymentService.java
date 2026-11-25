package io.github.juste120.docmint.demo.service;

import io.github.juste120.docmint.demo.dto.response.PaymentSummaryResponse;
import io.github.juste120.docmint.demo.dto.response.ReceiptResponse;
import java.util.List;

/** Payment use cases. */
public interface PaymentService {

  List<PaymentSummaryResponse> findAll();

  ReceiptResponse receipt(Long id);
}
