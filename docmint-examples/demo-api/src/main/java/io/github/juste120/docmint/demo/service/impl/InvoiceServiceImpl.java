package io.github.juste120.docmint.demo.service.impl;

import io.github.juste120.docmint.demo.dto.response.InvoiceResponse;
import io.github.juste120.docmint.demo.dto.response.InvoiceSummaryResponse;
import io.github.juste120.docmint.demo.mapper.InvoiceMapper;
import io.github.juste120.docmint.demo.repository.InvoiceRepository;
import io.github.juste120.docmint.demo.service.InvoiceService;
import io.github.juste120.docmint.demo.service.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class InvoiceServiceImpl implements InvoiceService {

  private final InvoiceRepository repository;
  private final InvoiceMapper mapper;

  InvoiceServiceImpl(InvoiceRepository repository, InvoiceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<InvoiceSummaryResponse> findAll() {
    return repository.findAll().stream().map(mapper::toSummary).toList();
  }

  @Override
  public InvoiceResponse findById(Long id) {
    return repository
        .findWithDetailsById(id)
        .map(mapper::toResponse)
        .orElseThrow(() -> new NotFoundException("Invoice", id));
  }
}
