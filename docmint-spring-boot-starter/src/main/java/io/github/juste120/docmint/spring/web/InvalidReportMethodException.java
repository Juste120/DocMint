package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.DocMintException;
import java.io.Serial;

/** Thrown when a {@link Report @Report} method is declared or used incorrectly. */
public final class InvalidReportMethodException extends DocMintException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_INVALID_REPORT_METHOD";

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  public InvalidReportMethodException(String message) {
    super(CODE, message, null);
  }
}
