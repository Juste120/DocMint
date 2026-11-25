package io.github.juste120.docmint.domain;

/**
 * PDF/A conformance levels for long-term archiving. Encryption is forbidden in PDF/A.
 *
 * <ul>
 *   <li>{@code B} (basic): visual appearance is preserved.
 *   <li>{@code U} (Unicode): B, and all text can be extracted as Unicode.
 *   <li>{@code A} (accessible): U, and the document is tagged.
 * </ul>
 *
 * PDF/A-3 also allows embedded files; PDF/A-2 and 3 allow transparency (watermarks).
 */
public enum PdfStandard {
  /** PDF/A-2b. */
  PDF_A_2B,
  /** PDF/A-2u. */
  PDF_A_2U,
  /** PDF/A-2a. */
  PDF_A_2A,
  /** PDF/A-3b (recommended for invoices and archives). */
  PDF_A_3B,
  /** PDF/A-3u. */
  PDF_A_3U,
  /** PDF/A-3a. */
  PDF_A_3A;

  /**
   * Returns the PDF/A part number (2 or 3).
   *
   * @return the part
   */
  public int part() {
    return name().charAt(6) - '0';
  }

  /**
   * Returns the conformance letter ({@code B}, {@code U} or {@code A}).
   *
   * @return the conformance
   */
  public String conformance() {
    return name().substring(7);
  }
}
