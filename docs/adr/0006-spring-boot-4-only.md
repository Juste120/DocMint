# 0006. Spring Boot 4.x only in V1

- Status: Accepted
- Date: 2026-11-25

## Context

Spring Boot 4.2 is the current stable line (Java 17 baseline). Boot 3.5 is at the end of its
open source support. Supporting both lines means two test matrices and conditional code
(modularised auto-configuration packages, Jackson 3).

## Decision

The V1 starter targets **Spring Boot 4.x** (built and tested against 4.2.0). Support for 3.5 may
be added later if users ask for it; the design does not prevent it (the web part only uses
stable Spring MVC APIs).

## Consequences

- Projects still on Boot 3.x can use `docmint-core` directly (`DocMint.builder().build()` as a
  `@Bean`).
- Tests use the Boot 4 modules (`spring-boot-webmvc-test`, `ApplicationContextRunner`).
