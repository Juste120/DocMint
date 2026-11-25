package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.domain.TemplateName;
import java.util.Locale;

/** Builds the public response from a rendered document. */
final class RenderResponseMapper {

  RenderResponse toResponse(RenderedDocument document, TemplateName name, RenderOptions options) {
    String extension = "." + document.format().fileExtension();
    String fileName = options.fileName();
    if (fileName == null || fileName.isBlank()) {
      fileName = name.simpleName() + extension;
    } else if (!fileName.toLowerCase(Locale.ROOT).endsWith(extension)) {
      fileName = fileName + extension;
    }
    return new RenderResponse(
        document.content(), document.format().mediaType(), fileName, document.pageCount());
  }
}
