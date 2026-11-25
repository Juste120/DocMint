package io.github.juste120.docmint.api;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

/** Thrown when a template references an allowed resource that cannot be found or read. */
public final class ResourceMissingException extends RenderException {

  @Serial private static final long serialVersionUID = 1L;

  /** Stable error code. */
  public static final String CODE = "DOCMINT_RESOURCE_MISSING";

  private final transient List<String> resources;

  /**
   * Creates the exception.
   *
   * @param templateName the template being rendered
   * @param resources the missing resources
   */
  public ResourceMissingException(String templateName, List<String> resources) {
    super(
        CODE,
        "Template '"
            + quote(templateName)
            + "' references resources that cannot be found or read: "
            + list(resources)
            + ". Check the path (relative to the template) and the file format, or switch"
            + " missing resources to warn mode.",
        null);
    this.resources = List.copyOf(resources);
  }

  /**
   * Returns the missing resources.
   *
   * @return the resource references
   */
  public List<String> resources() {
    return resources == null ? List.of() : resources;
  }

  static String list(List<String> resources) {
    return resources.stream()
        .limit(10)
        .map(r -> "'" + quote(r) + "'")
        .collect(Collectors.joining(", "));
  }
}
