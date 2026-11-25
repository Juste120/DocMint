package io.github.juste120.agbale.demo.repository;

import io.github.juste120.agbale.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of {@link Customer}. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
