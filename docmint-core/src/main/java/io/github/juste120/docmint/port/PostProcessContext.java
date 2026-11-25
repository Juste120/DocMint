package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.TemplateName;
import java.util.Objects;

/**
 * Information given to {@link PostProcessor}s.
 *
 * @param templateName the rendered template
 * @param requestedFormat the format the caller asked for
 * @param options the document options
 * @param limits the resource limits
 */
public record PostProcessContext(
    TemplateName templateName,
    OutputFormat requestedFormat,
    DocumentOptions options,
    ResourceLimits limits) {

  /**
   * Creates a context.
   *
   * @throws NullPointerException if a value is null
   */
  public PostProcessContext {
    Objects.requireNonNull(templateName, "templateName");
    Objects.requireNonNull(requestedFormat, "requestedFormat");
    Objects.requireNonNull(options, "options");
    Objects.requireNonNull(limits, "limits");
  }
}
