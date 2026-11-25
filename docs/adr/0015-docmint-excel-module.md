# 0015. Optional `docmint-excel` module and `TableWriter` SPI

- Status: Accepted
- Date: 2026-11-25

## Context

Excel export needs Apache POI 6 (Apache 2.0) and its dependencies (~12 MB). Most users only need
PDF. CSV needs no dependency.

## Decision

- New formats `HTML`, `CSV`, `XLSX`; `CSV` and `XLSX` are derived from `HTML`: a
  `TablePostProcessor` extracts the template's tables (jsoup) and hands them to a `TableWriter`.
- `TableWriter` is a public SPI discovered with `ServiceLoader`; the core provides CSV, the new
  module `docmint-excel` provides XLSX (`META-INF/services`). Adding the dependency is enough.
- `@ExcelColumn` lives in the core API (no POI needed) and drives list exports for both formats.
- Spreadsheet formula injection is neutralised in CSV; Excel cells are always values.

## Consequences

Users who do not need Excel do not download POI. The BOM aligns `docmint-excel` with the other
modules. Without the module, requesting XLSX fails with a message naming the dependency.
