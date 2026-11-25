# Roadmap

DocMint grows one version at a time. Each version gets its own specification
(`specs/`, spec-driven development with Spec Kit) before any code is written. There are no
dates: a version ships when it is complete, tested and documented.

## 0.1.0 — The PDF in one line

Pebble templates (variables, loops, conditions, layouts), `docMint.pdf()`, `@Report` (file name,
inline/attachment), filters `fcfa`, `money`, `sum`, `date`, embedded font, class path and Data
URI images, default print CSS, thread-safe cache, clear errors, security by default, demos,
Maven Central.

## 0.2.0 — Professional documents

Custom headers and footers, watermark, QR codes and barcodes, amount in words, password
protection, PNG preview, Angular helper library.

## 0.3.0 — Charts and Excel

`<chart>` tag rendered as SVG, Excel export (from HTML tables and `@ExcelColumn` annotations),
CSV export.

## 0.4.0 — Word and large volumes

`.docx` export, pivot tables, streaming, asynchronous generation.

## 0.5.0 — Official documents

Electronic signature, PDF/A, accessibility (tagged PDF).

## 1.0.0 — Stable

Frozen API, complete documentation, migration guide from JasperReports.

## After 1.0

`docmint-server` (REST catalogue of reports, scheduling, sending by email) and a visual Angular
editor with live preview.

Ideas outside this list are welcome as
[feature requests](https://github.com/Juste120/DocMint/issues/new/choose).
