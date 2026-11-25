# 0008. RFC 9457 Problem Details for HTTP errors

- Status: Accepted
- Date: 2026-11-25

## Context

`@Report` endpoints are consumed by front ends (Angular). Errors must be machine-readable,
stable (public contract) and never leak stack traces or paths (Principle VIII).

## Decision

Agbale errors in Spring MVC become **RFC 9457** responses (`application/problem+json`) built
with Spring's `ProblemDetail`: `type` (link to the user guide), `title`, `status`, `detail`
(actionable message), `instance`, plus an Agbale-specific `code` member
(`AGBALE_TEMPLATE_NOT_FOUND`, …). Template and render errors are developer/server errors → 500;
a client that does not accept PDF → 406. The handler is a `@RestControllerAdvice` with highest
precedence, limited to `AgbaleException`, and can be disabled
(`agbale.web.problem-details.enabled=false`).

## Consequences

- The list of error codes is part of the public API (documented in the user guide).
- Errors are logged at WARN with code and message (no data), DEBUG with the stack trace.

## Alternatives considered

A custom JSON format (non-standard) or Spring's default `/error` body (not stable, may include
internal details depending on configuration).
