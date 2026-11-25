package io.github.juste120.docmint.port;

import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;

/**
 * A step applied to a rendered document, such as a watermark, encryption or a conversion to an
 * image. Post-processors run in the order they are registered; each one decides whether it applies.
 * Implementations must be thread-safe.
 */
public interface PostProcessor {

  /**
   * Tells whether this step applies.
   *
   * @param document the document produced so far
   * @param context the render context
   * @return {@code true} to run {@link #process}
   */
  boolean appliesTo(RenderedDocument document, PostProcessContext context);

  /**
   * Transforms the document.
   *
   * @param document the document produced so far
   * @param context the render context
   * @return the new document (possibly in another format)
   * @throws io.github.juste120.docmint.api.DocMintException on error
   */
  RenderedDocument process(RenderedDocument document, PostProcessContext context);

  /**
   * Returns {@code true} when the document has the given format.
   *
   * @param document a document
   * @param format a format
   * @return whether they match
   */
  static boolean is(RenderedDocument document, OutputFormat format) {
    return document.format() == format;
  }
}
