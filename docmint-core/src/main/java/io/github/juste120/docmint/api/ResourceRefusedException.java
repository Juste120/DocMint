package io.github.juste120.docmint.api;

import java.io.Serial;
import java.util.List;

/**
 * Thrown when a template references a resource from a forbidden location (remote host not
 * allow-listed, {@code http}, {@code file}, ...). This is a security protection and cannot be
 * turned into a warning.
 */
public final class ResourceRefusedException extends RenderException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_RESOURCE_REFUSED";

  private final transient List<String> resources;

  /**
   * Creates the exception.
   *
   * @param templateName the template being rendered
   * @param resources the refused resource URIs
   */
  public ResourceRefusedException(String templateName, List<String> resources) {
    super(
        CODE,
        "Template '"
            + quote(templateName)
            + "' references resources that are not allowed: "
            + ResourceMissingException.list(resources)
            + ". Put images, stylesheets and fonts in the templates folder, use data: URIs, or"
            + " allow-list an https host explicitly.",
        null);
    this.resources = List.copyOf(resources);
  }

  /**
   * Returns the refused resources.
   *
   * @return the URIs
   */
  public List<String> resources() {
    return resources == null ? List.of() : resources;
  }
}
