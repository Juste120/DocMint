package io.github.juste120.docmint.domain;

import java.util.regex.Pattern;

/**
 * A text or image drawn on every page, e.g. {@code BROUILLON}, {@code COPIE} or {@code PAYÉ}.
 *
 * <pre>{@code
 * Watermark.text("COPIE")
 * Watermark.text("ANNULÉ").withColor("#C00000").withOpacity(0.3)
 * Watermark.image("images/paid-stamp.png").withLayer(Watermark.Layer.OVER)
 * }</pre>
 *
 * @param text the text, or {@code null} for an image watermark
 * @param imageLocation the image, relative to the templates folder, or {@code null}
 * @param angle the rotation in degrees (−90 to 90)
 * @param opacity the opacity (0.05 to 1)
 * @param color the text colour, {@code #RRGGBB}
 * @param fontSize the font size in points (8 to 200), or 0 to fit the page diagonal
 * @param layer whether the watermark is drawn behind or over the content
 */
public record Watermark(
    String text,
    String imageLocation,
    double angle,
    double opacity,
    String color,
    double fontSize,
    Layer layer) {

  private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

  /** Where the watermark is drawn. */
  public enum Layer {
    /** Behind the content (default): text stays readable. */
    BEHIND,
    /** Over the content, like a stamp. */
    OVER
  }

  /**
   * Creates a watermark, validating it.
   *
   * @throws IllegalArgumentException if a value is invalid
   */
  public Watermark {
    boolean hasText = text != null && !text.isBlank();
    boolean hasImage = imageLocation != null && !imageLocation.isBlank();
    if (hasText == hasImage) {
      throw new IllegalArgumentException("a watermark needs either a text or an image");
    }
    if (hasText && text.length() > 200) {
      throw new IllegalArgumentException("the watermark text is longer than 200 characters");
    }
    if (hasImage && !TemplateName.isValid(stripExtension(imageLocation))) {
      throw new IllegalArgumentException(
          "the watermark image must be a path inside the templates folder, e.g. images/stamp.png");
    }
    if (angle < -90 || angle > 90) {
      throw new IllegalArgumentException("the watermark angle must be between -90 and 90");
    }
    if (opacity < 0.05 || opacity > 1) {
      throw new IllegalArgumentException("the watermark opacity must be between 0.05 and 1");
    }
    if (color == null || !COLOR.matcher(color).matches()) {
      throw new IllegalArgumentException("the watermark color must be like #808080");
    }
    if (fontSize != 0 && (fontSize < 8 || fontSize > 200)) {
      throw new IllegalArgumentException("the watermark font size must be 0 (auto) or 8 to 200");
    }
    if (layer == null) {
      throw new IllegalArgumentException("the watermark layer is missing");
    }
    text = hasText ? text.strip() : null;
    imageLocation = hasImage ? imageLocation.strip() : null;
  }

  private static String stripExtension(String path) {
    String value = path.strip();
    int dot = value.lastIndexOf('.');
    return dot > value.lastIndexOf('/') && dot > 0 ? value.substring(0, dot) : value;
  }

  /**
   * Creates a text watermark with the defaults: 45°, 15 % opacity, grey, auto size, behind.
   *
   * @param text the text, e.g. {@code COPIE}
   * @return the watermark
   */
  public static Watermark text(String text) {
    return new Watermark(text, null, 45, 0.15, "#808080", 0, Layer.BEHIND);
  }

  /**
   * Creates an image watermark (centred, not rotated, 30 % opacity, behind).
   *
   * @param location the image, relative to the templates folder
   * @return the watermark
   */
  public static Watermark image(String location) {
    return new Watermark(null, location, 0, 0.3, "#808080", 0, Layer.BEHIND);
  }

  /**
   * Returns a copy with another angle.
   *
   * @param degrees −90 to 90
   * @return the new watermark
   */
  public Watermark withAngle(double degrees) {
    return new Watermark(text, imageLocation, degrees, opacity, color, fontSize, layer);
  }

  /**
   * Returns a copy with another opacity.
   *
   * @param value 0.05 to 1
   * @return the new watermark
   */
  public Watermark withOpacity(double value) {
    return new Watermark(text, imageLocation, angle, value, color, fontSize, layer);
  }

  /**
   * Returns a copy with another colour.
   *
   * @param hex e.g. {@code #C00000}
   * @return the new watermark
   */
  public Watermark withColor(String hex) {
    return new Watermark(text, imageLocation, angle, opacity, hex, fontSize, layer);
  }

  /**
   * Returns a copy with a fixed font size.
   *
   * @param points 8 to 200, or 0 for automatic
   * @return the new watermark
   */
  public Watermark withFontSize(double points) {
    return new Watermark(text, imageLocation, angle, opacity, color, points, layer);
  }

  /**
   * Returns a copy drawn on another layer.
   *
   * @param newLayer behind or over the content
   * @return the new watermark
   */
  public Watermark withLayer(Layer newLayer) {
    return new Watermark(text, imageLocation, angle, opacity, color, fontSize, newLayer);
  }
}
