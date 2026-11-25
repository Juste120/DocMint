package io.github.juste120.agbale.spring;

import io.github.juste120.agbale.domain.MissingResourceMode;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties of Agbale ({@code agbale.*}). Every property is optional.
 *
 * @param templatesPath class path folder of the templates
 * @param cache template cache settings
 * @param locale default locale for dates and amounts
 * @param fonts additional fonts loaded from the class path
 * @param limits resource limits
 * @param resources resource loading settings
 * @param web web settings for {@code @Report} endpoints
 */
@ConfigurationProperties("agbale")
public record AgbaleProperties(
    @DefaultValue("reports") String templatesPath,
    @DefaultValue Cache cache,
    @DefaultValue("fr") Locale locale,
    List<Font> fonts,
    @DefaultValue Limits limits,
    @DefaultValue Resources resources,
    @DefaultValue Web web) {

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
   */
  public AgbaleProperties {
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
   */
  public record Limits(
      @DefaultValue("1MB") DataSize maxTemplateSize,
      @DefaultValue("500") int maxPages,
      @DefaultValue("30s") Duration maxRenderTime) {}

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
   * @param problemDetails RFC 9457 error responses for Agbale errors
   */
  public record Web(@DefaultValue ProblemDetails problemDetails) {}

  /**
   * RFC 9457 error responses.
   *
   * @param enabled whether Agbale errors are turned into {@code application/problem+json}
   */
  public record ProblemDetails(@DefaultValue("true") boolean enabled) {}
}
