package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.DocMintException;
import java.io.Serial;

/** Thrown when the client of a {@link Report @Report} endpoint does not accept its format. */
public final class ReportNotAcceptableException extends DocMintException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_NOT_ACCEPTABLE";

  /** Creates the exception for a PDF endpoint. */
  public ReportNotAcceptableException() {
    this("application/pdf");
  }

  /**
   * Creates the exception.
   *
   * @param mediaType the media type produced by the endpoint
   */
  public ReportNotAcceptableException(String mediaType) {
    super(
        CODE,
        "This endpoint produces "
            + mediaType
            + ", which the request's Accept header does not allow. Send 'Accept: "
            + mediaType
            + "' or '*/*'.",
        null);
  }
}
