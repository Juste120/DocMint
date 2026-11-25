# Feature Specification: DocMint V5 — Official documents

**Feature Branch**: `005-v5-official-documents`
**Created**: 2026-11-25
**Status**: Draft
**Input**: "V5 (0.5.0) – Official documents: electronic signature, PDF/A, accessibility."
(`ROADMAP.md`). Defaults decided by the assistant at the maintainer's request.

## User Scenarios & Testing

### US1 - Electronic signature (P1)
An administration or company signs attestations and invoices with its certificate so that
anyone can verify the issuer and that the document was not modified.

**Acceptance**:
1. With a PKCS#12 key store (password, optional alias) and the optional module
   `docmint-signature`, the PDF carries a CMS/PAdES-compatible detached signature (SHA-256),
   with signer name, reason, location and date.
2. Verifying the signature with standard tools (PDFBox + BouncyCastle in tests) succeeds; the
   signed byte range covers the whole document except the signature.
3. A signature can be configured globally (builder / `docmint.signature.*`) or per render;
   passwords and keys never appear in logs or errors.
4. Signature with password protection, or without the module, fails with a clear error.

### US2 - PDF/A archival documents (P1)
Documents that must be archived for years (invoices, payslips) are produced as PDF/A.

**Acceptance**:
1. `PdfStandard.PDF_A_3B` (default when PDF/A is requested), `PDF_A_3U`, `PDF_A_3A`,
   `PDF_A_2B`, `PDF_A_2U`, `PDF_A_2A` produce XMP metadata with the right `pdfaid` part and
   conformance, an sRGB output intent, embedded fonts and the document title.
2. PDF/A with password protection fails with a clear error (PDF/A forbids encryption).
3. Configurable globally or per render.

### US3 - Accessibility (P2)
Documents are readable by screen readers (tagged PDF, PDF/UA).

**Acceptance**:
1. With accessibility enabled, the PDF is tagged (structure tree, marked content), declares its
   language (`lang` of the template or the render locale) and its title (`<title>` or the
   template name), and images keep their alternative text.
2. Templates without `<title>` or `lang` still work (defaults are added).

## Requirements
- **FR-401** `SignatureSettings` (PKCS#12 bytes, `char[]` password, alias, reason, location,
  contact), module `docmint-signature` (BouncyCastle), `PostProcessor` discovered by
  `ServiceLoader`; signature is the last PDF step.
- **FR-402** `PdfStandard` enum and `accessible` flag applied at rendering (OpenHTMLtoPDF), with
  sRGB profile from the JDK.
- **FR-403** Options per render (`RenderOptions`) and defaults (`DocMintBuilder`, starter
  properties `docmint.pdf.standard`, `docmint.pdf.accessible`, `docmint.signature.*`).
- **FR-404** Incompatible combinations (PDF/A or signature with protection; signature on a
  non-PDF format) fail with `DOCMINT_PROTECTION_INVALID` / `DOCMINT_SIGNATURE_INVALID`.
- **FR-405** Secrets (key store passwords) are `char[]`, redacted, wiped by `clear()`; never
  logged.
- **FR-406** No post-1.0 feature (server, scheduling, email, visual editor).

## Success Criteria
- **SC-401** 100 % of signed test documents verify cryptographically.
- **SC-402** PDF/A outputs contain valid `pdfaid` metadata and an output intent.
- **SC-403** Accessible outputs contain a structure tree and language.
- **SC-404** All previous tests pass unchanged.
