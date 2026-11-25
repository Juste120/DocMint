package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;

/**
 * Converts HTML into a document of one {@link OutputFormat} (Strategy pattern: one renderer per
 * format). Implementations must be thread-safe.
 */
public interface DocumentRenderer {

  /**
   * Returns the format this renderer produces.
   *
   * @return the format
   */
  OutputFormat format();

  /**
   * Renders HTML into a document.
   *
   * @param html the HTML produced by the template engine
   * @param context the render context
   * @return the document
   * @throws io.github.juste120.docmint.api.DocMintException on any render error
   */
  RenderedDocument render(String html, RenderContext context);
}
