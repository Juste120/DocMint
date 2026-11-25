package io.github.juste120.docmint.domain;

/**
 * Kind of document produced by DocMint.
 *
 * <p>Each format is produced by its own {@link io.github.juste120.docmint.port.DocumentRenderer}.
 * Future versions add constants (and renderers) without changing existing ones.
 */
public enum OutputFormat {

  /** Portable Document Format. */
  PDF("application/pdf", "pdf", null),

  /** PNG image of one page, produced from the PDF (preview). */
  PNG("image/png", "png", PDF),

  /** The HTML produced by the template, before any conversion. */
  HTML("text/html", "html", null),

  /** Comma (or semicolon) separated values, produced from a table of the template. */
  CSV("text/csv", "csv", HTML),

  /** Excel workbook, produced from the tables of the template (module {@code docmint-excel}). */
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", HTML),

  /** Word document, produced from the HTML of the template (module {@code docmint-word}). */
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", HTML);

  private final String mediaType;
  private final String fileExtension;
  private final OutputFormat baseFormat;

  OutputFormat(String mediaType, String fileExtension, OutputFormat baseFormat) {
    this.mediaType = mediaType;
    this.fileExtension = fileExtension;
    this.baseFormat = baseFormat;
  }

  /**
   * Returns the format rendered first, before conversion to this format: {@code PDF} for {@code
   * PNG}, the format itself otherwise.
   *
   * @return the base format
   */
  public OutputFormat baseFormat() {
    return baseFormat == null ? this : baseFormat;
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
