package io.github.juste120.docmint.demo.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Data of the sales statement: monthly sales per city and payment methods. */
public record SalesReportResponse(
    int year,
    List<String> months,
    List<BigDecimal> lome,
    List<BigDecimal> kara,
    List<String> paymentMethods,
    List<BigDecimal> paymentShares,
    BigDecimal total) {}
