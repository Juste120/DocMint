package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.DocMintException;
import java.io.Serial;

/** Thrown when the client of a {@link Report @Report} endpoint does not accept PDF. */
public final class ReportNotAcceptableException extends DocMintException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_NOT_ACCEPTABLE";

  /** Creates the exception. */
  public ReportNotAcceptableException() {
    super(
        CODE,
        "This endpoint produces application/pdf, which the request's Accept header does not"
            + " allow. Send 'Accept: application/pdf' or '*/*'.",
        null);
  }
}
