package io.github.juste120.agbale.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

/** A line of an invoice. */
@Entity
public class InvoiceLine {

  @Id @GeneratedValue private Long id;
  private String description;
  private int quantity;
  private BigDecimal unitPrice;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Invoice invoice;

  protected InvoiceLine() {}

  public InvoiceLine(String description, int quantity, BigDecimal unitPrice) {
    this.description = description;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
  }

  void attachTo(Invoice owner) {
    this.invoice = owner;
  }

  public Long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public Invoice getInvoice() {
    return invoice;
  }
}
