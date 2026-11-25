package io.github.juste120.docmint.demo.dto.response;

import java.math.BigDecimal;

/** Invoice line as shown on the invoice. */
public record InvoiceLineResponse(
    String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {}
