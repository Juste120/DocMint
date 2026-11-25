# Tasks: DocMint V2 — Professional Documents

Format: `- [ ] [ID] [P?] [Story?] Description`. Paths: `core` =
`docmint-core/src/main/java/io/github/juste120/docmint`, tests mirror it under `src/test`.

## Phase 1: Setup

- [x] T101 Add `com.google.zxing:core` 3.5.4 to the parent dependency management and to `docmint-core`; extend ArchUnit rule "only infrastructure uses engines" with `com.google.zxing..`

## Phase 2: Foundational

- [x] T102 [P] Domain: `Watermark`, `Protection`, `Permission`, `PreviewSettings`, `DocumentOptions`, `OutputFormat.PNG` + `baseFormat()`, `ResourceLimits.maxImagePixels` (V1 constructor kept) + tests
- [x] T103 [P] API: `RenderOptions` new components (V1 constructor kept), `CodeGenerationException`, `InvalidOptionsException`, `Limit.IMAGE_SIZE`
- [x] T104 Ports: `PostProcessor`, `PostProcessContext`; `RenderContext.options` (V1 constructor kept)
- [x] T105 `DefaultReportService`: render base format, then ordered post-processor chain, final format check

## Phase 3: US1 Headers and footers (P1)

- [x] T106 [US1] `PageMarginBoxes` (DOM inspection → generated `@page` rules) wired in `OpenHtmlPdfRenderer`; default footer only when no custom footer
- [x] T107 [US1] `HeaderFooterTest` (every page, numbers, first-page variants, replaces default, logo, V1 unchanged)

## Phase 4: US2 QR codes and barcodes (P1)

- [x] T108 [US2] `CodeFunctions` (`qr`, `barcode`) in the Pebble extension; size/level validation; escaped text
- [x] T109 [US2] `CodesTest` (decode from PDF images, sizes, levels, EAN-13 check digit, capacity, injection)

## Phase 5: US3 Amount in words (P2)

- [x] T110 [US3] `NumberSpeller` (French traditional + 1990, English) and `WordsFilter` (currencies XOF/XAF/fcfa/EUR/USD, decimals, negative, range)
- [x] T111 [US3] `NumberSpellerTest` (≥ 200 French + 100 English reference amounts) and filter tests

## Phase 6: US4 Watermark (P2)

- [x] T112 [US4] `PdfWatermarkPostProcessor` (text/image, angle, opacity, colour, layer, auto-fit) + builder default watermark
- [x] T113 [US4] `WatermarkTest`

## Phase 7: US5 Password protection (P2)

- [x] T114 [US5] `PdfProtectionPostProcessor` (AES-256, permissions, random owner password)
- [x] T115 [US5] `ProtectionTest` (cannot open without password, permissions, no password in logs/messages)

## Phase 8: US6 PNG preview (P3)

- [x] T116 [US6] `PngPreviewPostProcessor`, `DocMint.png`, pixel limit, page range
- [x] T117 [US6] `PreviewTest`

## Phase 9: Starter (US4–US6)

- [x] T118 `ReportContent`, `@Report(format)`, Accept check per format, properties `docmint.watermark.*`, `docmint.limits.max-image-pixels`
- [x] T119 Starter tests (watermark/protection/PNG endpoints, properties)

## Phase 10: US7 Angular library (P3)

- [x] T120 [P] [US7] `docmint-angular/` workspace, library `@juste120/docmint-angular` (`DocMintClient`, `parseFileName`, `toDocMintError`), README
- [x] T121 [US7] Vitest tests; demo uses the library; CI job builds and tests it

## Phase 11: Demo, docs, polish

- [x] T122 Demo: header/footer layout, QR verification code, amount in words, watermark for unpaid invoices, protected attestation, preview endpoint
- [x] T123 Docs: README, user guide, CHANGELOG `[0.2.0]`, ROADMAP, ADR-0011 (post-processors), ADR-0012 (ZXing), ADR-0013 (Angular library)
- [x] T124 Full verify on JDK 17 and 21; V1 tests unchanged; coverage ≥ 80 %
