package io.github.juste120.agbale.api;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

/** Thrown when a template (or a layout or include it references) does not exist. */
public final class TemplateNotFoundException extends AgbaleException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "AGBALE_TEMPLATE_NOT_FOUND";

  private static final int MAX_LISTED = 20;

  private final String templateName;
  private final transient List<String> availableTemplates;
  private final String suggestion;

  /**
   * Creates the exception.
   *
   * @param templateName the requested name
   * @param templatesPath the configured templates root (a class path location, not a file path)
   * @param availableTemplates the existing templates
   * @param suggestion the closest existing name, or {@code null}
   */
  public TemplateNotFoundException(
      String templateName,
      String templatesPath,
      List<String> availableTemplates,
      String suggestion) {
    super(CODE, message(templateName, templatesPath, availableTemplates, suggestion), null);
    this.templateName = templateName;
    this.availableTemplates = List.copyOf(availableTemplates);
    this.suggestion = suggestion;
  }

  private static String message(
      String name, String root, List<String> available, String suggestion) {
    StringBuilder message =
        new StringBuilder("Template '")
            .append(quote(name))
            .append("' not found in class path folder '")
            .append(quote(root))
            .append("'.");
    if (available.isEmpty()) {
      message.append(" No template was found there: put your .html files in src/main/resources/")
          .append(quote(root))
          .append("/ or configure another templates path.");
    } else {
      message.append(" Available templates: ");
      message.append(String.join(", ", available.subList(0, Math.min(MAX_LISTED, available.size()))));
      if (available.size() > MAX_LISTED) {
        message.append(", … (").append(available.size() - MAX_LISTED).append(" more)");
      }
      message.append('.');
    }
    if (suggestion != null) {
      message.append(" Did you mean '").append(suggestion).append("'?");
    }
    return message.toString();
  }

  /**
   * Returns the requested template name.
   *
   * @return the name
   */
  public String templateName() {
    return templateName;
  }

  /**
   * Returns the templates found under the templates root.
   *
   * @return the available template names, sorted
   */
  public List<String> availableTemplates() {
    return availableTemplates == null ? List.of() : availableTemplates;
  }

  /**
   * Returns the closest existing template name, if one is similar to the requested name.
   *
   * @return the suggestion
   */
  public Optional<String> suggestion() {
    return Optional.ofNullable(suggestion);
  }
}
