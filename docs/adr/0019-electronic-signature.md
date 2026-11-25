# 0019. Electronic signature module

- Status: Accepted
- Date: 2026-11-25

## Context

Attestations and certificates must prove their issuer and integrity. Signing needs CMS, which
the JDK does not provide.

## Decision

Optional module `docmint-signature`: PDFBox `addSignature` + Bouncy Castle
`CMSSignedDataGenerator` (detached, `adbe.pkcs7.detached`, SHA-256, chain included),
incremental save, last post-processing step. Keys come from PKCS#12 key stores
(`SignatureSettings`, `char[]` password, redacted, wipeable). A requested signature without the
module fails (`PostProcessor.provides("signature")`) instead of silently producing an unsigned
document. Signature with password protection is refused.

## Consequences

- Tests verify signatures cryptographically and check that tampering is detected.
- Not yet included: timestamping (TSA needs network access, to be added behind the allow-list),
  visible signature appearance, long-term validation (PAdES-LTV).
