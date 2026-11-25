# 0018. PDF/A and PDF/UA through OpenHTMLtoPDF

- Status: Accepted
- Date: 2026-11-25

## Context

Official and archived documents must be PDF/A; public services increasingly require accessible
PDFs.

## Decision

Use OpenHTMLtoPDF's built-in `usePdfAConformance` (levels 2 and 3, B/U/A) and
`usePdfUaAccessibility`. The sRGB output intent comes from the JDK
(`ICC_Profile.getInstance(CS_sRGB)`), so no ICC file is bundled. PDF/A-1 is not offered (no
transparency, which watermarks use). DocMint adds the title and language when the template has
none. PDF/A with encryption is refused.

## Consequences

Formal conformance is produced by the engine; a veraPDF validation step can be added to CI later.
