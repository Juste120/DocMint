# 0012. ZXing for QR codes and barcodes

- Status: Accepted
- Date: 2026-11-25

## Context

Invoices and attestations need verification QR codes and barcodes, generated without any
network access and with a licence compatible with Apache 2.0.

## Decision

`com.google.zxing:core` (Apache 2.0, no runtime dependency) computes the code matrix; DocMint
draws it as a black-and-white PNG (8 pixels per QR module) embedded as a `data:` URI, exposed
through the Pebble functions `qr(...)` and `barcode(...)`. Supported formats: QR, Code 128,
EAN-13. Content length, sizes and levels are validated; the optional human-readable text is
HTML-escaped.

## Consequences

- No SVG engine (Batik) is needed; codes stay sharp because modules are scaled without
  smoothing.
- New formats (DataMatrix, PDF417) are a few lines each when requested.

## Alternatives considered

SVG output (needs Batik, large and with a wider attack surface); `zxing:javase` (extra
dependency for image writing that ImageIO already covers).
