# DocMint user guide

This guide covers DocMint 0.1.0 to 0.5.0. For a quick start, read the [README](../README.md)
first.

- [1. Concepts](#1-concepts)
- [2. Templates](#2-templates)
- [3. Data](#3-data)
- [4. Filters](#4-filters)
- [5. Layout and printing](#5-layout-and-printing)
- [6. Images, stylesheets and fonts](#6-images-stylesheets-and-fonts)
- [7. Java API](#7-java-api)
- [8. Spring Boot](#8-spring-boot)
- [9. Errors](#9-errors)
- [10. Security and limits](#10-security-and-limits)
- [11. Logging](#11-logging)
- [12. Professional documents (0.2.0)](#12-professional-documents-020)
- [13. Charts, Excel and CSV (0.3.0)](#13-charts-excel-and-csv-030)
- [14. Word, pivot tables, streaming and async (0.4.0)](#14-word-pivot-tables-streaming-and-async-040)
- [15. Official documents: PDF/A, accessibility, signature (0.5.0)](#15-official-documents-pdfa-accessibility-signature-050)

## 1. Concepts

A **template** is an HTML file in the templates folder (class path `reports/` by default).
DocMint fills it with your **data** using the [Pebble](https://pebbletemplates.io) template
language, then converts the HTML to **PDF** with print rules (pages, margins, page numbers).

```
reports/invoice.html  +  InvoiceResponse  ──►  HTML  ──►  PDF
```

The template name is the path relative to the templates folder, without `.html`:
`reports/invoice.html` → `invoice`, `reports/hr/attestation.html` → `hr/attestation`.

## 2. Templates

Templates are standard HTML5 — you can open them in a browser. Pebble syntax:

| Syntax | Purpose |
|---|---|
| `{{ customer.name }}` | Print a value (HTML-escaped) |
| `{% for line in lines %} … {% endfor %}` | Loop; `loop.index`, `loop.first`, `loop.last` are available |
| `{% if discount is not null %} … {% else %} … {% endif %}` | Condition |
| `{{ note \| default("—") }}` | Value when missing or null |
| `{% extends "layouts/base" %}` + `{% block content %}` | Layout inheritance |
| `{% include "parts/signature" %}` | Include another template |
| `{# comment #}` | Comment |

**Layouts**: put the common structure (header, footer, styles) in `reports/layouts/base.html`
with `{% block %}` placeholders; child templates `{% extends "layouts/base" %}` and override the
blocks. Names in `extends`/`include` are always relative to the templates root.

Strict mode: using a variable that does not exist is an error (`DOCMINT_TEMPLATE_EVALUATION`), so
typos are caught immediately. A property that exists but is `null` prints nothing.

## 3. Data

`docMint.pdf("invoice", data)` accepts:

- a **record** (`record Invoice(String number, …)`) — components become variables;
- an **object with getters** (`getNumber()`, `isPaid()`) — properties become variables;
- a **`Map<String, ?>`** — keys become variables.

Nested objects, lists, arrays and maps are converted the same way. Values reach the template as
`String`, numbers (`BigDecimal`, `Integer`, …), `Boolean`, `java.time` dates or `null`; enums
become their name. A root that is a list or a simple value is available as `data`:
`docMint.pdf("list", List.of(…))` → `{% for item in data %}`.

Templates **cannot call methods** on your objects. Prepare the values in a response DTO
(totals, labels, formatted fields). Never pass JPA entities: lazy associations and
back-references (cycles) are rejected.

## 4. Filters

### `fcfa`

`{{ amount | fcfa }}` → `1 250 000 FCFA`. Rounded half-up to the unit (the CFA franc has no
decimals), no-break spaces between thousands, same output for every locale. Works for XOF
(UEMOA) and XAF (CEMAC).

### `money(currency, [locale])`

`{{ price | money("EUR") }}` → `1 234,50 €` in French; `money("USD", "en-US")` → `$1,234.50`;
`money("XOF")` → `1 250 000 F CFA` (CLDR notation). The currency is an ISO 4217 code.

### `sum([property])`

`{{ lines | sum("amount") }}` sums a property of each item with exact decimal arithmetic
(`0.1 + 0.2 = 0.3`). Nested properties: `sum("price.total")`. Without argument, sums a list of
numbers. Null items and values are ignored; an empty list gives `0`. Combine with other filters:
`{{ lines | sum("amount") | fcfa }}`.

### `date([pattern], [locale])`

`{{ issuedOn | date }}` → `24 novembre 2026` (long format of the locale).
`date("dd/MM/yyyy")` → `24/11/2026`; `date("dd/MM/yyyy 'à' HH:mm")` for date-times;
`date("d MMMM yyyy", "en")` → `24 November 2026`. Accepts `LocalDate`, `LocalDateTime`,
`OffsetDateTime`, `ZonedDateTime`, `Instant` (UTC), `java.util.Date` and ISO-8601 strings.
Patterns follow `java.time.format.DateTimeFormatter`.

### `raw`

`{{ html | raw }}` prints a value **without** HTML escaping. Use it only for content you fully
control, never for user input.

## 5. Layout and printing

DocMint inserts a default print stylesheet **before** your styles, so you can override anything:

| Default | How to change it |
|---|---|
| A4 portrait, margins 20 mm / 15 mm | `@page { size: A4 landscape; margin: 10mm; }` or `DocMintBuilder.pageSettings(…)` |
| "Page X / Y" at the bottom centre | `@page { @bottom-center { content: none; } }` or your own `content` |
| `Noto Sans` 10 pt | `body { font-family: …; font-size: …; }` |
| Table headers repeated on each page | `<thead>` rows repeat automatically |
| Rows not split across pages | `tr { page-break-inside: auto; }` to allow it |

Force a new page with `<div class="page-break"></div>`. All CSS paged-media features supported
by OpenHTMLtoPDF are available (`@page :first`, margin boxes, `counter(page)`,
`counter(pages)`).

## 6. Images, stylesheets and fonts

| Reference | Resolved to |
|---|---|
| `images/logo.png` | relative to the template's folder, e.g. `reports/images/logo.png` |
| `/images/logo.png` | from the templates root: `reports/images/logo.png` |
| `data:image/png;base64,…` | embedded image |
| `https://cdn.example.com/x.png` | only if `cdn.example.com` is allow-listed |
| `http://…`, `file:…`, `../../x` | always refused |

The same rules apply to `<link rel="stylesheet">`, CSS `url(…)` and `@import`.

**Fonts.** Noto Sans (regular, bold, italic) is embedded and used for `Noto Sans`,
`sans-serif`, `serif`, `monospace` and any family that is not available (e.g. `Arial` on a Linux
server). It covers French and the Latin letters of Ewe and many West African languages. Add
other TrueType fonts from the class path:

```java
DocMint.builder()
    .font("Inter", "fonts/Inter-Regular.ttf")
    .font(new FontDefinition("Inter", "fonts/Inter-Bold.ttf", FontDefinition.BOLD, false))
    .build();
```

```yaml
docmint:
  fonts:
    - family: Inter
      location: fonts/Inter-Regular.ttf
    - family: Inter
      location: fonts/Inter-Bold.ttf
      weight: 700
```

## 7. Java API

```java
DocMint docMint = DocMint.builder()
    .templatesPath("reports")                       // class path folder
    .locale(Locale.FRENCH)                          // dates and amounts
    .cache(true)                                    // compiled template cache
    .limits(ResourceLimits.defaults().withMaxPages(100))
    .allowRemoteHost("cdn.example.com")             // https only
    .missingResources(MissingResourceMode.FAIL)     // or WARN
    .pageSettings(PageSettings.defaults())
    .build();                                       // immutable, thread-safe: share it

byte[] pdf = docMint.pdf("invoice", data);
byte[] english = docMint.pdf("invoice", data, Locale.ENGLISH);

RenderResponse response = docMint.render(
    RenderRequest.of("invoice", data).withOptions(RenderOptions.defaults().withFileName("F-1.pdf")));
response.content(); response.fileName(); response.pageCount(); response.mediaType();

docMint.availableTemplates();   // ["invoice", "layouts/base", …]
docMint.clearCache();
```

**Extension points** (experts): implement `TemplateResolver` to read templates from elsewhere,
or `DocumentRenderer` to replace the PDF renderer, and register them with
`templateResolver(…)` / `renderer(…)`. Your resolver must never read outside its own root.

## 8. Spring Boot

The starter auto-configures an `DocMint` bean from `docmint.*` properties (see the
[README](../README.md#configuration-all-optional)). Define your own `DocMint` bean to take full
control; the starter then backs off.

### `@Report`

```java
@GetMapping("/{id}/pdf")
@Report(value = "invoice", filename = "invoice-{number}-{customer.code}.pdf",
        disposition = Disposition.ATTACHMENT)
public InvoiceResponse pdf(@PathVariable Long id) { … }
```

- `value` (or `template`): the template name.
- `filename`: default `<template>.pdf`. `{property}` placeholders are read from the returned
  object; values are sanitised (no `/`, `\`, quotes, `;`, line breaks) and `.pdf` is added.
- `disposition`: `INLINE` (default, the browser displays it) or `ATTACHMENT` (download).

The method must return a data object (record, bean, map, `Optional`), not `byte[]` or
`ResponseEntity`. Returning `null` is an error: throw your own 404 exception instead.

The response has `Content-Type: application/pdf`, `Content-Length`, and
`Content-Disposition: inline; filename="…"; filename*=UTF-8''…`. Clients whose `Accept` header
excludes PDF get `406`.

### CORS

Browsers hide `Content-Disposition` from JavaScript on cross-origin calls unless it is exposed:

```java
registry.addMapping("/api/**").allowedOrigins("http://localhost:4200")
    .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION);
```

## 9. Errors

Every error is an unchecked `DocMintException` with a stable `code()`:

<a id="error-docmint-template-not-found"></a>
<a id="error-docmint-template-syntax"></a>
<a id="error-docmint-template-evaluation"></a>
<a id="error-docmint-invalid-template-name"></a>
<a id="error-docmint-resource-refused"></a>
<a id="error-docmint-resource-missing"></a>
<a id="error-docmint-limit-exceeded"></a>
<a id="error-docmint-render-failed"></a>
<a id="error-docmint-invalid-report-method"></a>
<a id="error-docmint-not-acceptable"></a>

| Code | Exception | Typical cause and fix |
|---|---|---|
| `DOCMINT_TEMPLATE_NOT_FOUND` | `TemplateNotFoundException` | Wrong name or folder. The message lists available templates and suggests the closest one. |
| `DOCMINT_TEMPLATE_SYNTAX` | `TemplateSyntaxException` | Unclosed tag or bad expression; message gives template and line. |
| `DOCMINT_TEMPLATE_EVALUATION` | `RenderException` | Undefined variable, wrong filter input. |
| `DOCMINT_INVALID_TEMPLATE_NAME` | `InvalidTemplateNameException` | `../`, absolute path, forbidden characters. |
| `DOCMINT_RESOURCE_REFUSED` | `ResourceRefusedException` | Remote/file resource not allowed. |
| `DOCMINT_RESOURCE_MISSING` | `ResourceMissingException` | Image/CSS not found or unreadable (switch to `warn` to ignore). |
| `DOCMINT_LIMIT_EXCEEDED` | `LimitExceededException` | Template size, rendered size, pages or time limit; message names the setting. |
| `DOCMINT_RENDER_FAILED` | `RenderException` | Other failure (e.g. `@Report` method returned null). |
| `DOCMINT_INVALID_REPORT_METHOD` | `InvalidReportMethodException` | `@Report` misuse (no template, wrong return type, unknown placeholder). |
| `DOCMINT_NOT_ACCEPTABLE` | `ReportNotAcceptableException` | Client does not accept `application/pdf` (HTTP 406). |

In Spring MVC, these errors become RFC 9457 responses:

```json
{
  "type": "https://github.com/Juste120/DocMint/blob/main/docs/user-guide.md#error-docmint-template-not-found",
  "title": "Template not found",
  "status": 500,
  "detail": "Template 'invoce' not found in class path folder 'reports'. Available templates: invoice. Did you mean 'invoice'?",
  "instance": "/api/invoices/1/pdf",
  "code": "DOCMINT_TEMPLATE_NOT_FOUND"
}
```

Messages never contain stack traces, absolute paths or your data. Disable with
`docmint.web.problem-details.enabled=false` to handle errors yourself.

## 10. Security and limits

DocMint is safe by default; see [SECURITY.md](../SECURITY.md) for the full list. Limits:

| Limit | Default | Builder | Property |
|---|---|---|---|
| Template file size | 1 MiB | `ResourceLimits.withMaxTemplateBytes` | `docmint.limits.max-template-size` |
| Rendered HTML size | 8 × template size | (derived) | (derived) |
| Pages | 500 | `withMaxPages` | `docmint.limits.max-pages` |
| Render time | 30 s | `withMaxRenderTime` | `docmint.limits.max-render-time` |

A render that exceeds the time limit returns an error to the caller immediately; the JVM cannot
force the rendering thread to stop, but the page and size limits bound the remaining work.

Templates are trusted code: keep them in your application's resources, never load templates
written by end users.

## 11. Logging

DocMint logs through SLF4J (no backend imposed):

- `DEBUG io.github.juste120.docmint` — one line per render (template, format, duration, pages).
- `WARN` — missing resources in `warn` mode, wildcard remote hosts, DocMint errors in `@Report`
  endpoints.
- `io.github.juste120.docmint.pdf.engine` — OpenHTMLtoPDF internals at DEBUG/TRACE.

Data values and rendered content are never logged.

## 12. Professional documents (0.2.0)

### Headers and footers

Mark elements anywhere in the template (they are moved to the page margins):

```html
<div class="docmint-header"><img src="/images/logo.png" height="40"> ACME SARL</div>
<div class="docmint-footer">ACME SARL · Page <span class="docmint-page"></span> / <span class="docmint-pages"></span></div>
```

| Class | Effect |
|---|---|
| `docmint-header` / `docmint-footer` | repeated at the top / bottom of every page |
| + `docmint-first-page` | only on page 1 |
| + `docmint-not-first-page` | on every page except page 1 |
| `docmint-page` / `docmint-pages` | current page number / page count |

A custom footer replaces the default "Page X / Y". Leave enough margin for them:
`@page { margin-top: 35mm; margin-bottom: 25mm; }`.

### QR codes and barcodes

```
{{ qr("https://verify.example.com/" ~ number) }}           size 30mm, level M
{{ qr(url, size="25mm", level="H") }}                      level L | M | Q | H
{{ barcode(number) }}                                      Code 128
{{ barcode("5901234123457", format="ean13", text=true) }}  EAN-13 with its digits
```

Codes are generated locally (ZXing). Invalid content (wrong EAN-13 check digit, too long) fails
with `DOCMINT_CODE_INVALID`.

### Amount in words

`{{ total | words("XOF") }}` → `un million deux cent cinquante mille francs CFA`. Currencies:
`XOF`, `XAF`, `fcfa`, `EUR` (`… euros et cinquante centimes`), `USD`; no currency →
`mille deux cent trente-quatre virgule cinq`. English with `words("EUR", "en")`; 1990 French
spelling with `words("XOF", spelling="1990")` (`deux-cent-cinquante-mille`). Up to
999 999 999 999 999.

### Watermark, protection and preview (Java options)

```java
RenderOptions options = RenderOptions.defaults()
    .withWatermark(Watermark.text("COPIE").withColor("#C00000").withOpacity(0.2))
    .withProtection(Protection.openPassword(password).withPermissions(Permission.PRINT));
docMint.render(RenderRequest.of("payslip", data).withOptions(options));

byte[] thumbnail = docMint.png("invoice", data);                       // page 1, 96 DPI
docMint.render(new RenderRequest("invoice", data, OutputFormat.PNG,
    RenderOptions.defaults().withPreview(new PreviewSettings(2, 150))));
```

- Watermark: text or image (`Watermark.image("images/stamp.png")`), angle −90…90, opacity
  0.05…1, `#RRGGBB` colour, behind (default) or over the content. A default watermark can be set
  with `DocMintBuilder.defaultWatermark(...)` or `docmint.watermark.text`.
- Protection: AES-256 only. `openPassword(...)` requires a password to open;
  `permissions(...)` restricts printing/copying/modification with a random owner password.
  Passwords are `char[]`, never logged; call `protection.clear()` when done.
- Preview: PNG of one page, 36–300 DPI, at most `maxImagePixels` (25 megapixels by default).

In Spring, return `ReportContent` from a `@Report` method to set them per request, and use
`@Report(format = OutputFormat.PNG)` for preview endpoints.

### Angular

See [`@juste120/docmint-angular`](../docmint-angular/projects/docmint-angular/README.md):
`view`, `download`, `preview` and `DocMintError`.

## 13. Charts, Excel and CSV (0.3.0)

### Charts

```
{{ chart(type="bar", labels=months, values=sales, title="Ventes 2026") }}
{{ chart(type="line", labels=months, series=[{"name": "Lomé", "values": lome}, {"name": "Kara", "values": kara}]) }}
{{ chart(type="pie", labels=["Mobile Money", "Espèces"], values=[58, 42], width="120mm", height="60mm") }}
```

Types `bar`, `line`, `pie`, `doughnut`; `width`/`height` in mm, cm, in, pt or px (default
160mm × 80mm); `colors=["#006A4E", …]`. Values are formatted with the render locale. Charts are
drawn at about 288 DPI and embedded as images (no network, no JavaScript). Limits: 10 series,
1000 points; a pie cannot have negative values.

### Exporting a template to Excel or CSV

The same template can be rendered as PDF, `XLSX` or `CSV`: its tables become sheets/rows.

```html
<table class="docmint-export" data-docmint-sheet="Ventes">
  <thead><tr><th>Mois</th><th>Montant</th><th>Date</th></tr></thead>
  <tbody><tr>
    <td>Janvier</td>
    <td data-value="1250000">{{ 1250000 | fcfa }}</td>          <!-- number in Excel -->
    <td data-type="date" data-value="2026-01-31">31/01/2026</td> <!-- date in Excel -->
  </tr></tbody>
</table>
```

- Only tables with class `docmint-export` are exported when there is one; otherwise all tables.
- Sheet name: `data-docmint-sheet`, else the `<caption>`, else `Sheet1`…
- `data-value` gives the raw value; `data-type` is `number`, `date` or `text`; plain numbers are
  detected. `colspan` becomes merged cells in Excel.
- CSV uses the first exported table.

### Exporting a list

```java
record InvoiceRow(
    @ExcelColumn(name = "N°", order = 1) String number,
    @ExcelColumn(name = "Montant", order = 2, format = "#,##0") BigDecimal amount) {}

byte[] csv = docMint.csv(rows);
RenderResponse xlsx = docMint.export(rows, OutputFormat.XLSX, RenderOptions.defaults());
```

Without annotations, every property is exported (records: declaration order; other classes:
alphabetical order). Maps export their keys.

### CSV dialect and security

Default: `;` separator and UTF-8 BOM, so Excel in French opens columns and accents correctly.
`RenderOptions.withCsv(CsvSettings.rfc4180())` gives `,` without BOM. Values starting with
`=`, `+`, `-`, `@`, tab or CR are prefixed with `'` so that spreadsheets never run them as
formulas; Excel cells are always written as values.

### Excel module

```xml
<dependency>
  <groupId>io.github.juste120</groupId>
  <artifactId>docmint-excel</artifactId>
  <version>0.3.0</version>
</dependency>
```

It is discovered automatically. Without it, asking for `XLSX` fails with a message naming the
dependency. Exports are limited to `maxExportRows` (100 000 by default).

### Spring

```java
@Report(value = "sales", format = OutputFormat.XLSX)           // template tables → workbook
@Report(format = OutputFormat.CSV, filename = "clients.csv")   // method returns a List: no template
```

## 14. Word, pivot tables, streaming and async (0.4.0)

### Word

Add `io.github.juste120:docmint-word`, then render any template as `OutputFormat.DOCX` (or
`@Report(format = OutputFormat.DOCX)`). Converted: headings, paragraphs, bold/italic/underline,
line breaks, lists, tables (bold header rows, `colspan`), images (logos and template images,
QR codes, charts), `.page-break`, and `docmint-header` / `docmint-footer` with page numbers.
CSS layout (positions, colours, widths) is not reproduced: Word documents are meant to be edited.
Data is always inserted as text: no macros, no fields built from data.

### Pivot tables

```html
{% set p = pivot(sales, rows="city", columns="month", value="amount", aggregate="sum", sort=true) %}
<table>
  <tr><th></th>{% for c in p.columnKeys %}<th>{{ c }}</th>{% endfor %}<th>Total</th></tr>
  {% for r in p.rowKeys %}{% set i = loop.index %}
  <tr><td>{{ r }}</td>{% for c in p.columnKeys %}<td>{{ p.cells[i][loop.index] | default("-") }}</td>{% endfor %}<td>{{ p.rowTotals[i] }}</td></tr>
  {% endfor %}
  <tr><td>Total</td>{% for t in p.columnTotals %}<td>{{ t }}</td>{% endfor %}<td>{{ p.total }}</td></tr>
</table>
```

Aggregates: `sum`, `count`, `avg` (2 decimals), `min`, `max`. Keys keep their order of
appearance unless `sort=true`. Limits: 10 000 input rows, 500 row keys and 500 column keys. In
Java: `PivotTable.compute(items, rowKey, columnKey, value, aggregate, sorted)`.

### Streaming

```java
docMint.render(request, outputStream);                               // write a document to a stream
long rows = docMint.exportCsv(repository.streamAll(), out, null);     // CSV row by row
```

`exportCsv` reads the rows while writing, so memory does not depend on the number of rows (the
row limit does not apply). In Spring, a `@Report(format = CSV)` method returning a
`java.util.stream.Stream` is streamed to the response.

### Asynchronous rendering

```java
docMint.renderAsync(RenderRequest.of("statement", data))
    .thenAccept(document -> mailer.send(document.content()));
```

The executor defaults to a pool of daemon threads (one per processor); set your own with
`DocMintBuilder.executor(...)`. In Spring, a `@Report` method may return a `CompletableFuture`.

## 15. Official documents: PDF/A, accessibility, signature (0.5.0)

### PDF/A

```java
docMint.render(RenderRequest.of("invoice", data)
    .withOptions(RenderOptions.defaults().withPdfStandard(PdfStandard.PDF_A_3B)));
DocMint.builder().pdfStandard(PdfStandard.PDF_A_3B).build();      // every PDF
```

Levels: `PDF_A_2B`, `PDF_A_2U`, `PDF_A_2A`, `PDF_A_3B` (recommended for invoices and archives),
`PDF_A_3U`, `PDF_A_3A`. DocMint embeds the fonts, adds an sRGB output intent and the XMP
metadata (`pdfaid`), and uses the template's `<title>` (or its name) as document title.
PDF/A forbids encryption: combining it with password protection is refused.

### Accessibility (PDF/UA)

`withAccessible(true)` or `DocMintBuilder.accessible(true)` produces a tagged PDF with its
language (the template's `lang`, or the render locale) and title, so screen readers can read
it. Give images an `alt` text (QR codes and charts have one).

### Electronic signature

Add `io.github.juste120:docmint-signature`, then:

```java
SignatureSettings signature =
    SignatureSettings.pkcs12(Files.readAllBytes(Path.of("company.p12")), password)
        .withReason("Attestation officielle").withLocation("Lomé, Togo");
docMint.render(RenderRequest.of("attestation", data)
    .withOptions(RenderOptions.defaults().withSignature(signature)));
```

or with Spring properties (`docmint.signature.key-store`, `.password`, `.alias`, `.reason`,
`.location`, `.contact`). The signature is a detached CMS (SHA-256) covering the whole file, with
the certificate chain; any later modification invalidates it. It works with PDF/A. Use a
certificate from a certification authority recognised by your readers; keep the key store and
its password out of the repository (environment variables, secret store). A signed document
cannot also be password-protected by DocMint. Timestamping (TSA) is not included yet.
