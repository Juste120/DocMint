# Tasks: DocMint V3 — Charts, Excel and CSV

- [x] T201 Domain: `OutputFormat` HTML/CSV/XLSX, `CsvSettings`, `ResourceLimits.maxExportRows`; API `ExcelColumn`, `RenderOptions.csv`
- [x] T202 `HtmlDocumentRenderer` (HTML output) registered for `OutputFormat.HTML`
- [x] T203 [US1] `ChartFunction` + `ChartPainter` (bar, line, pie, doughnut, legend, locale, font, limits) + tests
- [x] T204 [US2/US3] `TableExtractor` + `TableData` port types + tests
- [x] T205 [US3] `CsvWriter`, `CsvPostProcessor`, `ColumnModel`, `DocMint.csv(rows)` + tests (RFC 4180, BOM, injection)
- [x] T206 [US2] Module `docmint-excel`: `XlsxWriter`, `XlsxPostProcessor`, `DocMintExcel.xlsx(rows)`, ServiceLoader registration, tests; BOM entry
- [x] T207 `DocMintBuilder` ServiceLoader discovery; clear error when XLSX is requested without the module
- [x] T208 Starter: formats CSV/XLSX/HTML, collection exports without template, tests
- [x] T209 Demo: sales chart on a statement, Excel/CSV export endpoints, Angular buttons
- [x] T210 Docs: README, user guide, CHANGELOG 0.3.0, ADR-0014 (charts), ADR-0015 (docmint-excel); version 0.3.0-SNAPSHOT; full verify JDK 17/21
