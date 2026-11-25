package io.github.juste120.docmint.demo.repository;

import io.github.juste120.docmint.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of {@link Customer}. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
