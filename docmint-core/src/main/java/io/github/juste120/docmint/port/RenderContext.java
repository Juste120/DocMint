package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.TemplateName;
import java.util.Locale;
import java.util.Objects;

/**
 * Per-render information given to a {@link DocumentRenderer}.
 *
 * @param templateName the rendered template, used to resolve relative resources and in errors
 * @param locale the locale of the render
 * @param limits the resource limits
 * @param options the document options (watermark, protection, preview)
 */
public record RenderContext(
    TemplateName templateName, Locale locale, ResourceLimits limits, DocumentOptions options) {

  /**
   * Creates a render context.
   *
   * @throws NullPointerException if a value is null
   */
  public RenderContext {
    Objects.requireNonNull(templateName, "templateName");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(limits, "limits");
    options = options == null ? DocumentOptions.none() : options;
  }

  /**
   * Creates a context without document options (V1 constructor).
   *
   * @param templateName the rendered template
   * @param locale the locale
   * @param limits the resource limits
   */
  public RenderContext(TemplateName templateName, Locale locale, ResourceLimits limits) {
    this(templateName, locale, limits, DocumentOptions.none());
  }
}
