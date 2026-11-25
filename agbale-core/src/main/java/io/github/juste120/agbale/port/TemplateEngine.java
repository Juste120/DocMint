package io.github.juste120.agbale.port;

import io.github.juste120.agbale.domain.TemplateName;
import java.util.Locale;
import java.util.Map;

/** Turns a template and a data model into HTML. Implementations must be thread-safe. */
public interface TemplateEngine {

  /**
   * Renders a template to HTML.
   *
   * @param name the template name
   * @param model the template variables (already converted to safe values)
   * @param locale the locale for dates and amounts
   * @return the HTML document
   * @throws io.github.juste120.agbale.api.AgbaleException on any template error
   */
  String render(TemplateName name, Map<String, Object> model, Locale locale);

  /** Removes every compiled template from the cache. */
  void clearCache();
}
