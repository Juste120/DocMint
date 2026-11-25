package io.github.juste120.docmint.infrastructure.export;

import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.port.DocumentRenderer;
import io.github.juste120.docmint.port.RenderContext;
import java.nio.charset.StandardCharsets;

/** Returns the HTML produced by the template, as UTF-8 bytes (base of CSV and Excel exports). */
public final class HtmlDocumentRenderer implements DocumentRenderer {

  @Override
  public OutputFormat format() {
    return OutputFormat.HTML;
  }

  @Override
  public RenderedDocument render(String html, RenderContext context) {
    return new RenderedDocument(html.getBytes(StandardCharsets.UTF_8), OutputFormat.HTML, 1);
  }
}
