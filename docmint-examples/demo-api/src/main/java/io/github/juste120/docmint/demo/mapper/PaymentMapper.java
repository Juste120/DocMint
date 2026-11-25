package io.github.juste120.docmint.demo.mapper;

import io.github.juste120.docmint.demo.dto.response.PaymentSummaryResponse;
import io.github.juste120.docmint.demo.dto.response.ReceiptResponse;
import io.github.juste120.docmint.demo.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps payments to receipt DTOs. */
@Mapper
public interface PaymentMapper {

  @Mapping(target = "method", expression = "java(label(payment.getMethod()))")
  ReceiptResponse toReceipt(Payment payment);

  PaymentSummaryResponse toSummary(Payment payment);

  default String label(Payment.Method method) {
    return switch (method) {
      case CASH -> "Espèces";
      case MOBILE_MONEY -> "Mobile Money";
      case BANK_TRANSFER -> "Virement bancaire";
    };
  }
}
