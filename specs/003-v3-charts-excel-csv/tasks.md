# Tasks: DocMint V3 — Charts, Excel and CSV

- [ ] T201 Domain: `OutputFormat` HTML/CSV/XLSX, `CsvSettings`, `ResourceLimits.maxExportRows`; API `ExcelColumn`, `RenderOptions.csv`
- [ ] T202 `HtmlDocumentRenderer` (HTML output) registered for `OutputFormat.HTML`
- [ ] T203 [US1] `ChartFunction` + `ChartPainter` (bar, line, pie, doughnut, legend, locale, font, limits) + tests
- [ ] T204 [US2/US3] `TableExtractor` + `TableData` port types + tests
- [ ] T205 [US3] `CsvWriter`, `CsvPostProcessor`, `ColumnModel`, `DocMint.csv(rows)` + tests (RFC 4180, BOM, injection)
- [ ] T206 [US2] Module `docmint-excel`: `XlsxWriter`, `XlsxPostProcessor`, `DocMintExcel.xlsx(rows)`, ServiceLoader registration, tests; BOM entry
- [ ] T207 `DocMintBuilder` ServiceLoader discovery; clear error when XLSX is requested without the module
- [ ] T208 Starter: formats CSV/XLSX/HTML, collection exports without template, tests
- [ ] T209 Demo: sales chart on a statement, Excel/CSV export endpoints, Angular buttons
- [ ] T210 Docs: README, user guide, CHANGELOG 0.3.0, ADR-0014 (charts), ADR-0015 (docmint-excel); version 0.3.0-SNAPSHOT; full verify JDK 17/21
