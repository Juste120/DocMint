# Data Model — DocMint V1 (Phase 1)

DocMint has no persistent storage. This document describes the in-memory model of `docmint-core`
(package `io.github.juste120.docmint`) and the demo entities.

## Domain (`…docmint.domain`) — pure Java, no dependency

| Type | Kind | Fields / values | Rules |
|---|---|---|---|
| `TemplateName` | record | `value` (normalised, without `.html`) | `of(raw)`: not blank; ≤ 200 chars; segments separated by `/`; each segment matches `[\p{L}\p{N}_-]+(\.[\p{L}\p{N}_-]+)*`; no `..`, no leading `/`, no `\`, no `:`, no `%`, no control char; trailing `.html` removed. `path()` → `value + ".html"`. Throws `IllegalArgumentException` (translated to `InvalidTemplateNameException`). |
| `OutputFormat` | enum | `PDF("application/pdf", "pdf")` | `mediaType()`, `fileExtension()`. New constants added by future versions (non-breaking). |
| `PageSettings` | record | `size` (`A4`), `orientation` (`PORTRAIT`/`LANDSCAPE`), `marginTop/Right/Bottom/Left` (mm) | defaults `A4`, portrait, 20/15/20/15 mm; margins ≥ 0. Used to generate the default `@page` CSS. |
| `ResourceLimits` | record | `maxTemplateBytes` (1 MiB), `maxPages` (500), `maxRenderTime` (`Duration` 30 s) | all > 0; `defaults()`; derived `maxRenderedChars()` = 8 × maxTemplateBytes. |
| `MissingResourceMode` | enum | `FAIL`, `WARN` | default `FAIL`. |
| `ResourceAccessPolicy` | record | `allowedHosts` (`Set<String>`, lower-case) | default empty (classpath + data URIs only). `isAllowed(uri)`; host entries are exact or `*.example.com`. `http` never allowed; `https` only for allowed hosts. |
| `FontDefinition` | record | `family`, `classpathLocation`, `weight` (100–900, default 400), `italic` | location validated like a template path (classpath only). |
| `RenderedDocument` | record | `content` (`byte[]`, defensive copy), `format`, `pageCount` | produced by a `DocumentRenderer`. |

## Public API contract types (`…docmint.api`)

| Type | Kind | Fields | Notes |
|---|---|---|---|
| `RenderOptions` | record | `locale` (`Locale`, nullable → engine default), `fileName` (nullable) | `RenderOptions.defaults()`; builder-style `withLocale`, `withFileName`. |
| `RenderRequest` | record | `templateName` (String), `data` (Object, non-null), `format` (`OutputFormat`, default PDF), `options` | `RenderRequest.of(name, data)`. |
| `RenderResponse` | record | `content` (byte[]), `mediaType`, `fileName`, `pageCount` | `fileName` = options file name or `<last-segment>.pdf`. Defensive copies. |
| `DocMintException` | abstract, unchecked | `code()` (stable string) | root of the hierarchy. |
| `TemplateNotFoundException` | final | `templateName`, `availableTemplates` (≤ 50), `suggestion` (Optional) | code `DOCMINT_TEMPLATE_NOT_FOUND`. |
| `TemplateSyntaxException` | final | `templateName`, `line`, `column` (nullable) | code `DOCMINT_TEMPLATE_SYNTAX`. |
| `RenderException` | non-final | — | code `DOCMINT_RENDER_FAILED`; also used for runtime template errors (undefined variable, wrong filter input) with code `DOCMINT_TEMPLATE_EVALUATION`. |
| `InvalidTemplateNameException` | extends `RenderException` | `reason` | code `DOCMINT_INVALID_TEMPLATE_NAME`. Never echoes more than 100 chars of the rejected name (control chars stripped). |
| `ResourceRefusedException` | extends `RenderException` | `resources` (sanitised URIs) | code `DOCMINT_RESOURCE_REFUSED`. |
| `ResourceMissingException` | extends `RenderException` | `resources` | code `DOCMINT_RESOURCE_MISSING`. |
| `LimitExceededException` | extends `RenderException` | `limit` (`TEMPLATE_SIZE`, `PAGES`, `RENDER_TIME`), `configuredValue` | code `DOCMINT_LIMIT_EXCEEDED`; message names the builder method / property to change. |

## Template model (output of `TemplateModelMapper`)

```
root object  ──► Map<String,Object>  (top-level variables)
   record     → components (accessor order)
   POJO       → public getters getX()/isX() (excluding getClass)
   Map        → String keys (non-String keys → String.valueOf)
   Collection/array/scalar at root → { "data": <converted value> }
nested values:
   null, String, Boolean, Character→String, Number (kept), Enum→name(),
   java.time.* / Date / Calendar (kept, consumed by the date filter),
   Optional → value or null, Collection/array → List, Map → Map, other object → Map (as above)
```

Constraints: immutable (`Map.copyOf` semantics allowing nulls via unmodifiable wrappers), cycle
detection (identity), max depth 32, max 100 000 nodes → `RenderException`.

## State transitions

Rendering is stateless per call. The only shared state is the template cache:
`absent → compiled (on first render) → cleared (clearCache() / cache disabled)`.

## Demo API entities (`docmint-examples/demo-api`, H2)

| Entity | Fields | Relations |
|---|---|---|
| `Customer` | id, code, name, address, city, phone | 1 — n `Invoice` |
| `Invoice` | id, number (unique, e.g. `F-2026-001`), issueDate, dueDate, customer | 1 — n `InvoiceLine` |
| `InvoiceLine` | id, description, quantity, unitPrice (`BigDecimal`), invoice | — |
| `Payment` (receipt) | id, number (`R-2026-001`), date, amount, method, payer name, invoice ref | — |
| `Employee` (attestation) | id, matricule, fullName, position, hireDate | — |

Response DTOs (records): `InvoiceResponse(numero, dateEmission, dateEcheance, client, lignes,
totalHt, tva, totalTtc)`, `ReceiptResponse`, `AttestationResponse`. Templates only ever receive
these DTOs (MapStruct mappers from entities).
