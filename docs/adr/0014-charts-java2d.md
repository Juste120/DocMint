# 0014. Charts drawn with Java2D

- Status: Accepted
- Date: 2026-11-25

## Context

The roadmap announced a `<chart>` tag rendered as SVG. OpenHTMLtoPDF needs Apache Batik
(≈ 10 MB, regular CVEs) to draw SVG, and charting libraries are either LGPL (JFreeChart) or
JavaScript-based.

## Decision

A Pebble function `chart(...)` (the "tag") draws bar, line, pie and doughnut charts with Java2D
at three times the CSS size (≈ 288 DPI, sharp when printed), with the embedded Noto Sans font,
and embeds the PNG as a `data:` URI. Inputs are validated and bounded (10 series, 1000 points).

## Consequences

- No new dependency, no network, no script execution.
- Charts are raster images in the PDF (not vector): acceptable at 288 DPI for business documents;
  an SVG renderer can be added later as another adapter if needed.
