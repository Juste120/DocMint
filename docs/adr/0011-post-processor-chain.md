# 0011. Post-processor chain for watermark, protection and preview

- Status: Accepted
- Date: 2026-11-25

## Context

V2 adds operations on the rendered document (watermark, encryption, conversion to PNG). V1
rendering must not change (constitution, Principle V; spec SC-105).

## Decision

- New public SPI `PostProcessor` (`appliesTo`, `process`) and `PostProcessContext`. The
  application service renders the **base format** (`OutputFormat.baseFormat()`, PDF for PNG),
  then runs the post-processors in order: watermark → protection → custom ones → PNG preview.
  The final format must equal the requested one.
- Watermark, protection and preview are **render options** (`RenderOptions`), decided by Java
  code; templates stay data-only and passwords never enter the template model.
- Implementations use PDFBox (already a dependency): content streams with an extended graphics
  state for transparency, `StandardProtectionPolicy` with AES-256 only, `PDFRenderer` for PNG.

## Consequences

- A PDF is loaded and saved once per applicable post-processor (acceptable for business
  documents; can be merged later if profiling shows a need).
- `Protection` is shared, not copied, so callers can wipe passwords (`clear()`); this is
  justified in the SpotBugs exclusions.
