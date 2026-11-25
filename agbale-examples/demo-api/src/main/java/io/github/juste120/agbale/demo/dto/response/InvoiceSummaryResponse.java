package io.github.juste120.agbale.demo.dto.response;

import java.time.LocalDate;

/** Invoice in a list. */
public record InvoiceSummaryResponse(
    Long id, String number, String customerName, LocalDate issueDate) {}
