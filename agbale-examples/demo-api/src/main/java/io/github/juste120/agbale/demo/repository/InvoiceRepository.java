package io.github.juste120.agbale.demo.repository;

import io.github.juste120.agbale.demo.entity.Invoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of {@link Invoice}. */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  /** Loads an invoice with its customer and lines in one query. */
  @EntityGraph(attributePaths = {"customer", "lines"})
  Optional<Invoice> findWithDetailsById(Long id);
}
