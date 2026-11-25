package io.github.juste120.agbale.demo.service.impl;

import io.github.juste120.agbale.demo.dto.response.PaymentSummaryResponse;
import io.github.juste120.agbale.demo.dto.response.ReceiptResponse;
import io.github.juste120.agbale.demo.mapper.PaymentMapper;
import io.github.juste120.agbale.demo.repository.PaymentRepository;
import io.github.juste120.agbale.demo.service.NotFoundException;
import io.github.juste120.agbale.demo.service.PaymentService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository repository;
  private final PaymentMapper mapper;

  PaymentServiceImpl(PaymentRepository repository, PaymentMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<PaymentSummaryResponse> findAll() {
    return repository.findAll().stream().map(mapper::toSummary).toList();
  }

  @Override
  public ReceiptResponse receipt(Long id) {
    return repository
        .findById(id)
        .map(mapper::toReceipt)
        .orElseThrow(() -> new NotFoundException("Payment", id));
  }
}
