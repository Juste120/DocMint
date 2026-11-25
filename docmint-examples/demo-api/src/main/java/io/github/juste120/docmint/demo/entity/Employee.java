package io.github.juste120.docmint.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDate;

/** An employee, for work attestations. */
@Entity
public class Employee {

  @Id @GeneratedValue private Long id;

  @Column(unique = true, nullable = false)
  private String registrationNumber;

  private String fullName;
  private String position;
  private LocalDate hireDate;

  protected Employee() {}

  public Employee(String registrationNumber, String fullName, String position, LocalDate hireDate) {
    this.registrationNumber = registrationNumber;
    this.fullName = fullName;
    this.position = position;
    this.hireDate = hireDate;
  }

  public Long getId() {
    return id;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPosition() {
    return position;
  }

  public LocalDate getHireDate() {
    return hireDate;
  }
}
