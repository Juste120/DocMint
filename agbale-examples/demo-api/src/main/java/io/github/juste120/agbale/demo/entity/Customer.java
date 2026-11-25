package io.github.juste120.agbale.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** A customer. */
@Entity
public class Customer {

  @Id @GeneratedValue private Long id;
  private String code;
  private String name;
  private String address;
  private String city;
  private String phone;

  protected Customer() {}

  public Customer(String code, String name, String address, String city, String phone) {
    this.code = code;
    this.name = name;
    this.address = address;
    this.city = city;
    this.phone = phone;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getCity() {
    return city;
  }

  public String getPhone() {
    return phone;
  }
}
