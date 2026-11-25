package io.github.juste120.docmint.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A payment, printed as a receipt. */
@Entity
public class Payment {

  /** Payment methods. */
  public enum Method {
    CASH,
    MOBILE_MONEY,
    BANK_TRANSFER
  }

  @Id @GeneratedValue private Long id;

  @Column(unique = true, nullable = false)
  private String number;

  private LocalDateTime paidAt;
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  private Method method;

  private String payerName;
  private String invoiceNumber;

  protected Payment() {}

  public Payment(
      String number,
      LocalDateTime paidAt,
      BigDecimal amount,
      Method method,
      String payerName,
      String invoiceNumber) {
    this.number = number;
    this.paidAt = paidAt;
    this.amount = amount;
    this.method = method;
    this.payerName = payerName;
    this.invoiceNumber = invoiceNumber;
  }

  public Long getId() {
    return id;
  }

  public String getNumber() {
    return number;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Method getMethod() {
    return method;
  }

  public String getPayerName() {
    return payerName;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }
}
