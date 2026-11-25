package io.github.juste120.docmint.demo.service;

import io.github.juste120.docmint.demo.dto.response.InvoiceResponse;
import io.github.juste120.docmint.demo.dto.response.InvoiceSummaryResponse;
import java.util.List;

/** Invoice use cases. */
public interface InvoiceService {

  List<InvoiceSummaryResponse> findAll();

  InvoiceResponse findById(Long id);
}
