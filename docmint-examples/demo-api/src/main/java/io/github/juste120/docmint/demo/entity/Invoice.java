package io.github.juste120.docmint.demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** An invoice with its lines. */
@Entity
public class Invoice {

  @Id @GeneratedValue private Long id;

  @Column(unique = true, nullable = false)
  private String number;

  private LocalDate issueDate;
  private LocalDate dueDate;
  private boolean paid;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Customer customer;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<InvoiceLine> lines = new ArrayList<>();

  protected Invoice() {}

  public Invoice(String number, LocalDate issueDate, LocalDate dueDate, Customer customer) {
    this.number = number;
    this.issueDate = issueDate;
    this.dueDate = dueDate;
    this.customer = customer;
  }

  /** Adds a line to this invoice. */
  public Invoice addLine(InvoiceLine line) {
    line.attachTo(this);
    lines.add(line);
    return this;
  }

  /** Marks this invoice as paid. */
  public Invoice markPaid() {
    this.paid = true;
    return this;
  }

  public boolean isPaid() {
    return paid;
  }

  public Long getId() {
    return id;
  }

  public String getNumber() {
    return number;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Customer getCustomer() {
    return customer;
  }

  public List<InvoiceLine> getLines() {
    return lines;
  }
}
