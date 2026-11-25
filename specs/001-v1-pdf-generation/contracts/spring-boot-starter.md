# Contract — Spring Boot starter (`agbale-spring-boot-starter`)

## Configuration properties (`agbale.*`) — all optional

| Property | Type | Default | Description |
|---|---|---|---|
| `agbale.templates-path` | String | `reports` | Classpath root of templates. |
| `agbale.cache.enabled` | boolean | `true` | Template cache. |
| `agbale.locale` | Locale | `fr` | Default locale for `date` and `money`. |
| `agbale.fonts[i].family` / `.location` / `.weight` / `.italic` | — | — | Additional classpath fonts. |
| `agbale.limits.max-template-size` | DataSize | `1MB` | Max size of each template file. |
| `agbale.limits.max-pages` | int | `500` | Max pages per document. |
| `agbale.limits.max-render-time` | Duration | `30s` | Max time per render. |
| `agbale.resources.allowed-hosts` | List<String> | empty | HTTPS hosts allowed for remote images/CSS/fonts. |
| `agbale.resources.missing` | `fail` \| `warn` | `fail` | Behaviour for missing (allowed) resources. |
| `agbale.web.problem-details.enabled` | boolean | `true` | Agbale error handler for `@Report` endpoints. |

Invalid values (negative limits, unknown enum, blank path) fail at startup with a binding
error naming the property.

Beans: `Agbale` (`@ConditionalOnMissingBean`). Web beans only for servlet web applications with
Spring MVC on the class path.

## `@Report`

```java
@Target(METHOD) @Retention(RUNTIME) @Documented
public @interface Report {
    String value();                          // template name (alias: template)
    String filename() default "";            // supports {property.path} placeholders; default "<template>.pdf"
    Disposition disposition() default Disposition.INLINE; // INLINE | ATTACHMENT
}
```

Rules: method must be a handler method returning a data object (not `byte[]`, `ResponseEntity`,
`void` → `IllegalStateException` at first invocation with a clear message). Works with
`@RestController` and `@Controller`.

## HTTP success response

```
200 OK
Content-Type: application/pdf
Content-Disposition: inline; filename="facture-F-2026-001.pdf"; filename*=UTF-8''facture-F-2026-001.pdf
Content-Length: <n>
```

File name sanitising: placeholders resolved from the returned DTO; in resolved values, `/ \ " ;`
and control characters (incl. CR/LF) → `_`; result trimmed, ≤ 150 chars, `.pdf` ensured; empty
→ `<template>.pdf`.

## HTTP error response (RFC 9457)

```
500 Internal Server Error
Content-Type: application/problem+json

{
  "type": "https://github.com/Juste120/agbale/blob/main/docs/user-guide.md#errors",
  "title": "Template not found",
  "status": 500,
  "detail": "Template 'invoce' not found in 'reports'. Available templates: facture, recu. Did you mean 'facture'?",
  "instance": "/api/factures/1/pdf",
  "code": "AGBALE_TEMPLATE_NOT_FOUND"
}
```

| Code | Status |
|---|---|
| `AGBALE_TEMPLATE_NOT_FOUND`, `AGBALE_TEMPLATE_SYNTAX`, `AGBALE_TEMPLATE_EVALUATION`, `AGBALE_INVALID_TEMPLATE_NAME`, `AGBALE_RESOURCE_REFUSED`, `AGBALE_RESOURCE_MISSING`, `AGBALE_LIMIT_EXCEEDED`, `AGBALE_RENDER_FAILED`, `AGBALE_INVALID_REPORT_METHOD` | 500 |
| `AGBALE_NOT_ACCEPTABLE` (client `Accept` excludes `application/pdf`) | 406 |

Never present: stack traces, exception class names, absolute paths, data values.
