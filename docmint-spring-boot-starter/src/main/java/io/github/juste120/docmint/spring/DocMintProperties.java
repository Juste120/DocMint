package io.github.juste120.docmint.spring;

import io.github.juste120.docmint.domain.MissingResourceMode;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties of DocMint ({@code docmint.*}). Every property is optional.
 *
 * @param templatesPath class path folder of the templates
 * @param cache template cache settings
 * @param locale default locale for dates and amounts
 * @param fonts additional fonts loaded from the class path
 * @param limits resource limits
 * @param resources resource loading settings
 * @param web web settings for {@code @Report} endpoints
 * @param watermark default watermark applied to every document (none by default)
 */
@ConfigurationProperties("docmint")
public record DocMintProperties(
    @DefaultValue("reports") String templatesPath,
    @DefaultValue Cache cache,
    @DefaultValue("fr") Locale locale,
    List<Font> fonts,
    @DefaultValue Limits limits,
    @DefaultValue Resources resources,
    @DefaultValue Web web,
    @DefaultValue WatermarkDefaults watermark) {

  /**
   * Creates the properties, replacing a missing font list by an empty list.
   *
   * @param templatesPath class path folder of the templates
   * @param cache template cache settings
   * @param locale default locale
   * @param fonts additional fonts
   * @param limits resource limits
   * @param resources resource loading settings
   * @param web web settings
   * @param watermark default watermark
   */
  public DocMintProperties {
    fonts = fonts == null ? List.of() : List.copyOf(fonts);
  }

  /**
   * Template cache settings.
   *
   * @param enabled whether compiled templates are cached
   */
  public record Cache(@DefaultValue("true") boolean enabled) {}

  /**
   * An additional font.
   *
   * @param family the CSS family name
   * @param location the class path location of the TrueType file
   * @param weight the CSS weight (100 to 900)
   * @param italic whether the file is the italic style
   */
  public record Font(
      String family, String location, @DefaultValue("400") int weight, boolean italic) {}

  /**
   * Resource limits.
   *
   * @param maxTemplateSize maximum size of each template file
   * @param maxPages maximum number of pages of a document
   * @param maxRenderTime maximum duration of a render
   * @param maxImagePixels maximum number of pixels of a PNG preview
   * @param maxExportRows maximum number of rows of a CSV or Excel export
   */
  public record Limits(
      @DefaultValue("1MB") DataSize maxTemplateSize,
      @DefaultValue("500") int maxPages,
      @DefaultValue("30s") Duration maxRenderTime,
      @DefaultValue("25000000") long maxImagePixels,
      @DefaultValue("100000") int maxExportRows) {}

  /**
   * Default watermark, e.g. {@code TEST} on a staging server.
   *
   * @param text the text; empty (default) for no watermark
   * @param angle the angle in degrees
   * @param opacity the opacity, 0.05 to 1
   * @param color the colour, {@code #RRGGBB}
   */
  public record WatermarkDefaults(
      String text,
      @DefaultValue("45") double angle,
      @DefaultValue("0.15") double opacity,
      @DefaultValue("#808080") String color) {}

  /**
   * Resource loading settings.
   *
   * @param allowedHosts https hosts allowed for remote images, stylesheets and fonts (exact host or
   *     {@code *.domain}); empty by default, which forbids every remote resource
   * @param missing what to do with missing resources: {@code fail} (default) or {@code warn}
   */
  public record Resources(
      List<String> allowedHosts, @DefaultValue("fail") MissingResourceMode missing) {

    /**
     * Creates the settings, replacing a missing host list by an empty list.
     *
     * @param allowedHosts allowed hosts
     * @param missing missing resource mode
     */
    public Resources {
      allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
    }
  }

  /**
   * Web settings.
   *
   * @param problemDetails RFC 9457 error responses for DocMint errors
   */
  public record Web(@DefaultValue ProblemDetails problemDetails) {}

  /**
   * RFC 9457 error responses.
   *
   * @param enabled whether DocMint errors are turned into {@code application/problem+json}
   */
  public record ProblemDetails(@DefaultValue("true") boolean enabled) {}
}
