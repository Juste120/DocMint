package io.github.juste120.agbale.domain;

import java.util.Objects;

/**
 * A font file to embed, loaded from the class path.
 *
 * @param family the CSS font family name, as used in {@code font-family}
 * @param classpathLocation the class path location of the TrueType file, e.g. {@code
 *     fonts/Inter-Regular.ttf}
 * @param weight the CSS weight, from 100 to 900
 * @param italic whether this file is the italic style
 */
public record FontDefinition(String family, String classpathLocation, int weight, boolean italic) {

  /** Regular CSS weight. */
  public static final int NORMAL = 400;

  /** Bold CSS weight. */
  public static final int BOLD = 700;

  /**
   * Creates a font definition, validating it.
   *
   * @throws IllegalArgumentException if a value is invalid
   */
  public FontDefinition {
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(classpathLocation, "classpathLocation");
    if (family.isBlank()) {
      throw new IllegalArgumentException("the font family is empty");
    }
    if (classpathLocation.isBlank()
        || classpathLocation.contains("..")
        || classpathLocation.contains(":")
        || classpathLocation.contains("\\")) {
      throw new IllegalArgumentException(
          "the font location must be a class path location such as 'fonts/MyFont.ttf'");
    }
    if (weight < 100 || weight > 900) {
      throw new IllegalArgumentException("the font weight must be between 100 and 900");
    }
    classpathLocation =
        classpathLocation.startsWith("/") ? classpathLocation.substring(1) : classpathLocation;
  }

  /**
   * Creates a regular (weight 400, not italic) font definition.
   *
   * @param family the CSS font family
   * @param classpathLocation the class path location of the TrueType file
   * @return the definition
   */
  public static FontDefinition regular(String family, String classpathLocation) {
    return new FontDefinition(family, classpathLocation, NORMAL, false);
  }
}
