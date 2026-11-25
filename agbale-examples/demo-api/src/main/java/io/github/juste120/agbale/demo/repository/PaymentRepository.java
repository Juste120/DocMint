package io.github.juste120.agbale.demo.repository;

import io.github.juste120.agbale.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of {@link Payment}. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {}
