package io.github.juste120.docmint.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * The result of the advanced API.
 *
 * @param content the document bytes
 * @param mediaType the media type, e.g. {@code application/pdf}
 * @param fileName the suggested file name, e.g. {@code invoice.pdf}
 * @param pageCount the number of pages
 */
public record RenderResponse(byte[] content, String mediaType, String fileName, int pageCount) {

  /**
   * Creates a response, copying the content.
   *
   * @throws NullPointerException if a value is null
   */
  public RenderResponse {
    Objects.requireNonNull(content, "content");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(fileName, "fileName");
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
    return other instanceof RenderResponse that
        && Arrays.equals(content, that.content)
        && mediaType.equals(that.mediaType)
        && fileName.equals(that.fileName)
        && pageCount == that.pageCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(content), mediaType, fileName, pageCount);
  }

  @Override
  public String toString() {
    return "RenderResponse[mediaType="
        + mediaType
        + ", fileName="
        + fileName
        + ", pageCount="
        + pageCount
        + ", size="
        + content.length
        + "]";
  }
}
