package io.github.juste120.docmint.domain;

/**
 * Kind of document produced by DocMint.
 *
 * <p>Each format is produced by its own {@link io.github.juste120.docmint.port.DocumentRenderer}.
 * Future versions add constants (and renderers) without changing existing ones.
 */
public enum OutputFormat {

  /** Portable Document Format. */
  PDF("application/pdf", "pdf");

  private final String mediaType;
  private final String fileExtension;

  OutputFormat(String mediaType, String fileExtension) {
    this.mediaType = mediaType;
    this.fileExtension = fileExtension;
  }

  /**
   * Returns the IANA media type of this format.
   *
   * @return for example {@code application/pdf}
   */
  public String mediaType() {
    return mediaType;
  }

  /**
   * Returns the usual file extension of this format, without the dot.
   *
   * @return for example {@code pdf}
   */
  public String fileExtension() {
    return fileExtension;
  }
}
