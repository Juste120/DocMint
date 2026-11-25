package io.github.juste120.docmint.demo.config;

import io.github.juste120.docmint.demo.entity.Customer;
import io.github.juste120.docmint.demo.entity.Employee;
import io.github.juste120.docmint.demo.entity.Invoice;
import io.github.juste120.docmint.demo.entity.InvoiceLine;
import io.github.juste120.docmint.demo.entity.Payment;
import io.github.juste120.docmint.demo.repository.CustomerRepository;
import io.github.juste120.docmint.demo.repository.EmployeeRepository;
import io.github.juste120.docmint.demo.repository.InvoiceRepository;
import io.github.juste120.docmint.demo.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Inserts demo data into the in-memory database at startup. */
@Configuration
public class DemoDataSeeder {

  @Bean
  ApplicationRunner seed(
      CustomerRepository customers,
      InvoiceRepository invoices,
      PaymentRepository payments,
      EmployeeRepository employees) {
    return args -> {
      Customer kossi =
          customers.save(
              new Customer(
                  "C-001", "Kossi Mensah", "Rue 120, Bè-Kpota", "Lomé", "+228 90 00 00 01"));
      Customer akossiwa =
          customers.save(
              new Customer(
                  "C-002", "Akossiwa Ɖogbe", "Quartier Ŋɔɖo", "Kpalimé", "+228 91 00 00 02"));

      Invoice first =
          new Invoice("F-2026-001", LocalDate.of(2026, 11, 24), LocalDate.of(2026, 12, 24), kossi)
              .addLine(new InvoiceLine("Ciment CPJ 45 (sac de 50 kg)", 40, new BigDecimal("5200")))
              .addLine(new InvoiceLine("Fer à béton Ø12", 25, new BigDecimal("4750")))
              .addLine(new InvoiceLine("Transport Lomé – Kpalimé", 1, new BigDecimal("35000")));
      invoices.save(first);
      Invoice second =
          new Invoice(
              "F-2026-002", LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 25), akossiwa);
      for (int i = 1; i <= 45; i++) {
        second.addLine(
            new InvoiceLine(
                "Article de quincaillerie n° " + i, i % 7 + 1, new BigDecimal(1500 + i * 100)));
      }
      invoices.save(second);

      payments.save(
          new Payment(
              "R-2026-001",
              LocalDateTime.of(2026, 11, 24, 10, 30),
              new BigDecimal("150000"),
              Payment.Method.MOBILE_MONEY,
              "Kossi Mensah",
              "F-2026-001"));
      payments.save(
          new Payment(
              "R-2026-002",
              LocalDateTime.of(2026, 11, 25, 16, 5),
              new BigDecimal("75500"),
              Payment.Method.CASH,
              "Akossiwa Ɖogbe",
              "F-2026-002"));

      employees.save(
          new Employee(
              "MAT-0042", "Yawo Agbeko", "Développeur Java senior", LocalDate.of(2021, 3, 1)));
      employees.save(
          new Employee("MAT-0057", "Ama Kpodar", "Comptable", LocalDate.of(2023, 9, 15)));
    };
  }
}
