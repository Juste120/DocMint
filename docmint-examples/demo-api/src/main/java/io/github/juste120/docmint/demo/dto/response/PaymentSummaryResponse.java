package io.github.juste120.docmint.demo.dto.response;

import java.math.BigDecimal;

/** Payment in a list. */
public record PaymentSummaryResponse(Long id, String number, String payerName, BigDecimal amount) {}
