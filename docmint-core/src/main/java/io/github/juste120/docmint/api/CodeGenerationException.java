package io.github.juste120.docmint.api;

import java.io.Serial;

/** Thrown when a QR code or barcode cannot be generated from the given content. */
public final class CodeGenerationException extends RenderException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_CODE_INVALID";

  /**
   * Creates the exception.
   *
   * @param templateName the template being rendered
   * @param line the line of the expression, or {@code null}
   * @param detail what is wrong with the content or the arguments
   */
  public CodeGenerationException(String templateName, Integer line, String detail) {
    super(
        CODE,
        "Cannot generate the code in template '"
            + quote(templateName)
            + "'"
            + (line == null ? "" : " at line " + line)
            + ": "
            + detail
            + ".",
        null);
  }
}
