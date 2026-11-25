package io.github.juste120.agbale.spring.web;

import io.github.juste120.agbale.api.AgbaleException;
import java.io.Serial;

/** Thrown when a {@link Report @Report} method is declared or used incorrectly. */
public final class InvalidReportMethodException extends AgbaleException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "AGBALE_INVALID_REPORT_METHOD";

  /**
   * Creates the exception.
   *
   * @param message what is wrong and how to fix it
   */
  public InvalidReportMethodException(String message) {
    super(CODE, message, null);
  }
}
