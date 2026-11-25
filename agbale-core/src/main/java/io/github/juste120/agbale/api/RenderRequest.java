package io.github.juste120.agbale.api;

import io.github.juste120.agbale.domain.OutputFormat;
import java.util.Objects;

/**
 * A render request for the advanced API ({@link Agbale#render(RenderRequest)}).
 *
 * @param templateName the template name, relative to the templates folder, e.g. {@code invoice}
 * @param data the data exposed to the template: a record, an object with getters or a map
 * @param format the output format
 * @param options the per-render options
 */
public record RenderRequest(
    String templateName, Object data, OutputFormat format, RenderOptions options) {

  /**
   * Creates a request.
   *
   * @throws NullPointerException if data is null
   */
  public RenderRequest {
    Objects.requireNonNull(
        data, "data must not be null: pass an empty Map if the template needs no data");
    format = format == null ? OutputFormat.PDF : format;
    options = options == null ? RenderOptions.defaults() : options;
  }

  /**
   * Creates a PDF request with default options.
   *
   * @param templateName the template name
   * @param data the data
   * @return the request
   */
  public static RenderRequest of(String templateName, Object data) {
    return new RenderRequest(templateName, data, OutputFormat.PDF, RenderOptions.defaults());
  }

  /**
   * Returns a copy with other options.
   *
   * @param newOptions the options
   * @return the new request
   */
  public RenderRequest withOptions(RenderOptions newOptions) {
    return new RenderRequest(templateName, data, format, newOptions);
  }
}
