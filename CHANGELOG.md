# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Before 1.0.0,
minor versions may change the public API; every such change is listed here.

## [Unreleased]

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

[Unreleased]: https://github.com/Juste120/DocMint/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/Juste120/DocMint/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Juste120/DocMint/releases/tag/v0.1.0
