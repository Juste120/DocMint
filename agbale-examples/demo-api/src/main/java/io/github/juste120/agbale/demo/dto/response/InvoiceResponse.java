package io.github.juste120.agbale.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Everything the invoice template needs. Templates never receive JPA entities. */
public record InvoiceResponse(
    String number,
    LocalDate issueDate,
    LocalDate dueDate,
    CustomerResponse customer,
    List<InvoiceLineResponse> lines,
    BigDecimal totalExcludingTax,
    BigDecimal vatRate,
    BigDecimal vat,
    BigDecimal totalIncludingTax) {}
