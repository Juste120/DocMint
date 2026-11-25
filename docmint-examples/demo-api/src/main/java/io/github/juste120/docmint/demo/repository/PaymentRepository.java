package io.github.juste120.docmint.demo.repository;

import io.github.juste120.docmint.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of {@link Payment}. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {}
