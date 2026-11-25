package io.github.juste120.docmint.word;

import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import java.nio.charset.StandardCharsets;

/** Converts the HTML of a template into a Word document when {@code DOCX} is requested. */
public final class DocxPostProcessor implements PostProcessor {

  @Override
  public boolean appliesTo(RenderedDocument document, PostProcessContext context) {
    return PostProcessor.is(document, OutputFormat.HTML)
        && context.requestedFormat() == OutputFormat.DOCX;
  }

  @Override
  public RenderedDocument process(RenderedDocument document, PostProcessContext context) {
    byte[] docx = new HtmlToDocx().convert(new String(document.content(), StandardCharsets.UTF_8));
    return new RenderedDocument(docx, OutputFormat.DOCX, 1);
  }
}
