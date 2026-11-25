package io.github.juste120.docmint.demo.repository;

import io.github.juste120.docmint.demo.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of {@link Employee}. */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {}
