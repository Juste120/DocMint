package io.github.juste120.docmint.domain;

/**
 * Which page of a document to render as an image, and at which resolution.
 *
 * @param page the 1-based page number
 * @param dpi the resolution, from {@value #MIN_DPI} to {@value #MAX_DPI} dots per inch
 */
public record PreviewSettings(int page, int dpi) {

  /** Default resolution (screen). */
  public static final int DEFAULT_DPI = 96;

  /** Minimum resolution. */
  public static final int MIN_DPI = 36;

  /** Maximum resolution. */
  public static final int MAX_DPI = 300;

  /**
   * Creates settings, validating them.
   *
   * @throws IllegalArgumentException if a value is out of range
   */
  public PreviewSettings {
    if (page < 1) {
      throw new IllegalArgumentException("the preview page must be >= 1");
    }
    if (dpi < MIN_DPI || dpi > MAX_DPI) {
      throw new IllegalArgumentException(
          "the preview resolution must be between " + MIN_DPI + " and " + MAX_DPI + " DPI");
    }
  }

  /**
   * Returns the defaults: first page, 96 DPI.
   *
   * @return the default settings
   */
  public static PreviewSettings defaults() {
    return new PreviewSettings(1, DEFAULT_DPI);
  }
}
