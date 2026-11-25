package io.github.juste120.docmint.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Everything the receipt template needs. */
public record ReceiptResponse(
    String number,
    LocalDateTime paidAt,
    BigDecimal amount,
    String method,
    String payerName,
    String invoiceNumber) {}
