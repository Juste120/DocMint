# Contract — V2 templates and Java API

## Template classes

| Class | Effect |
|---|---|
| `docmint-header` | element repeated at the top of every page |
| `docmint-footer` | element repeated at the bottom of every page (replaces "Page X / Y") |
| + `docmint-first-page` | only on page 1 |
| + `docmint-not-first-page` | on every page except page 1 |
| `docmint-page` / `docmint-pages` | current page number / page count (inside header/footer) |

## Template functions and filters

```
{{ qr("https://verify.example.com/F-1") }}
{{ qr(url, size="25mm", level="H") }}              level: L | M (default) | Q | H
{{ barcode("F-2026-001") }}                        format code128 (default)
{{ barcode("5901234123457", format="ean13", size="40mm", text=true) }}
{{ total | words }}                                 un million deux cent cinquante mille
{{ total | words("XOF") }}                          … francs CFA  (alias "fcfa")
{{ total | words("EUR", "en") }}                    one thousand … euros and fifty cents
{{ total | words(spelling="1990") }}                deux-cent-cinquante-mille
```

## Java

```java
byte[] png = docMint.png("invoice", data);                       // page 1, 96 DPI

docMint.render(RenderRequest.of("invoice", data).withOptions(RenderOptions.defaults()
    .withWatermark(Watermark.text("COPIE"))
    .withProtection(Protection.openPassword("1234".toCharArray())
        .withPermissions(Permission.PRINT))));

docMint.render(new RenderRequest("invoice", data, OutputFormat.PNG,
    RenderOptions.defaults().withPreview(new PreviewSettings(2, 150))));
```

## Spring

```java
@GetMapping("/{id}/pdf")
@Report(value = "facture", filename = "facture-{number}.pdf")
ReportContent pdf(@PathVariable Long id) {
  InvoiceResponse invoice = service.findById(id);
  return ReportContent.of(invoice)
      .withWatermark(invoice.paid() ? Watermark.text("PAYÉE") : null);
}

@GetMapping("/{id}/preview")
@Report(value = "facture", format = OutputFormat.PNG)
InvoiceResponse preview(@PathVariable Long id) { … }
```

New codes: `DOCMINT_CODE_INVALID`, `DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE`,
`DOCMINT_PROTECTION_INVALID`, `DOCMINT_WATERMARK_INVALID` (HTTP 500, RFC 9457).

## Angular (`@juste120/docmint-angular`)

```ts
const docs = inject(DocMintClient);
docs.view('/api/invoices/1/pdf').subscribe();
docs.download('/api/invoices/1/pdf').subscribe();
docs.preview('/api/invoices/1/preview').subscribe(url => this.src = url);  // release(url) when done
// errors: DocMintError { status, code, title, detail }
```
