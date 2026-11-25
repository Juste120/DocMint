package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.TemplateName;
import java.util.Objects;

/**
 * The source text of a template.
 *
 * @param name the template name
 * @param content the template text (HTML with template tags)
 */
public record TemplateSource(TemplateName name, String content) {

  /**
   * Creates a template source.
   *
   * @throws NullPointerException if a value is null
   */
  public TemplateSource {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(content, "content");
  }
}
