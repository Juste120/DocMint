package io.github.juste120.agbale.api;

import java.io.Serial;

/**
 * Thrown when a document cannot be rendered. Subtypes describe specific causes (invalid template
 * name, refused or missing resource, exceeded limit).
 */
public class RenderException extends AgbaleException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable code of a generic render failure. */
  public static final String CODE = "AGBALE_RENDER_FAILED";

  /** Stable code of an error raised while evaluating a template (undefined variable, ...). */
  public static final String EVALUATION_CODE = "AGBALE_TEMPLATE_EVALUATION";

  /**
   * Creates a generic render exception.
   *
   * @param message the actionable message
   * @param cause the cause, may be {@code null}
   */
  public RenderException(String message, Throwable cause) {
    this(CODE, message, cause);
  }

  /**
   * Creates a render exception with a specific code.
   *
   * @param code the stable error code
   * @param message the actionable message
   * @param cause the cause, may be {@code null}
   */
  protected RenderException(String code, String message, Throwable cause) {
    super(code, message, cause);
  }

  /**
   * Creates the exception raised when a template fails during evaluation.
   *
   * @param templateName the template being evaluated
   * @param line the line number, or {@code null}
   * @param detail what went wrong
   * @param cause the engine error
   * @return the exception
   */
  public static RenderException evaluation(
      String templateName, Integer line, String detail, Throwable cause) {
    return new RenderException(
        EVALUATION_CODE,
        "Error while evaluating template '"
            + quote(templateName)
            + "'"
            + (line == null ? "" : " at line " + line)
            + ": "
            + quote(detail)
            + ". Check that the variable exists in the data you pass, or use the 'default'"
            + " filter.",
        cause);
  }
}
