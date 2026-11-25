package io.github.juste120.docmint.demo.mapper;

import io.github.juste120.docmint.demo.dto.response.CustomerResponse;
import io.github.juste120.docmint.demo.dto.response.InvoiceLineResponse;
import io.github.juste120.docmint.demo.dto.response.InvoiceResponse;
import io.github.juste120.docmint.demo.dto.response.InvoiceSummaryResponse;
import io.github.juste120.docmint.demo.entity.Customer;
import io.github.juste120.docmint.demo.entity.Invoice;
import io.github.juste120.docmint.demo.entity.InvoiceLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps invoice entities to response DTOs, computing the totals (Togo VAT: 18 %). */
@Mapper
public interface InvoiceMapper {

  BigDecimal VAT_RATE = new BigDecimal("0.18");

  @Mapping(target = "totalExcludingTax", expression = "java(totalExcludingTax(invoice))")
  @Mapping(target = "vatRate", expression = "java(VAT_RATE)")
  @Mapping(target = "vat", expression = "java(vat(invoice))")
  @Mapping(
      target = "totalIncludingTax",
      expression = "java(totalExcludingTax(invoice).add(vat(invoice)))")
  InvoiceResponse toResponse(Invoice invoice);

  @Mapping(target = "customerName", source = "customer.name")
  InvoiceSummaryResponse toSummary(Invoice invoice);

  CustomerResponse toResponse(Customer customer);

  @Mapping(target = "amount", expression = "java(amount(line))")
  InvoiceLineResponse toResponse(InvoiceLine line);

  default BigDecimal amount(InvoiceLine line) {
    return line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
  }

  default BigDecimal totalExcludingTax(Invoice invoice) {
    return invoice.getLines().stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  default BigDecimal vat(Invoice invoice) {
    return totalExcludingTax(invoice).multiply(VAT_RATE).setScale(0, RoundingMode.HALF_UP);
  }
}
