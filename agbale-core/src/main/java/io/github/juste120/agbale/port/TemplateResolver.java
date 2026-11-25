package io.github.juste120.agbale.port;

import io.github.juste120.agbale.domain.TemplateName;
import java.util.List;
import java.util.Optional;

/**
 * Finds template sources by name. Implementations must never read outside their own root.
 *
 * <p>Implementations must be thread-safe.
 */
public interface TemplateResolver {

  /**
   * Finds a template.
   *
   * @param name a validated template name
   * @return the template source, or empty if it does not exist
   * @throws io.github.juste120.agbale.api.LimitExceededException if the template is too large
   */
  Optional<TemplateSource> resolve(TemplateName name);

  /**
   * Lists the available template names (without extension), used to build helpful errors.
   *
   * @return the names, sorted; may be empty if listing is not supported
   */
  List<String> availableTemplates();

  /**
   * Describes where templates are looked up, for error messages. Must not be an absolute file
   * system path.
   *
   * @return for example {@code reports}
   */
  String location();
}
