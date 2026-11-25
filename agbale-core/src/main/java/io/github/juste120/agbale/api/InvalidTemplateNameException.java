package io.github.juste120.agbale.api;

import java.io.Serial;

/**
 * Thrown when a template name is invalid, for example when it tries to leave the templates folder.
 */
public final class InvalidTemplateNameException extends RenderException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "AGBALE_INVALID_TEMPLATE_NAME";

  /**
   * Creates the exception.
   *
   * @param name the rejected name
   * @param reason why it is rejected
   */
  public InvalidTemplateNameException(String name, String reason) {
    super(
        CODE,
        "Invalid template name '"
            + quote(name)
            + "': "
            + reason
            + ". A name is relative to the templates folder, for example 'invoice' or"
            + " 'layouts/base'.",
        null);
  }
}
