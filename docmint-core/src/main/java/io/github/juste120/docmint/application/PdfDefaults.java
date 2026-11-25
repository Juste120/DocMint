package io.github.juste120.docmint.application;

import io.github.juste120.docmint.domain.PdfStandard;
import io.github.juste120.docmint.domain.SignatureSettings;

/**
 * Default PDF/A level, accessibility and signature of PDF documents (from the builder).
 *
 * @param pdfStandard the PDF/A level, or {@code null}
 * @param accessible whether PDFs are tagged (PDF/UA)
 * @param signature the signature, or {@code null}
 */
public record PdfDefaults(
    PdfStandard pdfStandard, boolean accessible, SignatureSettings signature) {

  /** No PDF/A, not tagged, not signed. */
  public static final PdfDefaults NONE = new PdfDefaults(null, false, null);
}
