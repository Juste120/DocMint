package io.github.juste120.agbale.domain;

import java.util.Objects;

/**
 * Default page layout applied by Agbale's print stylesheet. Template CSS can override every value
 * with a standard {@code @page} rule.
 *
 * @param size the page size
 * @param orientation the page orientation
 * @param marginTopMm top margin in millimetres
 * @param marginRightMm right margin in millimetres
 * @param marginBottomMm bottom margin in millimetres
 * @param marginLeftMm left margin in millimetres
 */
public record PageSettings(
    PageSize size,
    Orientation orientation,
    double marginTopMm,
    double marginRightMm,
    double marginBottomMm,
    double marginLeftMm) {

  /** Supported paper sizes. */
  public enum PageSize {
    /** ISO A4, 210 × 297 mm. */
    A4,
    /** ISO A5, 148 × 210 mm. */
    A5,
    /** ISO A3, 297 × 420 mm. */
    A3,
    /** US Letter, 8.5 × 11 in. */
    LETTER
  }

  /** Page orientation. */
  public enum Orientation {
    /** Taller than wide. */
    PORTRAIT,
    /** Wider than tall. */
    LANDSCAPE
  }

  /**
   * Creates page settings, validating them.
   *
   * @throws NullPointerException if size or orientation is null
   * @throws IllegalArgumentException if a margin is negative
   */
  public PageSettings {
    Objects.requireNonNull(size, "size");
    Objects.requireNonNull(orientation, "orientation");
    if (marginTopMm < 0 || marginRightMm < 0 || marginBottomMm < 0 || marginLeftMm < 0) {
      throw new IllegalArgumentException("page margins must not be negative");
    }
  }

  /**
   * Returns the defaults: A4 portrait, margins 20 mm (top, bottom) and 15 mm (left, right).
   *
   * @return the default page settings
   */
  public static PageSettings defaults() {
    return new PageSettings(PageSize.A4, Orientation.PORTRAIT, 20, 15, 20, 15);
  }
}
