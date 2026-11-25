# Migrating from JasperReports to DocMint

DocMint replaces JasperReports for business documents (invoices, receipts, statements,
attestations) with plain HTML/CSS templates. This guide maps JasperReports concepts to DocMint
and gives a step-by-step migration path.

## Why migrate

| JasperReports | DocMint |
|---|---|
| `.jrxml` XML designed in Jaspersoft Studio | `.html` templates edited in any IDE, previewable in a browser |
| Compile `.jrxml` → `.jasper` | Nothing to compile; templates are cached automatically |
| Bands, absolute positioning | CSS layout, tables that grow and paginate |
| Expressions in Java/Groovy inside the report | Data prepared in a response DTO; filters for formatting |
| `JRBeanCollectionDataSource`, `JRDataSource` | Any record, JavaBean or `Map` |
| Fonts extensions to embed fonts | Noto Sans embedded; add fonts with one line |
| Exporters (PDF, XLSX, DOCX, CSV) | `OutputFormat.PDF`, `XLSX`, `DOCX`, `CSV`, `PNG`, `HTML` |

## Concept mapping

| JasperReports | DocMint |
|---|---|
| Report template (`.jrxml`) | `src/main/resources/reports/<name>.html` |
| `JasperCompileManager.compileReport` | not needed |
| `JasperFillManager.fillReport(report, params, dataSource)` | `docMint.pdf("name", data)` |
| `JasperExportManager.exportReportToPdf` | the bytes returned by `pdf(...)` / `render(...)` |
| Parameters (`$P{name}`) | properties of the data object: `{{ name }}` |
| Fields (`$F{amount}`) in the detail band | `{% for line in lines %}{{ line.amount }}{% endfor %}` |
| Variables (`$V{total}`, sum calculation) | `{{ lines \| sum("amount") }}` or a field computed in the DTO |
| Title band | top of the HTML body |
| Page header / page footer bands | `<div class="docmint-header">`, `<div class="docmint-footer">` |
| `$V{PAGE_NUMBER}` / total pages | `<span class="docmint-page">` / `<span class="docmint-pages">` |
| Column header band repeated on each page | `<thead>` (repeated automatically) |
| Group header / footer | nested loops, or `pivot(...)` for cross tables |
| Subreports | `{% include "parts/lines" %}` or layouts with `{% extends %}` |
| Crosstab | `pivot(items, rows=…, columns=…, value=…, aggregate="sum")` |
| Charts (JFreeChart) | `chart(type="bar"…)` |
| Barcode component | `barcode(...)`, `qr(...)` |
| Pattern on text fields (`#,##0.00`, dates) | `money`, `fcfa`, `date("dd/MM/yyyy")`, `numberformat` |
| Images (`$P{logo}` or path) | `<img src="images/logo.png">` from the templates folder |
| Print when expression | `{% if condition %} … {% endif %}` |
| Page break element | `<div class="page-break"></div>` |
| Background band (watermark) | `RenderOptions.withWatermark(Watermark.text("COPIE"))` |
| PDF exporter: encryption, PDF/A, tagged PDF | `withProtection`, `withPdfStandard`, `withAccessible` |
| Digital signature (third-party) | `docmint-signature` + `withSignature` |
| Virtualizer for large reports | `exportCsv(Stream…)` for large exports; split very large PDFs |

## Step by step

1. **Add DocMint** (`docmint-spring-boot-starter`, or `docmint-core` without Spring) next to
   JasperReports; migrate one report at a time.
2. **Create a response DTO** for the report: one record holding exactly what the document
   shows, including computed values (totals, labels). Map it from your entities (MapStruct).
3. **Rewrite the layout in HTML**: start from the PDF produced by JasperReports and write the
   HTML structure (tables for lines, `div`s for blocks). Use a shared layout
   (`layouts/base.html`) for the logo, header and footer.
4. **Replace expressions**: `$F{x}` → `{{ x }}`, loops over the data source → `{% for %}`,
   patterns → filters. Undefined variables fail fast, which catches mistakes.
5. **Compare outputs**: render both versions with the same data and compare the extracted text
   (as DocMint's own tests do with PDFBox).
6. **Expose it**: `@Report("invoice")` on the controller method; remove the JasperReports export
   code.
7. When every report is migrated, remove the JasperReports dependencies and `.jrxml` files.

## Example

JasperReports detail band:

```xml
<detail><band height="20">
  <textField><reportElement x="0" y="0" width="300" height="20"/>
    <textFieldExpression><![CDATA[$F{label}]]></textFieldExpression></textField>
  <textField pattern="#,##0"><reportElement x="300" y="0" width="100" height="20"/>
    <textFieldExpression><![CDATA[$F{amount}]]></textFieldExpression></textField>
</band></detail>
```

DocMint:

```html
<table>
  <thead><tr><th>Désignation</th><th>Montant</th></tr></thead>
  <tbody>
    {% for line in lines %}
    <tr><td>{{ line.label }}</td><td>{{ line.amount | fcfa }}</td></tr>
    {% endfor %}
  </tbody>
</table>
```

## What is different on purpose

- **No logic in templates**: templates cannot call Java methods (security sandbox). Compute in
  the DTO, format with filters.
- **No absolute positioning by default**: CSS flows and paginates content. For pixel-exact forms
  (pre-printed paper), use CSS `position: absolute` with `mm` units on a page-sized container.
- **Flexbox and grid are not supported by the PDF engine**: use tables and blocks.
