package io.github.juste120.agbale.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * A document produced by a renderer.
 *
 * @param content the document bytes
 * @param format the format of the content
 * @param pageCount the number of pages (or 1 for formats without pages)
 */
public record RenderedDocument(byte[] content, OutputFormat format, int pageCount) {

  /**
   * Creates a rendered document, copying the content.
   *
   * @throws NullPointerException if content or format is null
   */
  public RenderedDocument {
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(format, "format");
    content = content.clone();
  }

  /**
   * Returns a copy of the document bytes.
   *
   * @return the content
   */
  @Override
  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof RenderedDocument that
        && Arrays.equals(content, that.content)
        && format == that.format
        && pageCount == that.pageCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(content), format, pageCount);
  }

  @Override
  public String toString() {
    return "RenderedDocument[format="
        + format
        + ", pageCount="
        + pageCount
        + ", size="
        + content.length
        + "]";
  }
}
