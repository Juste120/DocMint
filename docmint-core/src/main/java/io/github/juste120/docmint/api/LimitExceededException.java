package io.github.juste120.docmint.api;

import java.io.Serial;

/** Thrown when a render exceeds one of the configured resource limits. */
public final class LimitExceededException extends RenderException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_LIMIT_EXCEEDED";

  /** The kind of limit. */
  public enum Limit {
    /** Size of a template file. */
    TEMPLATE_SIZE("max template size", "maxTemplateBytes", "docmint.limits.max-template-size"),
    /** Size of the HTML produced by the template. */
    RENDERED_SIZE(
        "max rendered size (8 x max template size)",
        "maxTemplateBytes",
        "docmint.limits.max-template-size"),
    /** Number of pages of the document. */
    PAGES("max pages", "maxPages", "docmint.limits.max-pages"),
    /** Duration of the render. */
    RENDER_TIME("max render time", "maxRenderTime", "docmint.limits.max-render-time");

    private final String label;
    private final String builderSetting;
    private final String property;

    Limit(String label, String builderSetting, String property) {
      this.label = label;
      this.builderSetting = builderSetting;
      this.property = property;
    }
  }

  private final Limit limit;
  private final String configuredValue;

  /**
   * Creates the exception.
   *
   * @param templateName the template being rendered
   * @param limit the exceeded limit
   * @param configuredValue the configured value, for the message
   */
  public LimitExceededException(String templateName, Limit limit, String configuredValue) {
    super(
        CODE,
        "Template '"
            + quote(templateName)
            + "' exceeded the "
            + limit.label
            + " ("
            + configuredValue
            + "). Reduce the document, or raise the limit with ResourceLimits."
            + limit.builderSetting
            + " or the Spring Boot property '"
            + limit.property
            + "'.",
        null);
    this.limit = limit;
    this.configuredValue = configuredValue;
  }

  /**
   * Returns the exceeded limit.
   *
   * @return the limit
   */
  public Limit limit() {
    return limit;
  }

  /**
   * Returns the configured value of the limit, as text.
   *
   * @return for example {@code 500}
   */
  public String configuredValue() {
    return configuredValue;
  }
}
