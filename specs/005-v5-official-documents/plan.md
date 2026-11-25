# Implementation Plan: DocMint V5 — Official documents

| Topic | Decision | Rejected |
|---|---|---|
| Signature | module `docmint-signature`: PDFBox `addSignature` + BouncyCastle `CMSSignedDataGenerator` (SHA-256, detached, `ETSI.CAdES.detached`), incremental save; `PostProcessor` via `ServiceLoader` | OpenPDF/iText signing (licences); timestamping (TSA requires network — later, behind the allow-list) |
| PDF/A | OpenHTMLtoPDF `usePdfAConformance` + sRGB ICC profile from `ICC_Profile.getInstance(CS_sRGB)` (no file to bundle) | post-conversion with Ghostscript |
| PDF/UA | OpenHTMLtoPDF `usePdfUaAccessibility(true)`; DocMint adds `lang` and `<title>` when missing | — |
| Options | `PdfStandard`, `accessible`, `SignatureSettings` in `RenderOptions`/`DocumentOptions` (old constructors kept) and builder defaults | — |

Constitution check: II (one option each) ✅ · IV (SPI reuse) ✅ · V (BouncyCastle only in the
optional module) ✅ · VII (secrets `char[]`, redacted, incompatible combinations refused) ✅ ·
scope guard ✅.
