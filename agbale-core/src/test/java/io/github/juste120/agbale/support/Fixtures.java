package io.github.juste120.agbale.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Test data shared by the test classes. */
public final class Fixtures {

  private Fixtures() {}

  /** A customer. */
  public record Customer(String name) {}

  /** An invoice line. */
  public record Line(String label, BigDecimal amount) {}

  /** An invoice. */
  public record Invoice(
      String number, Customer customer, List<Line> lines, BigDecimal discount, LocalDate date) {}

  /** A typical invoice: 2 lines, total 1 250 000, no discount. */
  public static Invoice invoice() {
    return new Invoice(
        "F-2026-001",
        new Customer("Kossi Mensah"),
        List.of(
            new Line("Ciment 50 kg", new BigDecimal("1000000")),
            new Line("Transport Lomé - Kpalimé", new BigDecimal("250000"))),
        null,
        LocalDate.of(2026, 11, 24));
  }

  /** Same invoice as a JavaBean, for record/POJO equivalence tests. */
  public static final class InvoiceBean {
    private final Invoice invoice = invoice();

    public String getNumber() {
      return invoice.number();
    }

    public CustomerBean getCustomer() {
      return new CustomerBean();
    }

    public List<Line> getLines() {
      return invoice.lines();
    }

    public BigDecimal getDiscount() {
      return null;
    }

    /** A customer bean. */
    public final class CustomerBean {
      public String getName() {
        return invoice.customer().name();
      }
    }
  }
}
