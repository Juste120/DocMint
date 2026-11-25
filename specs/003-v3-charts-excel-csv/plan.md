# Implementation Plan: DocMint V3 — Charts, Excel and CSV

**Branch**: `003-v3-charts-excel-csv` | **Date**: 2026-11-25 | **Spec**: [spec.md](spec.md)

## Summary

Charts as a Pebble function drawn with Java2D (no dependency); new formats `HTML`, `CSV`, `XLSX`
derived from the template's HTML by post-processors; table extraction with jsoup; CSV in core;
XLSX in a new optional module `docmint-excel` (Apache POI 6.0.0) discovered with
`ServiceLoader`; `@ExcelColumn` annotation in the core API for list exports; starter support.

## Technical decisions (research)

| Topic | Decision | Rejected alternative |
|---|---|---|
| Chart rendering | Java2D, anti-aliased, drawn at 3× the CSS size (≈ 288 DPI), PNG `data:` URI, embedded Noto Sans | SVG via Batik (≈ 10 MB, larger attack surface, OpenHTMLtoPDF SVG plugin); JFreeChart (LGPL, heavy) |
| Base format | `OutputFormat.HTML` rendered by an `HtmlDocumentRenderer`; `CSV.baseFormat() = HTML`, `XLSX.baseFormat() = HTML` | rendering a PDF then extracting tables (lossy) |
| Tables | `TableExtractor` (jsoup) → `ExtractedTable(name, headerRows, rows of Cell(text, value, type, colspan))` | — |
| CSV | core `CsvPostProcessor` + `CsvWriter`; `CsvSettings(separator, bom)` in `RenderOptions` (default `;`, BOM) | — |
| XLSX | module `docmint-excel`: `XlsxPostProcessor` (registered in `META-INF/services/…PostProcessor`), `XlsxWriter` with `SXSSFWorkbook`-free `XSSFWorkbook`, styles cached | putting POI in core (+10 MB for everyone) |
| List export | `@ExcelColumn` (core `api`), `ColumnModel` introspection (fields, getters, record components), `DocMint.csv(rows)`, `DocMintExcel.xlsx(rows)` | — |
| Limits | `ResourceLimits.maxExportRows` (default 100 000) | — |
| Discovery | `DocMintBuilder` loads `PostProcessor`s with `ServiceLoader` (class loader of the build) before the PNG converter | explicit registration only (less "zero config") |

## Constitution Check

II one expression / one annotation ✅ · III tables stay HTML ✅ · IV `PostProcessor` SPI +
`ServiceLoader` ✅ · V new adapters, PDF pipeline untouched, optional module for POI ✅ ·
VI tests + coverage ✅ · VII formula injection neutralised, bounded rows/points ✅ ·
IX `;` + BOM for French Excel ✅ · Scope guard FR-210 ✅.

## Structure

```text
docmint-core/…/domain/      OutputFormat (+HTML, CSV, XLSX), CsvSettings, ResourceLimits.maxExportRows
docmint-core/…/api/         ExcelColumn, DocMint.csv(rows), RenderOptions.csv
docmint-core/…/port/        TableData (extracted tables) shared with modules
docmint-core/…/infrastructure/
    pebble/ ChartFunction, ChartPainter
    table/  TableExtractor, ColumnModel, CsvWriter, CsvPostProcessor, HtmlDocumentRenderer
docmint-excel/              XlsxPostProcessor, XlsxWriter, DocMintExcel, META-INF/services
docmint-spring-boot-starter/ @Report formats CSV/XLSX/HTML, collection exports
```

## Complexity Tracking

| Deviation | Why | Simpler alternative rejected |
|---|---|---|
| Charts as raster images instead of SVG | no SVG engine without Batik; 288 DPI prints sharply | Batik dependency (size, CVE history) |
| New module `docmint-excel` | POI and its transitive dependencies are ~12 MB | everyone would download POI |
| `port.TableData` public | the Excel module (separate artifact) must read extracted tables | duplicating the extractor in the module |
