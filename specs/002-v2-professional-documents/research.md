# Research — DocMint V2 (Phase 0)

Versions verified on Maven Central / npm on 2026-11-25.

## R1. Headers and footers — CSS running elements

- **Decision**: use CSS paged-media *running elements*, supported by OpenHTMLtoPDF:
  elements with class `docmint-header` / `docmint-footer` are moved to page margin boxes
  (`position: running(...)`, `@top-center { content: element(...) }`). Modifiers
  `docmint-first-page` (only page 1) and `docmint-not-first-page`. Placeholders
  `<span class="docmint-page"></span>` / `<span class="docmint-pages"></span>` use
  `counter(page)` / `counter(pages)`.
- The PDF renderer inspects the DOM (jsoup) and generates only the needed `@page` rules, so a
  custom footer **replaces** the V1 "Page X / Y" footer and templates without header/footer
  render exactly as in V1 (SC-105).
- **Alternatives**: Java API to pass header/footer HTML (less natural for template authors);
  PDFBox overlay after rendering (no layout integration, content overlap).

## R2. QR codes and barcodes — ZXing core 3.5.4

- **Decision**: `com.google.zxing:core:3.5.4` (Apache 2.0, no runtime dependency) to compute the
  code matrix; DocMint draws it into a PNG (1 module = 1 pixel scaled ×8, no smoothing) embedded
  as a `data:` URI with the requested CSS size → sharp at any print size (black/white pixels
  scaled by the PDF viewer, nearest neighbour). Formats: `qr`, `code128`, `ean13`.
- Exposed as Pebble **functions** `qr(content, size, level)` and
  `barcode(content, format, size, text)` returning safe HTML (`<img>` + optional text, both built
  by DocMint with escaping): the content is data, never markup.
- ZXing validates EAN-13 check digits and QR capacity → `CodeGenerationException`
  (`DOCMINT_CODE_INVALID`).
- **Alternatives**: SVG output (needs Batik, ~10 MB, more attack surface); `zxing:javase`
  (adds JCommander; not needed, ImageIO suffices).

## R3. Amount in words — own implementation

- **Decision**: implement French and English number spelling in DocMint (≈ 250 lines, fully
  tested) — no dependency. French: traditional spelling by default (`vingt et un`,
  `quatre-vingts`, `deux cents`, `mille` invariable, `cent`/`vingt` invariable before `mille`,
  plural before `millions`), 1990 spelling optional (hyphens between all numerals; `million`,
  `milliard`, `billion` are nouns and keep spaces), long scale (10^12 = `billion`). Currencies:
  XOF/XAF (`franc(s) CFA`, no subunit), EUR (`euro(s)`, `centime(s)`), USD (`dollar(s)`,
  `cent(s)`), with `de`/`d'` after exact millions (`un million de francs CFA`).
- **Alternatives**: ICU4J RuleBasedNumberFormat (12 MB, CLDR rules differ from invoice usage:
  no currency phrasing, `quatre-vingts` handling varies by version).

## R4. Watermark and protection — PDFBox post-processors

- **Decision**: new port `PostProcessor` (ordered chain) applied after rendering:
  `PdfWatermarkPostProcessor` (PDFBox content stream, Noto Sans subset, extended graphics state
  for opacity, prepend for "behind content"), then `PdfProtectionPostProcessor`
  (`StandardProtectionPolicy`, AES-256, random owner password from `SecureRandom` when absent).
- `Protection` stores passwords as `char[]` copies, redacts them in `toString`, and has no
  getters usable by the template model mapper (never passed to templates anyway).

## R5. PNG preview — PDFBox `PDFRenderer`

- **Decision**: `OutputFormat.PNG` with `baseFormat() == PDF`: the PDF pipeline runs (with
  watermark), then `PngPreviewPostProcessor` rasterises one page with
  `PDFRenderer.renderImageWithDPI` (`ImageType.RGB`) and encodes with ImageIO. Default 96 DPI,
  range 36–300, max 25 megapixels (`ResourceLimits.maxImagePixels`), checked **before**
  rasterising. Protection + PNG → `DOCMINT_PROTECTION_INVALID`.

## R6. Angular library — ng-packagr 22.3.0, Vitest 5.1.1

- **Decision**: Angular workspace `docmint-angular/` (outside the Maven build) with library
  `projects/docmint-angular` built by ng-packagr (Angular Package Format), published to npm as
  `@juste120/docmint-angular`. `DocMintClient` service (constructor injection of `HttpClient`)
  + pure functions `parseFileName`, `toDocMintError`. Unit tests with Vitest on pure functions
  and on the service with a fake `HttpClient` (no TestBed, no browser).
- The demo uses the library through a workspace path mapping.
