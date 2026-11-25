package io.github.juste120.agbale.port;

import io.github.juste120.agbale.domain.ResourceLimits;
import io.github.juste120.agbale.domain.TemplateName;
import java.util.Locale;
import java.util.Objects;

/**
 * Per-render information given to a {@link DocumentRenderer}.
 *
 * @param templateName the rendered template, used to resolve relative resources and in errors
 * @param locale the locale of the render
 * @param limits the resource limits
 */
public record RenderContext(TemplateName templateName, Locale locale, ResourceLimits limits) {

  /**
   * Creates a render context.
   *
   * @throws NullPointerException if a value is null
   */
  public RenderContext {
    Objects.requireNonNull(templateName, "templateName");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(limits, "limits");
  }
}
