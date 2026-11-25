package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.port.DocumentRenderer;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Selects the renderer of an output format (Strategy pattern). */
public final class RendererRegistry {

  private final Map<OutputFormat, DocumentRenderer> renderers = new EnumMap<>(OutputFormat.class);

  /**
   * Creates a registry. A later renderer for the same format replaces an earlier one.
   *
   * @param renderers the renderers
   */
  public RendererRegistry(Collection<DocumentRenderer> renderers) {
    for (DocumentRenderer renderer : renderers) {
      this.renderers.put(renderer.format(), renderer);
    }
  }

  /**
   * Returns the renderer of a format.
   *
   * @param format the format
   * @return the renderer
   * @throws RenderException if no renderer supports the format
   */
  public DocumentRenderer get(OutputFormat format) {
    DocumentRenderer renderer = renderers.get(format);
    if (renderer == null) {
      throw new RenderException(
          "No renderer is registered for format " + format + ". Supported: " + renderers.keySet(),
          null);
    }
    return renderer;
  }
}
