package io.github.juste120.agbale.demo.dto.response;

import java.time.LocalDate;

/** Everything the work attestation template needs. */
public record AttestationResponse(
    String registrationNumber,
    String fullName,
    String position,
    LocalDate hireDate,
    String purpose,
    LocalDate issuedOn,
    String city) {}
