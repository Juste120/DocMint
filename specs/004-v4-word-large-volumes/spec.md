# Feature Specification: DocMint V4 — Word and large volumes

**Feature Branch**: `004-v4-word-large-volumes`
**Created**: 2026-11-25
**Status**: Draft
**Input**: "V4 (0.4.0) – Word and large volumes: .docx export, pivot tables, streaming,
asynchronous generation." (`ROADMAP.md`)

> V1–V3 stay backward compatible. Defaults decided by the assistant at the maintainer's request.

## User Scenarios & Testing

### US1 - Word export (P1)
A developer renders an existing template as an editable Word document (.docx), e.g. a contract
or an attestation the customer must complete.

**Acceptance**:
1. Headings (`h1`–`h6`), paragraphs, bold/italic/underline, line breaks, lists, tables (header
   row bold, `colspan` merged), images (`data:` URIs: logos, QR codes, charts), page breaks
   (`.page-break`), and `docmint-header`/`docmint-footer` (with page numbers) are converted.
2. `OutputFormat.DOCX` works from the Java API and `@Report(format = DOCX)` with the optional
   module `docmint-word`; without it, a clear error names the dependency.
3. Text from data is inserted as text (no field codes, no macros); the document contains no
   macro.

### US2 - Pivot tables (P2)
A template author summarises rows (e.g. sales per city and month) in a cross table with totals.

**Acceptance**:
1. `{% set p = pivot(sales, rows="city", columns="month", value="amount", aggregate="sum") %}`
   gives `p.rowKeys`, `p.columnKeys`, `p.cells[r][c]`, `p.rowTotals`, `p.columnTotals`,
   `p.total`; aggregates `sum`, `count`, `avg`, `min`, `max`; exact decimals; missing cells are
   empty (`null`).
2. Keys keep their first-appearance order unless `sort=true`.
3. The rendered HTML table exports to CSV/Excel like any table.

### US3 - Streaming (P2)
A developer writes documents directly to an output stream, and exports large lists as CSV
without loading them in memory.

**Acceptance**:
1. `docMint.render(request, outputStream)` writes the document to the stream.
2. `docMint.exportCsv(Stream<?> rows, outputStream, settings)` writes rows one by one (memory
   independent of the number of rows); the row limit does not apply to streaming exports, which
   are bounded by the caller's stream.
3. A `@Report(format = CSV)` method returning a `java.util.stream.Stream` streams the response
   (no `Content-Length`).

### US4 - Asynchronous generation (P3)
A developer generates documents without blocking the calling thread.

**Acceptance**:
1. `docMint.renderAsync(request)` returns a `CompletableFuture<RenderResponse>`; errors complete
   it exceptionally with the usual `DocMintException`.
2. The executor is configurable (`DocMintBuilder.executor(...)`); the default is a bounded pool.
3. A `@Report` method returning `CompletableFuture<T>` is rendered when the future completes
   (Spring MVC asynchronous request).

## Requirements
- **FR-301** `docmint-word` module (Apache POI XWPF), `OutputFormat.DOCX` (base HTML),
  discovered with `ServiceLoader` as a `PostProcessor`.
- **FR-302** HTML→DOCX conversion of the elements in US1; unknown elements keep their text.
- **FR-303** `pivot(...)` template function with the aggregates of US2, bounded (≤ 10 000 rows,
  ≤ 500 row keys × 500 column keys).
- **FR-304** `render(request, OutputStream)`, `exportCsv(Stream, OutputStream, CsvSettings)`;
  starter streaming for `Stream` return values.
- **FR-305** `renderAsync`, `DocMintBuilder.executor`, starter support of `CompletableFuture`.
- **FR-306** Security: no macros or field codes from data in DOCX; CSV streaming keeps formula
  injection protection; async keeps the time limit.
- **FR-307** No V5+ feature (signature, PDF/A, accessibility, server).

## Success Criteria
- **SC-301** DOCX files reopen with POI with the expected paragraphs, tables and images.
- **SC-302** Pivot results equal a reference computation (tests).
- **SC-303** Streaming 200 000 CSV rows reads them while writing (fewer than 1 000 rows read
  before the first bytes are written), so memory does not depend on the number of rows (test).
- **SC-304** All previous tests pass unchanged.
