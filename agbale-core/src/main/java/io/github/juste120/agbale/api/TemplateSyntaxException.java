package io.github.juste120.agbale.api;

import java.io.Serial;
import java.util.Optional;

/** Thrown when a template cannot be parsed. */
public final class TemplateSyntaxException extends AgbaleException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "AGBALE_TEMPLATE_SYNTAX";

  private final String templateName;
  private final Integer line;

  /**
   * Creates the exception.
   *
   * @param templateName the template containing the error
   * @param line the line number, or {@code null} if unknown
   * @param detail what is wrong
   * @param cause the parser error
   */
  public TemplateSyntaxException(String templateName, Integer line, String detail, Throwable cause) {
    super(
        CODE,
        "Syntax error in template '"
            + quote(templateName)
            + "'"
            + (line == null ? "" : " at line " + line)
            + ": "
            + quote(detail)
            + ". Check the tag or expression at that position.",
        cause);
    this.templateName = templateName;
    this.line = line;
  }

  /**
   * Returns the template containing the error.
   *
   * @return the template name
   */
  public String templateName() {
    return templateName;
  }

  /**
   * Returns the line of the error, if known.
   *
   * @return the 1-based line number
   */
  public Optional<Integer> line() {
    return Optional.ofNullable(line);
  }
}
