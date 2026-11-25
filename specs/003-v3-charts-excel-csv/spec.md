# Feature Specification: DocMint V3 — Charts, Excel and CSV

**Feature Branch**: `003-v3-charts-excel-csv`
**Created**: 2026-11-25
**Status**: Draft
**Input**: "V3 (0.3.0) – Charts and Excel: `<chart>` tag (SVG), Excel export (from HTML tables +
`@ExcelColumn` annotations), CSV export." (project brief, `ROADMAP.md`)

> **Baseline**: V1 and V2 stay backward compatible (all their tests pass unchanged).

## Clarifications

### Session 2026-11-25

Decided by the assistant at the maintainer's request (recommended defaults; can be changed
before release):

- Charts are inserted with a template function `chart(...)` (the "tag"); types bar, line, pie
  and doughnut; rendered locally as high-resolution images (see plan for SVG vs raster).
- Excel export lives in an optional module `docmint-excel` (Apache POI is large); CSV is in the
  core.
- Default CSV dialect: `;` separator and UTF-8 BOM, so that Excel in French opens accents and
  columns correctly; both configurable.
- Spreadsheet formula injection is neutralised in CSV (cells starting with `=`, `+`, `-`, `@`,
  tab, CR are prefixed with `'`); Excel cells are written as values, never as formulas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Charts in documents (Priority: P1)

A template author adds a bar, line, pie or doughnut chart to a statement or report with one
expression, from data in the model.

**Independent Test**: render a template with each chart type; the PDF contains an image of the
requested size; invalid arguments fail with a clear error.

**Acceptance Scenarios**:
1. **Given** `{{ chart(type="bar", labels=months, values=sales, title="Ventes 2026") }}`,
   **When** rendered, **Then** the PDF shows a bar chart with title, axis labels, values formatted
   with the render locale and the embedded font (accents OK).
2. **Given** several series (`series=[{"name": "Lomé", "values": [...]}, …]`), **When**
   rendered, **Then** each series has its own colour and a legend.
3. **Given** `type="pie"` or `"doughnut"`, **When** rendered, **Then** slices are proportional,
   with a legend and percentages.
4. **Given** an unknown type, mismatched lengths, negative values in a pie, more than 1000
   points or 10 series, or an invalid size, **When** rendered, **Then** a clear template error.

### User Story 2 - Excel export (Priority: P1)

A developer exports data to Excel (.xlsx), either from the tables of an existing template or
from a list of annotated objects.

**Independent Test**: export a template and a list; reopen the workbook and check sheets,
headers, cell types and values.

**Acceptance Scenarios**:
1. **Given** a template with HTML tables and the `docmint-excel` module, **When** rendered as
   XLSX, **Then** each table becomes a sheet (name from `data-docmint-sheet` or the caption, or
   `Sheet1…`), header rows are bold and frozen, numbers and dates are real Excel numbers/dates
   (from `data-value` / `data-type` attributes or detected), columns are auto-sized.
2. **Given** `List<InvoiceRow>` whose fields carry `@ExcelColumn(name = "Montant", order = 2,
   format = "#,##0")`, **When** exported, **Then** one sheet with those columns in that order,
   typed values and the number format.
3. **Given** a `@Report(format = XLSX)` method, **When** called, **Then** the response is the
   workbook with the correct media type and a `.xlsx` file name.
4. **Given** text starting with `=`, **When** exported to Excel, **Then** it is stored as text,
   never evaluated as a formula.
5. **Given** XLSX requested without the `docmint-excel` module, **When** rendering, **Then** a
   clear error explains which dependency to add.

### User Story 3 - CSV export (Priority: P2)

A developer exports a table or a list of annotated objects as CSV, readable by Excel in French.

**Acceptance Scenarios**:
1. **Given** a template with a table, **When** rendered as CSV, **Then** the first table (or the
   one marked `docmint-export`) is written with `;`, quoted per RFC 4180, UTF-8 with BOM.
2. **Given** a list of `@ExcelColumn`-annotated objects, **When** exported as CSV, **Then** same
   columns and order as Excel; numbers unformatted (`1250000`), dates ISO-8601.
3. **Given** a cell beginning with `=`, `+`, `-`, `@`, tab or CR, **When** exported, **Then** it is
   prefixed with `'` (formula injection protection).
4. **Given** separator `,` and no BOM in the options, **When** exported, **Then** they are used.

### Edge Cases

- Template without any table rendered as CSV/XLSX → clear error.
- `colspan`/`rowspan` cells → value in the first cell, others empty (merged in Excel).
- Excel sheet names longer than 31 characters or with `[]:*?/\` → sanitised, unique.
- More than 1 048 576 rows → limit error (bounded by a configurable row limit, default 100 000).
- Empty list export → header only.
- `@ExcelColumn` on a getter or record component, unsupported types → `toString()`.

## Requirements *(mandatory)*

- **FR-201**: A `chart` template function MUST draw bar, line, pie and doughnut charts with
  title, labels, one or more series, legend, locale-formatted values, embedded font, configurable
  size and colours, without network access.
- **FR-202**: Chart inputs MUST be validated and bounded (≤ 10 series, ≤ 1000 points, sizes).
- **FR-203**: `OutputFormat` MUST gain `HTML`, `CSV` and `XLSX`; CSV and XLSX are produced from
  the HTML of the template by post-processors (no change to the PDF pipeline).
- **FR-204**: HTML tables MUST be converted to rows with support for `thead`/`tbody`/`tfoot`,
  `colspan`, `data-value` (raw value), `data-type` (`number`, `date`, `text`) and
  `data-docmint-sheet`.
- **FR-205**: CSV MUST follow RFC 4180 with configurable separator (default `;`) and BOM
  (default on), and neutralise formula injection.
- **FR-206**: XLSX MUST be produced by an optional module `docmint-excel` discovered
  automatically when on the class path; one sheet per table; typed cells; bold frozen header;
  auto-sized columns; values only (no formulas).
- **FR-207**: `@ExcelColumn(name, order, format)` on fields, getters or record components MUST
  drive exports of lists (`DocMint.csv(rows)`, `DocMint.export(rows, XLSX, options)`), and `@Report(format
  = CSV|XLSX)` methods returning a collection without template.
- **FR-208**: A configurable maximum number of exported rows (default 100 000).
- **FR-209**: The starter MUST support `format = CSV | XLSX | HTML` and the BOM MUST include
  `docmint-excel`.
- **FR-210**: V3 MUST NOT include V4+ features (Word, pivot tables, streaming, async, signature,
  PDF/A…).

## Success Criteria *(mandatory)*

- **SC-201**: Every chart type renders in PDF with correct proportions (tested on image pixels).
- **SC-202**: Exported workbooks reopen with the same values and types in 100 % of tests.
- **SC-203**: CSV files open in Excel (French) with correct columns and accents (BOM + `;`),
  verified by parsing in tests.
- **SC-204**: 0 formula injection: every dangerous prefix is neutralised in tests.
- **SC-205**: All V1/V2 tests pass unchanged; core coverage ≥ 80 %.

## Assumptions

- Charts are images generated by Java2D (see plan), sharp at print resolution.
- `docmint-excel` depends on Apache POI 6 (Apache 2.0); users who do not need Excel do not pay
  its size.
