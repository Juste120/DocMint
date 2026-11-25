package io.github.juste120.docmint.domain;

/**
 * CSV dialect.
 *
 * <p>The defaults ({@code ;} separator and a UTF-8 byte order mark) let Excel in French open the
 * file with the right columns and accents. Use {@link #rfc4180()} for {@code ,} without BOM.
 *
 * @param separator the field separator: {@code ;}, {@code ,}, tab or {@code |}
 * @param byteOrderMark whether to start the file with a UTF-8 BOM
 */
public record CsvSettings(char separator, boolean byteOrderMark) {

  /**
   * Creates settings, validating the separator.
   *
   * @throws IllegalArgumentException if the separator is not supported
   */
  public CsvSettings {
    if (";,\t|".indexOf(separator) < 0) {
      throw new IllegalArgumentException("the CSV separator must be ';', ',', tab or '|'");
    }
  }

  /**
   * Returns the defaults: {@code ;} and BOM (Excel in French).
   *
   * @return the default settings
   */
  public static CsvSettings defaults() {
    return new CsvSettings(';', true);
  }

  /**
   * Returns the RFC 4180 dialect: {@code ,} without BOM.
   *
   * @return the settings
   */
  public static CsvSettings rfc4180() {
    return new CsvSettings(',', false);
  }
}
