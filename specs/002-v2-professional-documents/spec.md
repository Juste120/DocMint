# Feature Specification: DocMint V2 — Professional Documents

**Feature Branch**: `002-v2-professional-documents`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "V2 (0.2.0) – Professional documents: custom headers and footers,
watermark, QR code and barcode, amount in words, password protection, PNG preview, Angular
helper." (from the project brief and `ROADMAP.md`)

> **Baseline.** Everything in V1 (`specs/001-v1-pdf-generation`) stays valid and backward
> compatible: existing templates, the one-line API, `@Report`, filters, security rules and
> error codes. V2 only adds capabilities. Each addition must keep the common case at one line
> and zero mandatory configuration (constitution, Principle II).

## Actors

Same as V1 (library user, template author, end user, contributor, maintainer), plus:

- **Front-end developer**: an Angular developer consuming DocMint endpoints through the helper
  library.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Headers and footers on every page (Priority: P1)

A template author defines, in plain HTML, a header (company identity, logo) and a footer (legal
mentions, page number) that are repeated on every page of a multi-page document, and can use a
different header or none on the first page.

**Why this priority**: every multi-page invoice or statement needs it; today V1 only offers a
fixed "Page X / Y" footer.

**Independent Test**: render a 3-page document with a header and a footer; extract the text of
each page and check that header and footer appear on every page, with the right page number.

**Acceptance Scenarios**:

1. **Given** a template with an element marked as the page header and one marked as the page
   footer, **When** it renders on 3 pages, **Then** both appear on each page, outside the
   content area, without overlapping the content.
2. **Given** a footer containing the page-number and page-count placeholders, **When** rendered,
   **Then** page 2 shows "2" and "3" in the expected places.
3. **Given** a template that marks a header as "first page only" or "not on first page",
   **When** rendered, **Then** the header appears only where requested.
4. **Given** a template that defines its own footer, **When** rendered, **Then** the V1 default
   "Page X / Y" footer is replaced, not duplicated; templates without a custom footer keep the V1
   default.
5. **Given** a header containing a logo from the templates folder, **When** rendered, **Then** the
   logo appears on every page (same resource rules as V1).

---

### User Story 2 - QR codes and barcodes (Priority: P1)

A template author adds a QR code (e.g. a verification URL or a Mobile Money payment reference)
or a barcode (e.g. an invoice number) to a document with one template expression.

**Why this priority**: verification QR codes on invoices, receipts and attestations are now
expected by customers and administrations; barcodes are needed for logistics and archives.

**Independent Test**: render a template with a QR code and a barcode, extract the images from the
PDF and decode them with a reader library; the decoded content equals the input.

**Acceptance Scenarios**:

1. **Given** `{{ qr("https://verify.example.com/F-2026-001") }}`, **When** rendered, **Then** the
   PDF contains a sharp (vector or high-resolution) QR code that decodes to that URL.
2. **Given** a requested size (e.g. 30 mm) and an error-correction level, **When** rendered,
   **Then** the code has that printed size and level.
3. **Given** `{{ barcode("F-2026-001", "code128") }}` or an EAN-13 number, **When** rendered,
   **Then** the barcode decodes to the input, optionally with the human-readable text below it.
4. **Given** content that is invalid for the format (wrong EAN-13 check digit, text too long for
   a QR code, unsupported format name), **When** rendered, **Then** rendering fails with a clear
   template error naming the problem.
5. **Given** content coming from user data (e.g. a customer name), **When** encoded, **Then** it is
   encoded as data only and can never inject markup into the document.

---

### User Story 3 - Amount in words (Priority: P2)

A template author writes "Arrêtée la présente facture à la somme de
{{ total | words }}" and gets the amount spelled out correctly in French, as required on
invoices in francophone Africa.

**Why this priority**: mandatory wording on many invoices and receipts in UEMOA/CEMAC countries;
error-prone to write by hand.

**Independent Test**: unit-test the spelling of a large sample of amounts (0, units, teens,
70–99, hundreds, thousands, millions, billions, decimals) against a reference list.

**Acceptance Scenarios**:

1. **Given** `1250000` and the French locale, **When** `words` is applied, **Then** the output is
   `un million deux cent cinquante mille`.
2. **Given** a currency argument `"XOF"` (or the `fcfa` shortcut), **When** applied, **Then** the
   output is `un million deux cent cinquante mille francs CFA`; for `"EUR"` with decimals,
   `mille deux cent trente-quatre euros et cinquante centimes`.
3. **Given** the French spelling rules (`vingt et un`, `quatre-vingts`, `quatre-vingt-un`,
   `deux cents`, `deux cent un`, `mille` invariable, `millions` plural), **When** applied,
   **Then** each rule is respected (traditional spelling by default; the 1990 reformed spelling
   with hyphens everywhere is available as an option).
4. **Given** the English locale, **When** applied, **Then** the output is in English
   (`one million two hundred fifty thousand`).
5. **Given** a negative amount, zero, `null` or a value beyond the supported range, **When**
   applied, **Then** the output is respectively `moins …`, `zéro`, empty, or a clear error.

---

### User Story 4 - Watermark (Priority: P2)

A developer marks a document as `BROUILLON`, `COPIE`, `ANNULÉ` or `PAYÉ` (or with an image) on
every page, without changing the template.

**Why this priority**: drafts, duplicates and cancelled invoices must be visually distinct from
originals.

**Independent Test**: render with a watermark option; check that every page contains the
watermark text drawn behind or over the content with the requested opacity, and that the
content text is still extractable.

**Acceptance Scenarios**:

1. **Given** a watermark text option on a render, **When** rendered, **Then** every page shows the
   text diagonally, semi-transparent, centred, without hiding the content.
2. **Given** options for angle, opacity, colour and size, **When** rendered, **Then** they are
   applied; values outside the allowed range are rejected with a clear error.
3. **Given** an image watermark from the templates folder, **When** rendered, **Then** it is
   applied on every page (same resource rules as V1).
4. **Given** a `@Report` endpoint, **When** the watermark is set from the method (e.g. depending on
   the invoice status), **Then** the PDF has it; the template is unchanged.
5. **Given** watermark text from user data, **When** rendered, **Then** it is treated as plain
   text.

---

### User Story 5 - Password protection (Priority: P2)

A developer protects a payslip or a confidential statement with an opening password and/or
restricts printing, copying and modification.

**Why this priority**: personal and financial documents are often sent by email or WhatsApp.

**Independent Test**: render with protection options, then try to open the PDF without password
(fails), with the password (succeeds), and check the declared permissions.

**Acceptance Scenarios**:

1. **Given** an opening (user) password, **When** the PDF is produced, **Then** it cannot be
   opened without the password, and opens with it.
2. **Given** only an owner password and permissions (e.g. printing allowed, copying and
   modification denied), **When** produced, **Then** the PDF opens without a password and declares
   exactly those permissions.
3. **Given** any protection, **When** produced, **Then** strong encryption is used (AES 256-bit);
   weaker algorithms are not offered.
4. **Given** protection options, **When** an error occurs or the render is logged, **Then**
   passwords never appear in logs, exception messages, HTTP errors or the template model.
5. **Given** a `@Report` endpoint, **When** the method provides protection for the response (e.g.
   the employee's ID number as password), **Then** the downloaded PDF is protected.

---

### User Story 6 - PNG preview (Priority: P3)

A developer generates a PNG image of a page of a document (thumbnail in a list, preview before
download, image to send by messaging).

**Why this priority**: useful, but documents are complete without it.

**Independent Test**: render a template as PNG; check the image dimensions match the page size
and resolution, and that the image is not blank.

**Acceptance Scenarios**:

1. **Given** a template, **When** a PNG preview is requested with the one-line API, **Then** the
   first page is returned as a PNG at the default resolution.
2. **Given** a page number and a resolution, **When** requested, **Then** that page is rendered at
   that resolution; a page number beyond the document or a resolution outside the allowed range
   is a clear error.
3. **Given** a `@Report` endpoint asked for an image (e.g. by declaring the PNG format),
   **When** called, **Then** it returns `image/png` with a `.png` file name.
4. **Given** the V1 resource limits, **When** rendering a preview, **Then** the same limits apply,
   plus a maximum image size in pixels.

---

### User Story 7 - Angular helper library (Priority: P3)

A front-end developer installs a small Angular library and views, downloads or previews DocMint
documents in one call, with errors shown properly.

**Why this priority**: removes boilerplate repeated in every project (blob handling, file name
parsing, RFC 9457 errors in blobs), but the V1 demo already shows how to do it by hand.

**Independent Test**: unit-test the service against a mocked HTTP backend: file name parsing
(`filename*` and `filename`), view/download behaviour, and error conversion.

**Acceptance Scenarios**:

1. **Given** the library installed and provided, **When** the developer calls the view method with
   an endpoint URL, **Then** the PDF opens in a new tab; the download method saves it with the
   server's file name.
2. **Given** a non-ASCII file name sent as `filename*=UTF-8''…`, **When** downloaded, **Then** the
   decoded name is used.
3. **Given** an error response (RFC 9457 inside a blob), **When** a call fails, **Then** the
   developer receives a typed error with `code`, `title` and `detail`.
4. **Given** a preview endpoint, **When** the preview method is called, **Then** an image URL is
   returned that can be bound to `<img>` and is released when no longer needed.
5. **Given** the library, **When** a developer reads its README, **Then** installation and usage fit
   on one screen.

---

### Edge Cases

- Header/footer taller than the page margin → content area shrinks or a clear error; never
  overlapping text.
- Header/footer using template variables → evaluated once per document (same data on each page),
  page numbers handled by the page placeholders only.
- QR code with empty content, extremely long content, or non-Latin characters → clear error or
  UTF-8 encoding as documented.
- Barcode in a table cell narrower than the barcode → rendered at minimum readable width or a
  clear error, never silently cropped.
- `words` on non-integer FCFA amounts → rounded half-up to the unit like `fcfa`.
- `words` on amounts ≥ 10^15 → clear error (range documented).
- Watermark with very long text → scaled to fit the page diagonal.
- Password protection combined with watermark → both applied (watermark drawn before encryption).
- Empty password string → treated as no password; protection with no password and no permission
  restriction → error explaining the options.
- PNG preview of a document of 0 pages (empty template) → first (blank) page.
- PNG preview at maximum resolution of a large page size (A3) → bounded by the pixel limit.
- Angular helper used with an endpoint returning JSON instead of PDF → typed error, no broken
  file saved.

## Requirements *(mandatory)*

### Functional Requirements

#### Headers and footers (US1)

- **FR-101**: Template authors MUST be able to mark an HTML element as the page header and
  another as the page footer, repeated on every page, using standard HTML plus DocMint's
  documented class names (no Java code).
- **FR-102**: Headers and footers MUST support page-number and page-count placeholders.
- **FR-103**: Template authors MUST be able to restrict a header/footer to the first page, or to
  all pages except the first.
- **FR-104**: A custom footer MUST replace the V1 default "Page X / Y" footer; templates without a
  custom footer MUST render exactly as in V1.

#### QR codes and barcodes (US2)

- **FR-105**: Templates MUST be able to insert a QR code with one expression, with optional size,
  error-correction level (L, M, Q, H; default M) and quiet zone.
- **FR-106**: Templates MUST be able to insert barcodes in at least Code 128 and EAN-13 formats,
  with optional size and human-readable text.
- **FR-107**: Codes MUST be embedded as vector graphics or at a resolution that prints sharply at
  the requested size, without any network access.
- **FR-108**: Invalid content for a format MUST fail with a template error naming the format and
  the problem.
- **FR-109**: Code content MUST be bounded (QR: format capacity; barcodes: format limits) and
  generation MUST not allow resource exhaustion.

#### Amount in words (US3)

- **FR-110**: A `words` filter MUST spell out integer and decimal amounts in French (default) and
  English, following the locale of the render or an explicit locale argument.
- **FR-111**: The filter MUST accept an optional currency (ISO code, plus `fcfa` as an alias of
  XOF) and then spell the currency unit and subunit names, singular/plural (e.g. `franc CFA` /
  `francs CFA`, `euro` / `euros`, `centime(s)`); currencies without subunits (XOF, XAF) round to
  the unit.
- **FR-112**: French output MUST follow traditional spelling by default and offer the 1990
  reformed spelling as an option.
- **FR-113**: The supported range MUST be at least 0 to 999 999 999 999 999; outside it, a clear
  error.

#### Watermark (US4)

- **FR-114**: Developers MUST be able to add a text or image watermark to every page through the
  render options (Java API) and from `@Report` methods, without changing templates.
- **FR-115**: Watermark angle, opacity, colour, font size and position (behind/over content)
  MUST be configurable, with safe defaults (45°, 15 % opacity, grey, behind content) and
  validated ranges.
- **FR-116**: Watermark text MUST be treated as plain text; image watermarks MUST follow the V1
  resource rules.

#### Password protection (US5)

- **FR-117**: Developers MUST be able to set an opening password, an owner password and
  permissions (print, print high quality, copy, modify, annotate, fill forms, extract for
  accessibility, assemble) per render.
- **FR-118**: Encryption MUST be AES 256-bit; no weaker option is offered.
- **FR-119**: When only permissions are set, an unpredictable owner password MUST be generated
  so restrictions cannot be removed trivially.
- **FR-120**: Passwords MUST never appear in logs, exceptions, HTTP error bodies, `toString()`
  output or the template model; password objects MUST not be serialisable to JSON by accident.

#### PNG preview (US6)

- **FR-121**: The library MUST render a page of a document as PNG through the one-line API (first
  page, default resolution 96 DPI) and the advanced API (page number, resolution 36–300 DPI).
- **FR-122**: `@Report` MUST support producing PNG instead of PDF (`image/png`, `.png` file name).
- **FR-123**: Image size MUST be bounded (maximum pixels per image, configurable, safe default).
- **FR-124**: PNG output MUST be produced by a new renderer/post-processor registered for its
  format, without changing the V1 rendering pipeline (Principle V).

#### Angular helper (US7)

- **FR-125**: The project MUST provide an Angular library with a service to view, download and
  preview documents from DocMint endpoints, parsing `Content-Disposition` (`filename*` first) and
  converting RFC 9457 errors (including when returned as `Blob`) into typed errors.
- **FR-126**: The library MUST support the current Angular major version, have no runtime
  dependency other than Angular and RxJS, be tested, and be published to npm under the name
  `@juste120/docmint-angular` with the same version number as the Java artifacts.
- **FR-127**: The demo Angular application MUST use the library instead of its hand-written
  service.

#### Cross-cutting

- **FR-128**: Every new feature MUST be available in plain Java and in the Spring Boot starter,
  with properties for global defaults where relevant (`docmint.watermark.*`,
  `docmint.preview.*`), all optional.
- **FR-129**: New errors MUST use the V1 exception hierarchy with new stable codes (e.g.
  `DOCMINT_CODE_INVALID`, `DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE`, `DOCMINT_PROTECTION_INVALID`).
- **FR-130**: Each new feature MUST be implemented as an adapter or post-processor plugged through
  ports (template functions, `PostProcessor` port, renderer per format), keeping the V1 core
  unchanged except for extension points (Principle V).
- **FR-131**: Every new security-relevant behaviour (code content, watermark text, passwords,
  image limits) MUST have dedicated tests.
- **FR-132**: V2 MUST NOT include features of later versions: charts, Excel, CSV (V3); Word,
  pivot tables, streaming, async (V4); electronic signature, PDF/A, accessibility (V5); server,
  scheduling, email, visual editor (post-1.0).

### Key Entities

- **Page header / footer**: HTML fragments of a template repeated on pages, with page scope (all,
  first only, all but first).
- **Code**: content, type (QR, Code 128, EAN-13), size, error-correction level, human-readable
  flag.
- **Watermark**: text or image, angle, opacity, colour, font size, layer (behind/over).
- **Protection**: opening password, owner password, permissions set (secrets, never logged).
- **Preview request**: page number, resolution, image format (PNG).
- **Post-processor**: a step applied to the rendered PDF (watermark, protection), in a defined
  order.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-101**: 100 % of generated QR codes and barcodes in the test suite decode to their input
  with a standard reader.
- **SC-102**: The `words` filter matches a reference list of at least 200 French and 100 English
  amounts with 0 errors, including all French spelling special cases.
- **SC-103**: A protected PDF cannot be opened without its password by a standard PDF library in
  100 % of tests; permissions read back equal the requested ones.
- **SC-104**: Adding a header/footer, QR code, amount in words or watermark to an existing V1
  template takes at most one line of template or Java code each.
- **SC-105**: All V1 tests still pass unchanged (backward compatibility).
- **SC-106**: A PNG preview of a one-page invoice is produced in under 1 second at 96 DPI once
  the template is cached.
- **SC-107**: A front-end developer can view and download a document with the Angular library in
  under 10 lines of code, following its README only.
- **SC-108**: Passwords never appear in any log line or error message produced during the full
  test suite (automated check).

## Assumptions

- Headers/footers rely on CSS paged media running elements supported by the V1 rendering engine;
  if a needed feature is missing, the plan proposes the closest standard alternative.
- QR codes and barcodes are generated locally by a library with an Apache-2.0-compatible licence
  (chosen in the plan); no online service is ever called.
- Supported code formats in V2: QR, Code 128, EAN-13. Others (DataMatrix, PDF417…) can be added
  later as new formats.
- Amount in words: French (traditional spelling by default, 1990 spelling optional) and English;
  other languages later.
- Watermarks and protection are per-render options decided by Java code, never by templates
  (templates stay data-only and passwords never enter the template model).
- Protection order: watermark first, then encryption.
- PNG preview defaults: first page, 96 DPI, max 300 DPI, max 25 megapixels per image.
- The Angular library is published to npm under `@juste120/docmint-angular` (scope matching the
  maintainer's account), versioned like the Java artifacts; publication is manual, like Maven
  Central. The maintainer creates the npm account/scope.
- V1 public API stays source-compatible; V2 additions are new methods, options and properties.
