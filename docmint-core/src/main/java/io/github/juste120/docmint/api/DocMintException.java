package io.github.juste120.docmint.api;

import java.io.Serial;

/**
 * Root of every error raised by DocMint.
 *
 * <p>Each subtype has a stable {@link #code() code} (for example {@code
 * DOCMINT_TEMPLATE_NOT_FOUND}) that applications and HTTP clients can rely on. Messages explain
 * what to do next; they never contain absolute paths, stack traces or data values.
 */
public abstract class DocMintException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String code;

  /**
   * Creates an exception.
   *
   * @param code the stable error code
   * @param message the actionable message
   * @param cause the cause, may be {@code null}
   */
  protected DocMintException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  /**
   * Returns the stable error code of this exception.
   *
   * @return for example {@code DOCMINT_TEMPLATE_NOT_FOUND}
   */
  public String code() {
    return code;
  }

  /**
   * Makes a value safe to quote inside an error message: control characters are removed and the
   * value is truncated to 100 characters.
   *
   * @param value the value to quote
   * @return the safe value
   */
  static String quote(String value) {
    if (value == null) {
      return "null";
    }
    StringBuilder safe = new StringBuilder();
    value
        .codePoints()
        .filter(cp -> !Character.isISOControl(cp))
        .limit(100)
        .forEach(safe::appendCodePoint);
    return safe.toString();
  }
}
