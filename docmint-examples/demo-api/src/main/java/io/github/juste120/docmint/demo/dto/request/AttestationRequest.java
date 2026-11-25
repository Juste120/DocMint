package io.github.juste120.docmint.demo.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Options of an attestation, from query parameters: {@code ?purpose=...}.
 *
 * @param purpose why the attestation is requested, printed on the document (optional)
 */
public record AttestationRequest(@Size(max = 200) String purpose) {}
