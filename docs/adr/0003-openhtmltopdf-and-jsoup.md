# 0003. OpenHTMLtoPDF + jsoup for HTML → PDF

- Status: Accepted
- Date: 2026-11-25

## Context

We need a Java HTML/CSS → PDF engine supporting CSS paged media (page size, margins, margin
boxes, page counters, repeated table headers), embeddable fonts, and hooks to control resource
loading, with a licence compatible with Apache 2.0.

## Decision

- Use the community-maintained **OpenHTMLtoPDF** (`io.github.openhtmltopdf:openhtmltopdf-pdfbox`,
  1.1.x, based on Apache PDFBox 3). It is actively released and offers:
  `useExternalResourceAccessControl` (SSRF allow-list), `useUriResolver` +
  `useProtocolsStreamImplementation` (private class path scheme), `usePageSupplier` (page limit
  checked while rendering), `useFont(..., FSFontUseCase.FALLBACK_PRE)` (fallback font) and a
  diagnostic consumer (missing images).
- Parse the HTML with **jsoup** (MIT) and pass a W3C DOM (`withW3cDocument`). Templates can be
  written in normal HTML5 (unclosed `<br>`, `<meta charset>`), and since jsoup is not an XML
  parser, DTDs and external entities are never resolved: **XXE is impossible by construction**.
- Route OpenHTMLtoPDF's JUL logs to SLF4J at DEBUG/TRACE (otherwise it prints INFO lines for
  every document).

## Licence analysis

OpenHTMLtoPDF is **LGPL-2.1-or-later**. DocMint depends on it as a separate, unmodified JAR
resolved by the user's build tool (no shading, no copied code). The LGPL allows such use by
software under any licence, including Apache 2.0 libraries and proprietary applications; users
can replace the LGPL JAR with another version. If DocMint ever needs to modify OpenHTMLtoPDF, the
changes must be contributed upstream or published under the LGPL.

## Consequences

- Only `infrastructure.pdf` imports OpenHTMLtoPDF and PDFBox.
- Some CSS3 features (flexbox, grid) are not supported by the engine; layouts use tables/blocks.
- `useFastMode()` is the default in 1.1.x and is not called (SpotBugs flagged it as a no-op).

## Alternatives considered

- **Flying Saucer**: older lineage of the same engine, fewer features.
- **iText pdfHTML**: AGPL or commercial — incompatible.
- **Headless Chrome / Playwright**: excellent CSS, but an external browser process, heavy in
  Docker and harder to secure.
