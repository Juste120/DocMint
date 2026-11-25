package io.github.juste120.docmint.infrastructure.pdf;

import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.PreviewSettings;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Converts one page of a PDF into a PNG image (preview). The image size is checked against the
 * pixel limit before rasterising.
 */
public final class PngPreviewPostProcessor implements PostProcessor {

  @Override
  public boolean appliesTo(RenderedDocument document, PostProcessContext context) {
    return PostProcessor.is(document, OutputFormat.PDF)
        && context.requestedFormat() == OutputFormat.PNG;
  }

  @Override
  public RenderedDocument process(RenderedDocument document, PostProcessContext context) {
    PreviewSettings settings = context.options().previewOrDefault();
    String template = context.templateName().value();
    try (PDDocument pdf = Loader.loadPDF(document.content())) {
      int pages = pdf.getNumberOfPages();
      if (settings.page() > pages) {
        throw new InvalidOptionsException(
            InvalidOptionsException.PREVIEW_PAGE_OUT_OF_RANGE,
            "Preview of page "
                + settings.page()
                + " requested, but template '"
                + template
                + "' produced "
                + pages
                + " page(s).");
      }
      PDRectangle box = pdf.getPage(settings.page() - 1).getMediaBox();
      long width = Math.round(box.getWidth() / 72.0 * settings.dpi());
      long height = Math.round(box.getHeight() / 72.0 * settings.dpi());
      long maxPixels = context.limits().maxImagePixels();
      if (width * height > maxPixels) {
        throw new LimitExceededException(
            template, LimitExceededException.Limit.IMAGE_SIZE, maxPixels + " pixels");
      }
      BufferedImage image =
          new PDFRenderer(pdf)
              .renderImageWithDPI(settings.page() - 1, settings.dpi(), ImageType.RGB);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return new RenderedDocument(out.toByteArray(), OutputFormat.PNG, 1);
    } catch (IOException e) {
      throw new RenderException("The preview image could not be produced.", e);
    }
  }
}
