# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Before 1.0.0,
minor versions may change the public API; every such change is listed here.

## [Unreleased]

## [0.5.0] - Not yet released

### Added

- PDF/A (`PdfStandard`: 2b, 2u, 2a, 3b, 3u, 3a) with sRGB output intent and XMP metadata.
- Accessible, tagged PDFs (PDF/UA); title and language added when the template has none.
- Optional module `docmint-signature`: electronic signature with a PKCS#12 key store (detached
  CMS, SHA-256, certificate chain), compatible with PDF/A.
- `RenderOptions.withPdfStandard/withAccessible/withSignature`, builder defaults, starter
  properties `docmint.pdf.*` and `docmint.signature.*`, `ReportContent` methods.
- `PostProcessor.provides(...)`: a requested signature never silently disappears when the
  module is missing. New code `DOCMINT_SIGNATURE_INVALID`.

### Security

- PDF/A or signature combined with password protection is refused; key store passwords are
  `char[]`, redacted and wipeable.

## [0.4.0] - Not yet released

### Added

- Optional module `docmint-word`: any template as a Word document (`OutputFormat.DOCX`):
  headings, formatting, lists, tables, images, page breaks, header/footer with page numbers.
- `pivot(...)` template function and `PivotTable` (sum, count, avg, min, max, totals).
- `DocMint.render(request, OutputStream)`, `DocMint.exportCsv(Stream, OutputStream, CsvSettings)`
  (row by row), `DocMint.renderAsync(request)` and `DocMintBuilder.executor(...)`.
- `PostProcessor`s are discovered with `ServiceLoader`; HTML output inlines template images.
- Starter: `@Report(format = DOCX)`, streamed CSV for methods returning a `Stream`,
  `CompletableFuture` results.

## [0.3.0] - Not yet released

### Added

- `chart(...)` template function: bar, line, pie and doughnut charts (Java2D, embedded font,
  locale-formatted values, legends, custom colours).
- Output formats `HTML`, `CSV` and `XLSX`, produced from the tables of a template
  (`data-value`, `data-type`, `data-docmint-sheet`, class `docmint-export`).
- CSV export (`;` and UTF-8 BOM by default, `CsvSettings.rfc4180()`), with spreadsheet formula
  injection neutralised.
- Optional module `docmint-excel` (Apache POI): typed cells, bold frozen headers, merged cells,
  auto-sized columns; discovered automatically.
- `@ExcelColumn` and `DocMint.csv(...)` / `DocMint.export(...)` for lists of objects.
- `TableWriter` SPI; `ResourceLimits.maxExportRows`; starter `@Report(format = CSV | XLSX |
  HTML)` and list exports without template; property `docmint.limits.max-export-rows`.

### Fixed

- `loop.index`, `loop.first`, `loop.last`, `loop.length` and `loop.revindex` were refused by the
  template sandbox; their getters are now allowed (and only them).

## [0.2.0] - Not yet released

### Added

- Page headers and footers from HTML: classes `docmint-header` / `docmint-footer`, variants
  `docmint-first-page` / `docmint-not-first-page`, page numbers `docmint-page` / `docmint-pages`.
- Template functions `qr(...)` and `barcode(...)` (Code 128, EAN-13), generated locally with ZXing.
- `words` filter: amounts in words in French (traditional and 1990 spelling) and English, with
  CFA franc, euro and dollar.
- Watermarks (text or image) and AES-256 password protection with permissions, as render
  options; `DocMintBuilder.defaultWatermark`.
- PNG preview: `DocMint.png(...)`, `OutputFormat.PNG`, `PreviewSettings`, image size limit.
- `PostProcessor` SPI (ordered chain after rendering).
- Starter: `ReportContent`, `@Report(format = PNG)`, properties `docmint.watermark.*` and
  `docmint.limits.max-image-pixels`; new error codes `DOCMINT_CODE_INVALID`,
  `DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE`, `DOCMINT_PROTECTION_INVALID`, `DOCMINT_WATERMARK_INVALID`.
- Angular library `@juste120/docmint-angular`.

### Changed

- The default "Page X / Y" footer is replaced when a template defines its own footer
  (templates without one render as in 0.1.0).

## [0.1.0] - Not yet released

### Added

- `docmint-core`: PDF generation from HTML/CSS templates in one line —
  `DocMint.createDefault().pdf(name, data)` and the advanced `render(RenderRequest)` API.
- Pebble templates with variables, loops, conditions, includes and layout inheritance; data from
  records, JavaBeans or maps.
- Filters `fcfa`, `money`, `sum` and `date` (French by default, configurable locale).
- Embedded Noto Sans font (French and West African Latin letters), used as fallback font.
- Default print stylesheet: A4, margins, "Page X / Y", `.page-break`, repeated table headers.
- Images from the class path and `data:` URIs; thread-safe template cache.
- Security by default: template name validation, no remote or file resources unless an HTTPS
  host is allow-listed, XXE-proof HTML parsing, auto-escaping, sandboxed templates, limits on
  template size, rendered size, pages and render time.
- Actionable errors with stable codes (`DOCMINT_TEMPLATE_NOT_FOUND`, …) and name suggestions.
- `docmint-spring-boot-starter` for Spring Boot 4: auto-configured `DocMint` bean, `docmint.*`
  properties, `@Report` annotation with file name placeholders, RFC 9457 error responses.
- `docmint-bom`.
- Demo Spring Boot API and Angular application (not published).

[Unreleased]: https://github.com/Juste120/DocMint/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/Juste120/DocMint/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/Juste120/DocMint/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/Juste120/DocMint/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Juste120/DocMint/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Juste120/DocMint/releases/tag/v0.1.0
