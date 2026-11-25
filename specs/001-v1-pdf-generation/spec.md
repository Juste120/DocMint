# Feature Specification: DocMint V1 — One-Line PDF Generation

**Feature Branch**: `001-v1-pdf-generation`

**Created**: 2026-11-24

**Status**: Draft

**Input**: User description: "DocMint V1 (0.1.0) – The PDF in one line: an open source Java library
that generates business documents (invoices, receipts, statements, attestations, certificates)
from HTML/CSS templates in one line of code, with a ready-to-use Spring Boot starter. Templates
support variables, loops, conditions and layout inheritance; `docMint.pdf()`; `@Report` with
configurable file name and inline/attachment; `fcfa`, `money`, `sum`, `date` filters (French by
default); embedded free font (accents OK, even under Docker); images from the classpath and Data
URIs; default print CSS (A4, margins, 'Page X / Y', `.page-break`, repeated table headers);
thread-safe template cache; clear errors; demo (Spring API + Angular); publication on Maven
Central; complete open source project files; security by default."

> **Note on vocabulary.** DocMint is a developer library: its "users" are Java developers, and the
> host platforms it integrates with (plain Java, Spring Boot, an Angular front end) are part of
> the product's scope, not implementation choices. Engine, renderer and build-tool choices are
> left to the plan.

## Clarifications

### Session 2026-11-24

- Q: Should the `@Report` file name support values taken from the returned DTO? → A: Yes,
  placeholders resolved from the DTO, with sanitising against header injection (FR-031).
- Q: Which Spring Boot versions must the starter support? → A: Spring Boot 4.x only in V1;
  3.5.x may be added later on demand (FR-035).
- Q: When a template references a missing or refused resource, what should rendering do by
  default? → A: Fail with a clear error by default; a property can switch *missing* resources to
  warn-and-continue, while *refused* resources always fail (FR-034).
- Q: Which JSON format should the starter use for HTTP error responses? → A: RFC 9457 Problem
  Details (`application/problem+json`) with an extra stable `code` field (FR-032).
- Q: What observability should V1 offer on renders? → A: Logs only (DEBUG: template, duration,
  pages; WARN; ERROR), never any data value in logs; no metrics in V1 (FR-047). Decided by the
  assistant at the maintainer's request.
- Q: How is the data object exposed inside templates? → A: Root properties (or map keys) are
  top-level variables (`{{ customer.name }}`); a root that is a list, array or simple value is
  exposed as `data` (FR-002). Decided by the assistant at the maintainer's request.
- The maintainer asked the assistant to stop the questioning loop and apply its recommended
  defaults for any remaining ambiguity (template cache stays enabled by default in every
  environment, disabled only by explicit configuration — FR-008).

## Actors

- **Library user (developer)**: a Java developer who adds DocMint to an application to produce
  business documents.
- **Template author**: the person (often the same developer, sometimes a front-end developer)
  who writes HTML/CSS templates.
- **End user**: the person who views or downloads the generated document, typically through a
  web front end calling an API.
- **Contributor**: an open source developer who reads, builds, tests or extends DocMint.
- **Maintainer**: the project owner (Juste120), who approves and publishes releases.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate a PDF from an HTML template in one line (Priority: P1)

A developer writes an HTML/CSS template containing placeholders, places it in the application's
resources, and obtains a PDF from a data object (plain object, record or key/value map) with a
single call, in plain Java, without any configuration.

**Why this priority**: this is the product. Everything else builds on it; on its own it already
replaces a heavyweight reporting tool for the common case.

**Independent Test**: in a plain Java project with only DocMint added, place `reports/invoice.html`
on the classpath, call the one-line API with a data object, and check that the returned bytes
are a valid PDF whose extracted text contains the expected values.

**Acceptance Scenarios**:

1. **Given** a template `reports/invoice.html` containing `{{ customer.name }}` and a data
   object whose customer name is "Kossi Mensah", **When** the developer calls the one-line PDF
   API with the name `invoice`, **Then** a valid PDF is returned and its text contains
   "Kossi Mensah".
2. **Given** a template that iterates over invoice lines and shows a block only when a discount
   exists, **When** it is rendered with 3 lines and no discount, **Then** the PDF contains the 3
   lines and does not contain the discount block.
3. **Given** a base layout `layouts/base.html` defining a header, a footer and a content block,
   and a template extending it, **When** the template is rendered, **Then** the PDF contains the
   layout's header and footer and the template's content.
4. **Given** the same data passed as a record, a plain object with getters, and a key/value map,
   **When** each is rendered with the same template, **Then** the three PDFs contain the same
   text.
5. **Given** no configuration at all, **When** the developer builds the engine with defaults,
   **Then** templates are looked up under the default templates location (`reports/` on the
   classpath) and rendering succeeds.

---

### User Story 2 - Safe by default, even with untrusted data (Priority: P1)

A developer renders documents that contain data typed by end users (names, addresses, comments)
and templates whose names may come from request parameters, without having to know or
configure any security setting.

**Why this priority**: the constitution makes security non-negotiable; a library inserted into
production APIs must never be the weak point. It must hold from the very first release.

**Independent Test**: a dedicated security test suite feeds malicious inputs (path traversal
names, HTML/script payloads, remote and local file URLs, external XML entities, template code
trying to reach Java internals, oversized or runaway templates) and verifies that each one is
refused or neutralised with a clear, non-sensitive error.

**Acceptance Scenarios**:

1. **Given** a requested template name such as `../../etc/passwd`, `/abs/path`, an encoded
   variant (`%2e%2e%2f`) or a name containing a backslash or a null character, **When** the
   developer renders it, **Then** rendering is refused with an invalid-template-name error and no
   file outside the templates location is read.
2. **Given** data containing `<script>alert(1)</script>` or `<b>bold</b>`, **When** it is
   rendered through a normal placeholder, **Then** the PDF shows the literal characters and no
   markup is interpreted.
3. **Given** a template author explicitly marks a value as raw/trusted HTML, **When** it is
   rendered, **Then** the markup is interpreted (the only way to obtain unescaped output).
4. **Given** a template referencing an image, stylesheet or font via `http://`, `https://`,
   `file://` or any other non-allowed scheme, **When** it is rendered with default settings,
   **Then** the resource is not fetched, no network or filesystem access happens outside the
   classpath, and rendering fails with a resource-refused error naming the resource, whatever
   the missing-resource setting (see FR-034).
5. **Given** the developer has explicitly allow-listed the host `cdn.example.com`, **When** a
   template references `https://cdn.example.com/logo.png`, **Then** that resource is loaded, and
   a resource from any other host is still refused.
6. **Given** template content containing an external XML entity or DTD declaration, **When** it
   is rendered, **Then** the entity is not resolved and no external content is loaded.
7. **Given** template code that tries to call arbitrary methods or reach sensitive classes
   (e.g. `getClass()`, class loaders, system or runtime objects, reflection), **When** it is
   rendered, **Then** rendering fails with a template error and no such call is executed.
8. **Given** default resource limits, **When** a template exceeds the maximum template size,
   produces more pages than the maximum page count, or takes longer than the maximum rendering
   time, **Then** rendering stops with a clear limit-exceeded error naming the limit and how to
   change it.

---

### User Story 3 - Professional business formatting for francophone Africa (Priority: P2)

A template author formats amounts in FCFA or another currency, sums invoice lines, displays
dates in French, uses accented text, adds a logo, and gets a print-ready A4 document with page
numbers, page breaks and repeated table headers, without writing any print CSS.

**Why this priority**: it turns "a PDF" into "a business document" that local companies can
actually send to their customers. It is valuable only once User Story 1 works.

**Independent Test**: render a multi-page invoice template that uses each formatting filter, an
accented customer name, a logo from the classpath and a Data URI image, then check the extracted
text (formatted values, accents, "Page 1 / 3"), the page size and the presence of images.

**Acceptance Scenarios**:

1. **Given** the amount `1250000`, **When** the template applies the `fcfa` filter, **Then** the
   output is `1 250 000 FCFA` (no decimals, space as thousands separator).
2. **Given** the amount `1234.5` and the currency `EUR`, **When** the template applies the
   `money` filter, **Then** the output follows the configured locale (by default French:
   `1 234,50 €`); with an explicit other locale the output follows that locale.
3. **Given** a list of invoice lines, **When** the template applies the `sum` filter on the
   line amount property, **Then** the output is the exact decimal total (no floating-point
   rounding error), and an empty list sums to 0.
4. **Given** the date 2026-11-24, **When** the template applies the `date` filter with no
   pattern, **Then** the output is `24 novembre 2026`; with an explicit pattern the output
   follows that pattern; date-only, date-time and legacy date values are all accepted.
5. **Given** text such as "Société Générale — Lomé, Kpalimé, Ŋlɔ̃, à payer avant le 15/12",
   **When** the document is rendered on a minimal container image with no system fonts,
   **Then** every character is rendered with the embedded font (no missing-glyph boxes) and the
   extracted text matches the input.
6. **Given** a template referencing `images/logo.png` (classpath) and a `data:image/png;base64,…`
   image, **When** it is rendered, **Then** both images appear in the PDF.
7. **Given** a template with no print CSS, **When** it is rendered, **Then** pages are A4
   portrait with default margins and every page shows "Page X / Y" in the footer.
8. **Given** an element with the `page-break` class, **When** rendered, **Then** the following
   content starts on a new page.
9. **Given** a table longer than one page with a header row, **When** rendered, **Then** the
   header row is repeated at the top of each page the table spans.
10. **Given** a template author sets their own page size, margins or footer in CSS, **When**
    rendered, **Then** the author's CSS overrides the defaults.
11. **Given** a developer registers an additional font from the classpath, **When** a template
    uses that font family, **Then** the text is rendered with it.

---

### User Story 4 - Spring Boot API endpoint that returns a PDF (Priority: P2)

A Spring Boot developer adds the DocMint starter, gets a ready-to-use DocMint component injected
with zero configuration, and turns a controller method that returns a Response DTO into a PDF
endpoint with a single annotation, choosing the file name and whether the browser displays or
downloads the document. An Angular front end calls the endpoint and displays or downloads it.

**Why this priority**: this is the main usage context (Spring Boot APIs consumed by Angular).
It depends on User Story 1.

**Independent Test**: in a Spring Boot test application containing only the starter and one
annotated controller, call the endpoint and check the HTTP status, the content type, the
`Content-Disposition` header (inline or attachment, file name) and the PDF text.

**Acceptance Scenarios**:

1. **Given** a Spring Boot application with the starter and no DocMint property, **When** it
   starts, **Then** an DocMint component is available for injection and `docMint.pdf("invoice",
   data)` works anywhere in the application.
2. **Given** a controller method annotated `@Report("invoice")` returning an invoice Response
   DTO, **When** a client calls the endpoint, **Then** the response is `200`, content type
   `application/pdf`, and the body is the rendered PDF.
3. **Given** the annotation specifies a file name and attachment mode, **When** the endpoint is
   called, **Then** `Content-Disposition` is `attachment` with that file name (non-ASCII file
   names correctly encoded); in inline mode it is `inline`.
4. **Given** no file name is specified, **When** the endpoint is called, **Then** the file name
   defaults to `<template-name>.pdf` and the mode defaults to inline.
5. **Given** a front end on another origin, **When** CORS is configured by the application,
   **Then** the documentation explains how to expose `Content-Disposition` so the front end can
   read the file name.
6. **Given** the application sets properties for templates location, cache, locale, fonts and
   limits, **When** it starts, **Then** the DocMint component uses them; invalid property values
   cause a clear startup error.
7. **Given** the application defines its own DocMint component, **When** it starts, **Then** the
   starter backs off and uses the application's component.
8. **Given** a rendering error in an annotated endpoint (unknown template, template syntax
   error, limit exceeded), **When** the endpoint is called, **Then** the response is an
   `application/problem+json` body with the appropriate HTTP status, a stable `code` and an
   actionable `detail`, without stack trace or internal path.
9. **Given** `@Report(value = "facture", filename = "facture-{numero}.pdf")` on a method
   returning an invoice DTO whose number is `F-2026-001`, **When** the endpoint is called,
   **Then** the file name is `facture-F-2026-001.pdf`.
10. **Given** a DTO value containing `../`, a quote or a CR/LF sequence used in the file name
    placeholder, **When** the endpoint is called, **Then** the header contains a sanitised file
    name and no injected header or path.

---

### User Story 5 - Errors that tell the developer what to do (Priority: P3)

A developer who makes a mistake (wrong template name, syntax error, missing variable, missing
image) immediately understands the cause and the fix from the error message.

**Why this priority**: it is decisive for the "first PDF in under 10 minutes" goal, but the
product is usable without polished messages.

**Independent Test**: trigger each error category and assert on the exception type and message
content (and absence of sensitive data).

**Acceptance Scenarios**:

1. **Given** a template name `invoce` while `invoice` and `receipt` exist, **When** rendering,
   **Then** a template-not-found error lists the available templates and suggests `invoice`.
2. **Given** a template with a syntax error at line 12, **When** rendering, **Then** a
   template-syntax error gives the template name, the line (and column when available) and the
   cause.
3. **Given** a template referencing a variable that does not exist in the data, **When**
   rendering, **Then** a clear error names the missing variable and the template (strict mode),
   instead of silently printing nothing.
4. **Given** a template referencing a classpath image that does not exist, **When** rendering
   with default settings, **Then** the error names the missing resource; **When** rendering with
   missing resources in warn mode, **Then** the PDF is produced without the image and a warning
   naming the resource is logged.
5. **Given** any error, **When** its message is inspected, **Then** it contains no absolute
   filesystem path, no stack trace text and no data values other than what the developer
   supplied as the template name.

---

### User Story 6 - Discover, try and adopt the project (Priority: P3)

A developer who has never heard of DocMint finds the repository, reads the README, adds the
dependency from Maven Central, and produces a first PDF in under 10 minutes; they can also run
the demo (Spring API + Angular) to see invoices, receipts and attestations, and a contributor
can build and test the project from `CONTRIBUTING.md`.

**Why this priority**: required for the 0.1.0 release and for an exemplary open source
project, but it packages the value delivered by the previous stories.

**Independent Test**: a person unfamiliar with the project follows only the README on a clean
machine and times the first PDF; a contributor follows `CONTRIBUTING.md` and gets a green local
build; the release pipeline produces signed artifacts accepted by the Maven Central validation.

**Acceptance Scenarios**:

1. **Given** the README only, **When** a newcomer follows the "first PDF in 5 minutes" section,
   **Then** they obtain a PDF in under 10 minutes.
2. **Given** the demo API is started, **When** its invoice, receipt and attestation endpoints are
   called, **Then** each returns a PDF built from Response DTOs (never persistence entities),
   using a shared base layout and a logo.
3. **Given** the demo Angular app is started against the demo API, **When** the user clicks
   "View" or "Download" for a document, **Then** the PDF opens in the browser or is saved with
   the server-provided file name.
4. **Given** the repository, **When** a visitor browses it, **Then** it contains the README
   (English, with a French section, badges, installation, quick start, Spring + Angular example,
   filter reference, FAQ on Docker/fonts/CORS, roadmap), LICENSE (Apache 2.0), CHANGELOG,
   ROADMAP (no dates), CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, issue and PR templates, CI
   workflows, Dependabot configuration, a user guide, an architecture guide with diagrams,
   ADRs, and CLAUDE.md.
5. **Given** a pull request, **When** CI runs, **Then** it builds and tests on Java 17 and 21,
   checks formatting, static analysis, Javadoc, coverage (≥ 80 % on the core module),
   architecture rules and vulnerability scans, and fails if any check fails.
6. **Given** the maintainer triggers a release, **When** CI is green, **Then** the core,
   starter and BOM artifacts are published with signatures, sources, Javadoc and complete
   metadata; example modules are never published.

---

### Edge Cases

- Data object is `null` → clear error (data is required; an empty map is allowed).
- Template name is `null`, empty, blank, or contains only an extension (`.html`) → invalid-name
  error. Names may be given with or without the `.html` extension and may contain sub-folders
  (`invoices/standard`).
- Template file exists but is empty → renders an empty one-page PDF (not an error).
- A `null` value is passed to `fcfa`, `money`, `sum` or `date` → outputs an empty string (no
  crash); a non-numeric value passed to a numeric filter → clear template error.
- Negative amounts → formatted with a leading minus sign (`-15 000 FCFA`).
- Very large amounts (beyond 64-bit integers) and many decimals → exact decimal handling;
  `fcfa` rounds half-up to the unit.
- `sum` on a list containing `null` items or `null` amounts → nulls are ignored.
- Concurrent rendering of the same and of different templates from many threads → correct
  output, no corruption, each template compiled once while cached.
- A template is modified on disk while the cache is enabled → the cached version is used; with
  the cache disabled (development mode) the change is picked up on the next render.
- A layout extended by a template does not exist → template-not-found error naming the layout.
- Mutual or self inheritance between layouts → clear error, no infinite loop.
- A Data URI is malformed or exceeds the template size limit → clear error.
- An image is in an unsupported format → clear error naming the resource.
- A `@Report` method returns `null` → Problem Details error (no PDF with empty content).
- A `@Report` method declares a return type other than a data object (e.g. already returns bytes
  or a response entity) → clear startup or invocation error explaining the rule.
- The client sends `Accept: application/json` only to a `@Report` endpoint → `406` with a
  Problem Details body.
- Characters outside the embedded font's coverage (e.g. emoji, CJK) → documented limitation;
  the developer can register an additional font.
- Locale configured to a value without a French translation → dates and money follow that
  locale; `fcfa` keeps its fixed format.

## Requirements *(mandatory)*

### Functional Requirements

#### Core rendering (US1)

- **FR-001**: The library MUST produce a PDF (as bytes) from a template name and a data object in
  a single call, both from a pre-built instance (`docMint.pdf(name, data)`) and from a default
  builder (`DocMint.builder().build().pdf(name, data)`), without any mandatory configuration.
- **FR-002**: The library MUST accept as data: records, plain objects exposing getters, and
  key/value maps (including nested objects, lists and maps). The properties of the root object
  (record components, getters or map keys) MUST be exposed as top-level template variables
  (`{{ customer.name }}`); a root that is a collection, an array or a simple value MUST be
  exposed under the variable name `data`.
- **FR-003**: Templates MUST be standard HTML/CSS files supporting variables, property access,
  loops, conditions, includes and layout inheritance (a base layout with overridable blocks).
- **FR-004**: Templates MUST be resolved from a configurable classpath location, defaulting to
  `reports/`; the `.html` extension MUST be optional in the requested name; sub-folders MUST be
  supported.
- **FR-005**: The library MUST offer an advanced API taking a render request (template name,
  data, output format, rendering options such as locale override) and returning a render
  response (content bytes, content type, suggested file name, page count), for expert use.
- **FR-006**: The output format model MUST be open to new formats in future versions without
  changing the existing public API; V1 supports PDF only.
- **FR-007**: The library MUST offer extension points allowing experts to provide their own
  template resolver and document renderer without affecting the default one-line usage.
- **FR-008**: Compiled templates MUST be cached in a thread-safe way, enabled by default, with
  the option to disable the cache (development) and to clear it programmatically.
- **FR-009**: A single DocMint instance MUST be safe to share across threads and reusable for
  any number of renders.

#### Security (US2)

- **FR-010**: Template names MUST be validated: path traversal sequences (raw or encoded),
  absolute paths, backslashes, null and control characters, and URL schemes MUST be rejected;
  resolution MUST never read outside the configured templates location.
- **FR-011**: By default, documents MUST NOT load any resource over the network or from the
  filesystem; images, stylesheets and fonts MUST come only from the classpath or Data URIs.
- **FR-012**: The library MUST allow remote resource loading only through an explicit allow-list
  of hosts (and `https` scheme by default when allow-listed); everything not allow-listed MUST be
  refused.
- **FR-013**: XML/HTML parsing MUST disable external entities and external DTD loading.
- **FR-014**: All template variables MUST be HTML-escaped by default; unescaped output MUST be
  possible only through an explicit, documented construct.
- **FR-015**: The template engine MUST run in a strict and safe mode: undefined variables cause
  an error, and templates MUST NOT be able to invoke arbitrary methods or access sensitive Java
  types (class objects, class loaders, system, runtime, threads, reflection, filesystem).
- **FR-016**: The library MUST enforce configurable resource limits with safe defaults: maximum
  template size (default 1 MB), maximum page count (default 500 pages), maximum rendering time
  (default 30 seconds). Exceeding a limit MUST stop the render with a limit-exceeded error.
- **FR-017**: The project MUST include dedicated automated tests for each requirement FR-010 to
  FR-016, FR-031 (file name sanitising) and FR-045.

#### Business formatting and print layout (US3)

- **FR-018**: The `fcfa` filter MUST format a number as an integer amount with a space as
  thousands separator followed by ` FCFA` (e.g. `1 250 000 FCFA`), rounding half-up, regardless
  of the configured locale.
- **FR-019**: The `money` filter MUST format a number with a given currency code (ISO 4217)
  according to the configured locale (French by default), with an optional locale override.
- **FR-020**: The `sum` filter MUST sum a list of numbers, or a given numeric property of a list
  of objects/maps, with exact decimal arithmetic.
- **FR-021**: The `date` filter MUST format date, date-time and legacy date values, using a
  long French format by default (`24 novembre 2026`) and accepting an explicit pattern; month and
  day names MUST follow the configured locale.
- **FR-022**: The default locale MUST be French and MUST be configurable globally and per render.
- **FR-023**: The library MUST embed a free, redistributable font covering Latin characters
  with French diacritics and the Latin characters used in Ewe and other West African languages
  (e.g. Ɖ ɖ, Ɛ ɛ, Ƒ ƒ, Ɣ ɣ, Ŋ ŋ, Ɔ ɔ, Ʋ ʋ, combining tilde), and MUST use it by default so that
  output is identical with or without system fonts.
- **FR-024**: Developers MUST be able to register additional fonts from the classpath.
- **FR-025**: Templates MUST be able to reference images from the classpath (relative to the
  templates location) and as Data URIs.
- **FR-026**: The library MUST apply a default print stylesheet: A4 portrait, default margins,
  "Page X / Y" in the page footer, a `page-break` class forcing a new page, and repetition of
  table header rows on each page. Template CSS MUST be able to override every default.

#### Spring Boot integration (US4)

- **FR-027**: The Spring Boot starter MUST auto-configure a shared DocMint component with zero
  mandatory properties, and MUST back off when the application defines its own.
- **FR-028**: The starter MUST expose configuration properties under the `docMint` prefix for at
  least: templates location, cache (enabled), default locale, additional fonts, resource limits,
  missing-resource mode (fail or warn) and the remote-resource allow-list; properties MUST be
  documented and validated at startup.
- **FR-029**: The starter MUST provide a `@Report` method annotation that renders the returned
  data object with the named template and writes the PDF to the HTTP response.
- **FR-030**: `@Report` MUST allow configuring the file name (default `<template-name>.pdf`) and
  the disposition (inline by default, or attachment); the `Content-Disposition` header MUST
  encode non-ASCII file names correctly.
- **FR-031**: The `@Report` file name MUST support placeholders resolved from the returned data
  object (e.g. `facture-{numero}.pdf`, including nested properties such as `{customer.code}`).
  Resolved values MUST be sanitised before reaching the HTTP header: path separators, control
  characters (including CR/LF) and quotes are removed or replaced, and the final name is length
  bounded. A placeholder naming an unknown property MUST cause a clear error; a property whose
  value is `null` MUST resolve to an empty string. Dedicated tests MUST cover header-injection
  attempts.
- **FR-032**: Rendering errors in the web layer MUST be returned as RFC 9457 Problem Details
  (`application/problem+json`) with the standard members `type`, `title`, `status` and
  `detail` (actionable message), plus an DocMint-specific `code` member holding a stable,
  documented error code (e.g. `DOCMINT_TEMPLATE_NOT_FOUND`). The body MUST never contain a stack
  trace or internal path. The list of error codes is part of the public contract. Template and
  rendering errors (not found, syntax, refused/missing resource, limit exceeded) are developer
  or server errors and MUST map to HTTP 500 with distinct error codes; a client that does not
  accept PDF MUST receive HTTP 406.
- **FR-033**: The starter MUST NOT require any web-specific dependency for the non-web usage of
  the DocMint component (injectable in batch or messaging applications).
- **FR-034**: Resource failures MUST be handled as follows:
  - a **refused** resource (scheme or host not allowed) MUST always fail the render with a
    resource-refused error naming the resource; this cannot be relaxed by configuration;
  - a **missing** resource (allowed source, but not found or unreadable) MUST, by default, fail
    the render with a resource-missing error naming the resource;
  - a configuration setting (core builder and starter property) MUST allow switching missing
    resources to *warn* mode: a warning naming the resource is logged and the document is
    rendered without it.
- **FR-035**: The V1 starter MUST target the Spring Boot 4.x line only. Support for the 3.5.x
  line is out of scope for V1 and may be added in a later version if users request it; the
  starter's design SHOULD NOT preclude it.

#### Errors (US5)

- **FR-036**: All errors MUST be reported through a public exception hierarchy with one root
  type and distinct types for: template not found, template syntax error, render failure (and
  its sub-cases: invalid template name, resource refused/missing, limit exceeded).
- **FR-037**: A template-not-found error MUST list the available templates (bounded list) and
  suggest the closest name when one is similar.
- **FR-038**: A template syntax error MUST give the template name, the line and, when
  available, the column.
- **FR-039**: Error messages MUST NOT contain absolute filesystem paths, stack traces, secret
  values or rendered data values.

#### Observability

- **FR-047**: The library MUST log through the standard logging facade of the host application
  (no logging backend imposed) with these levels: DEBUG for each render (template name,
  duration, page count, cache hit or miss), WARN for missing resources in warn mode and for
  insecure-looking configuration (e.g. a wildcard allow-list), ERROR never emitted by the
  library for exceptions it throws (the application decides). Logs MUST NEVER contain data
  values passed to templates or rendered content. Metrics are out of scope for V1.

#### Demo, documentation and release (US6)

- **FR-040**: The project MUST provide a demo Spring Boot API (layered architecture with
  in-memory database) exposing invoice, receipt and attestation endpoints built from Response
  DTOs, with a shared base layout and a logo, and a demo Angular application with "View" and
  "Download" actions. Demos MUST never be published.
- **FR-041**: The project MUST provide a bill of materials aligning the versions of all
  published DocMint modules.
- **FR-042**: The repository MUST contain all open source files listed in User Story 6,
  scenario 4, including ADRs for every significant technical decision.
- **FR-043**: CI MUST run on Java 17 and 21 and enforce: tests, formatting, static analysis,
  Javadoc on the public API, ≥ 80 % line coverage on the core module, architecture rules,
  CodeQL, dependency vulnerability scanning (failing on critical vulnerabilities) and Dependabot.
- **FR-044**: The release process MUST publish signed core, starter and BOM artifacts with
  sources, Javadoc and complete metadata to Maven Central, only from a green CI, using secrets
  stored exclusively in the CI secret store.
- **FR-045**: The repository MUST contain no secret; an automated check MUST prevent committing
  key material.

#### Scope guard

- **FR-046**: V1 MUST NOT include any feature planned for later versions: custom headers/footers
  API, watermark, QR/barcodes, amount in words, password protection, PNG preview, Angular
  helper library (V2); charts, Excel, CSV (V3); Word, pivot tables, streaming, async generation
  (V4); e-signature, PDF/A, accessibility tagging (V5); server, scheduling, email, visual editor
  (post-1.0); render metrics (not yet scheduled, see FR-047).

### Key Entities

- **Template**: a named HTML/CSS document with placeholders, located under the templates
  location; may extend a layout and include fragments.
- **Layout**: a template that defines the common structure (header, footer, styles) and named
  blocks that child templates override.
- **Template data (model)**: the data object (record, object or map) passed by the developer,
  exposed to the template as named values.
- **Render request**: template name, data, output format, options (locale override, file name).
- **Render response**: generated content, content type, suggested file name, page count.
- **Output format**: the kind of document produced (PDF in V1; extensible).
- **Page settings**: page size, orientation and margins (defaults, overridable by CSS).
- **Resource limits**: maximum template size, page count and rendering time.
- **Resource access policy**: allowed sources for images, CSS and fonts (classpath, Data URI,
  allow-listed hosts).
- **Report endpoint definition**: template name, file name, disposition attached to an API
  method.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer new to the project produces a first PDF in under 10 minutes using only
  the README (verified by at least one person unfamiliar with the project before release).
- **SC-002**: The common case (render a named template with a data object) requires exactly one
  line of application code and zero configuration entries, in plain Java and in Spring Boot.
- **SC-003**: 100 % of the security requirements (FR-010 to FR-016, FR-031, FR-045) are covered by
  dedicated automated tests, and all pass.
- **SC-004**: A typical one- to three-page invoice is generated in under 1 second on a standard
  developer laptop once the template is cached.
- **SC-005**: 50 concurrent renders of mixed templates complete with no error and with outputs
  identical to sequential renders.
- **SC-006**: The same document rendered on a developer machine and in a minimal container image
  without system fonts yields identical extracted text, with zero missing glyphs for French and
  Ewe sample texts.
- **SC-007**: Templates of the demo open and display legibly in a standard web browser without
  any DocMint tooling.
- **SC-008**: For 100 % of the error categories in US5, the message names the cause and the next
  action, and contains no absolute path or stack trace.
- **SC-009**: Line coverage of the core module is at least 80 %, and every public API type and
  method is documented.
- **SC-010**: The 0.1.0 artifacts pass Maven Central validation on the first publication attempt
  and are resolvable by a fresh project.

## Assumptions

- Target users are Java developers, mostly building Spring Boot REST APIs consumed by Angular;
  plain Java usage (batch, CLI) must work equally.
- Java 17 is the minimum runtime; Java 21 is also supported and tested.
- The default templates location is `reports/` on the classpath, matching the demo layout
  (`reports/layouts/base.html`, `reports/facture.html`, `reports/recu.html`,
  `reports/attestation.html`, `reports/images/logo.png`). Demo template names stay in French.
- FCFA formatting is identical for the West African (XOF) and Central African (XAF) francs (both
  are written "FCFA", with no decimals), so a single `fcfa` filter covers both; `money` handles
  any ISO currency code.
- The default French locale is generic French (`fr`); regional variants (e.g. `fr-TG`) are
  configurable and produce the same formatting for the V1 filters.
- Missing resources fail fast by default (FR-034); warn mode is an explicit opt-in. Refused
  resources always fail.
- Undefined variables are errors (strict mode); `null` values of existing properties render as
  empty and are accepted by filters.
- Resource limit defaults (1 MB template, 500 pages, 30 s) are generous enough for business
  documents and can be raised by configuration.
- Templates come from the application's own resources and are written by trusted developers;
  data is untrusted. Loading templates from end-user input (database, upload) is out of scope
  for V1.
- The maintainer holds the Maven Central namespace `io.github.juste120`, a GPG key and the
  publishing token, and stores them as CI secrets; the actual publication is triggered manually
  by the maintainer after full validation.
- A coverage badge may rely on an external service; if no account exists, the README shows
  coverage from the CI report instead (decided in the plan).
- The exact template engine, renderer, font, build plugins and dependency versions are decided
  and justified in the plan and ADRs, within the constitution's technology constraints.
