package io.github.juste120.docmint.domain;

/**
 * Options applied to a rendered document by post-processors.
 *
 * @param watermark the watermark, or {@code null}
 * @param protection the password protection, or {@code null}
 * @param preview the preview settings for image formats, or {@code null} for the defaults
 */
public record DocumentOptions(Watermark watermark, Protection protection, PreviewSettings preview) {

  /**
   * Returns options without watermark, protection or preview settings.
   *
   * @return empty options
   */
  public static DocumentOptions none() {
    return new DocumentOptions(null, null, null);
  }

  /**
   * Returns the preview settings, or the defaults.
   *
   * @return never null
   */
  public PreviewSettings previewOrDefault() {
    return preview == null ? PreviewSettings.defaults() : preview;
  }
}
