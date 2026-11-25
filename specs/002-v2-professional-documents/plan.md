# Implementation Plan: DocMint V2 — Professional Documents

**Branch**: `002-v2-professional-documents` | **Date**: 2026-11-25 | **Spec**: [spec.md](spec.md)

## Summary

Add headers/footers (CSS running elements generated from template classes), QR codes and
barcodes (ZXing → embedded PNG via Pebble functions), amount in words (own FR/EN speller),
watermark and AES-256 protection (PDFBox post-processors behind a new `PostProcessor` port),
PNG preview (`OutputFormat.PNG` derived from PDF + rasteriser post-processor), starter support
(`ReportContent`, `@Report(format)`, properties) and an Angular library
`@juste120/docmint-angular`. Details: [research.md](research.md),
[data-model.md](data-model.md), [contracts](contracts/templates-and-api.md).

## Technical Context

**Language/Version**: Java `--release 17`; TypeScript 6.0 / Angular 22 for the library.
**New dependencies**: `com.google.zxing:core:3.5.4` (Apache 2.0); npm dev: `ng-packagr` 22.3.0,
`vitest` 5.1.1.
**Testing**: as V1; codes decoded with ZXing in tests; protection checked by reopening with
PDFBox; Vitest for the library.
**Constraints**: V1 API source-compatible; V1 tests unchanged (SC-105).

## Constitution Check (v1.0.1)

| Principle | Compliance | Status |
|---|---|---|
| II Simplicity | each feature = one template expression or one option; all defaults safe | ✅ |
| III HTML/CSS | headers/footers are HTML elements with classes, CSS paged media | ✅ |
| IV Escape hatches | `PostProcessor` port is public SPI | ✅ |
| V Architecture | new adapters/post-processors; core pipeline only gains an extension point; ArchUnit rules extended to ZXing | ✅ |
| VI Quality | tests for each story, coverage ≥ 80 %, Javadoc | ✅ |
| VII Security | escaped code text, bounded code/image sizes, AES-256 only, passwords never logged (automated log check) | ✅ |
| IX Francophone Africa | `words` in French with CFA franc, traditional spelling | ✅ |
| Scope guard | FR-132 | ✅ |

## Project Structure (additions)

```text
docmint-core/src/main/java/io/github/juste120/docmint/
├── domain/   Watermark, Protection, Permission, PreviewSettings, DocumentOptions
├── api/      CodeGenerationException, InvalidOptionsException (+ RenderOptions, DocMint.png)
├── port/     PostProcessor, PostProcessContext
├── application/ DefaultReportService (post-processor chain)
└── infrastructure/
    ├── pebble/ CodeFunctions (qr, barcode), WordsFilter, NumberSpeller (FR, EN)
    └── pdf/    PageMarginBoxes (headers/footers CSS), PdfWatermarkPostProcessor,
                PdfProtectionPostProcessor, PngPreviewPostProcessor
docmint-spring-boot-starter/…/spring/web/ ReportContent (+ Report.format)
docmint-angular/   Angular workspace: projects/docmint-angular (library), Vitest tests
```

## Complexity Tracking

| Deviation | Why | Simpler alternative rejected |
|---|---|---|
| `OutputFormat.baseFormat()` | PNG reuses the whole PDF pipeline (fonts, watermark) | a separate HTML→PNG renderer would duplicate the PDF adapter |
| Angular workspace outside Maven | npm toolchain | publishing from Maven would need frontend plugins and Node in every Java build |
