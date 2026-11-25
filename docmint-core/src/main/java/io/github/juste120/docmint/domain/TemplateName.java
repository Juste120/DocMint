package io.github.juste120.docmint.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated, normalised template name such as {@code invoice} or {@code layouts/base}.
 *
 * <p>Validation is the first barrier against path traversal: a name can only designate a file below
 * the configured templates root. Accepted names are made of segments separated by {@code /}; each
 * segment contains letters, digits, {@code _} or {@code -}, optionally separated by single dots.
 * The {@code .html} extension is optional and removed during normalisation.
 *
 * @param value the normalised name, without the {@code .html} extension
 */
public record TemplateName(String value) {

  /** Maximum length of a template name, extension excluded. */
  public static final int MAX_LENGTH = 200;

  private static final String EXTENSION = ".html";
  private static final Pattern SEGMENT =
      Pattern.compile("[\\p{L}\\p{N}_-]+(\\.[\\p{L}\\p{N}_-]+)*");

  /**
   * Creates a template name, validating it.
   *
   * @param value the normalised name
   * @throws IllegalArgumentException if the name is invalid
   */
  public TemplateName {
    String reason = violation(value);
    if (reason != null) {
      throw new IllegalArgumentException(reason);
    }
  }

  /**
   * Parses a raw template name, as typed by a developer, into a validated name.
   *
   * @param raw the raw name, with or without the {@code .html} extension
   * @return the validated name
   * @throws IllegalArgumentException if the name is invalid; the message explains why
   */
  public static TemplateName of(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("the template name is null");
    }
    String normalised = raw.strip();
    if (normalised.endsWith(EXTENSION)) {
      normalised = normalised.substring(0, normalised.length() - EXTENSION.length());
    }
    return new TemplateName(normalised);
  }

  private static String violation(String value) {
    if (value == null || value.isBlank()) {
      return "the template name is empty";
    }
    if (value.length() > MAX_LENGTH) {
      return "the template name is longer than " + MAX_LENGTH + " characters";
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isISOControl(c)) {
        return "the template name contains a control character";
      }
      if (c == '\\' || c == ':' || c == '%') {
        return "the template name contains a forbidden character '" + c + "'";
      }
    }
    if (value.startsWith("/")) {
      return "the template name must be relative to the templates folder (no leading '/')";
    }
    for (String segment : value.split("/", -1)) {
      if (segment.equals("..") || segment.equals(".")) {
        return "the template name must not contain '.' or '..' segments";
      }
      if (!SEGMENT.matcher(segment).matches()) {
        return "the template name contains an empty or invalid segment; use letters, digits,"
            + " '_', '-' and '/' only";
      }
    }
    return null;
  }

  /**
   * Returns the relative path of the template file, with its {@code .html} extension.
   *
   * @return for example {@code layouts/base.html}
   */
  public String path() {
    return value + EXTENSION;
  }

  /**
   * Returns the folder part of the name, ending with {@code /}, or an empty string.
   *
   * @return for example {@code layouts/} for {@code layouts/base}
   */
  public String folder() {
    int slash = value.lastIndexOf('/');
    return slash < 0 ? "" : value.substring(0, slash + 1);
  }

  /**
   * Returns the last segment of the name.
   *
   * @return for example {@code base} for {@code layouts/base}
   */
  public String simpleName() {
    return value.substring(value.lastIndexOf('/') + 1);
  }

  /**
   * Returns {@code true} if the raw value is a valid template name.
   *
   * @param raw the raw name
   * @return whether {@link #of(String)} would succeed
   */
  public static boolean isValid(String raw) {
    try {
      of(raw);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return Objects.toString(value);
  }
}
