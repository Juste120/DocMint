package io.github.juste120.docmint.api;

import java.io.Serial;

/** Thrown when render options cannot be applied (preview page, protection, watermark). */
public final class InvalidOptionsException extends RenderException {

  @Serial private static final long serialVersionUID = 1L;

  /** The requested preview page does not exist. */
  public static final String PREVIEW_PAGE_OUT_OF_RANGE = "DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE";

  /** The protection cannot be applied (e.g. on an image). */
  public static final String PROTECTION_INVALID = "DOCMINT_PROTECTION_INVALID";

  /** The electronic signature cannot be applied (module missing, wrong key store...). */
  public static final String SIGNATURE_INVALID = "DOCMINT_SIGNATURE_INVALID";

  /** The watermark cannot be applied (e.g. image not found). */
  public static final String WATERMARK_INVALID = "DOCMINT_WATERMARK_INVALID";

  /**
   * Creates the exception.
   *
   * @param code one of the codes of this class
   * @param message what is wrong and what to do
   */
  public InvalidOptionsException(String code, String message) {
    super(code, message, null);
  }
}
