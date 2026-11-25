package io.github.juste120.agbale.demo.dto.response;

/** Customer as shown on documents. */
public record CustomerResponse(
    String code, String name, String address, String city, String phone) {}
