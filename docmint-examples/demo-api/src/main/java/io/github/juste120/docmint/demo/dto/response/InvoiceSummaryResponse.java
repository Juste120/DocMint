package io.github.juste120.docmint.demo.dto.response;

import io.github.juste120.docmint.api.ExcelColumn;
import java.time.LocalDate;

/** Invoice in a list; the annotations drive the Excel and CSV exports. */
public record InvoiceSummaryResponse(
    Long id,
    @ExcelColumn(name = "N° facture", order = 1) String number,
    @ExcelColumn(name = "Client", order = 2) String customerName,
    @ExcelColumn(name = "Date", order = 3, format = "dd/mm/yyyy") LocalDate issueDate) {}
