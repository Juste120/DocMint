package io.github.juste120.docmint.infrastructure.pdf;

import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.domain.TemplateName;
import io.github.juste120.docmint.domain.Watermark;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/**
 * Draws a text or image watermark on every page of a PDF, behind or over the content.
 *
 * <p>Text uses the embedded Noto Sans font (so {@code PAYÉ} or {@code ANNULÉ} render correctly) and
 * is written as plain text: it can never be interpreted as markup. Images are read from the
 * templates folder only.
 */
public final class PdfWatermarkPostProcessor implements PostProcessor {

  private static final float DIAGONAL_RATIO = 0.75f;
  private static final float MAX_AUTO_SIZE = 140f;

  private final String root;
  private final ClassLoader classLoader;

  /**
   * Creates the post-processor.
   *
   * @param root the templates folder on the class path (image watermarks are read below it)
   * @param classLoader the class loader to read images from
   */
  public PdfWatermarkPostProcessor(String root, ClassLoader classLoader) {
    this.root = Objects.requireNonNull(root, "root");
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
  }

  @Override
  public boolean appliesTo(RenderedDocument document, PostProcessContext context) {
    return PostProcessor.is(document, OutputFormat.PDF) && context.options().watermark() != null;
  }

  @Override
  public RenderedDocument process(RenderedDocument document, PostProcessContext context) {
    Watermark watermark = context.options().watermark();
    try (PDDocument pdf = Loader.loadPDF(document.content())) {
      PDFont font =
          watermark.text() == null
              ? null
              : PDType0Font.load(pdf, new ByteArrayInputStream(FontRegistry.defaultRegularFont()));
      PDImageXObject image =
          watermark.imageLocation() == null ? null : image(pdf, watermark.imageLocation());
      PDExtendedGraphicsState transparency = new PDExtendedGraphicsState();
      transparency.setNonStrokingAlphaConstant((float) watermark.opacity());
      transparency.setStrokingAlphaConstant((float) watermark.opacity());
      AppendMode mode =
          watermark.layer() == Watermark.Layer.BEHIND ? AppendMode.PREPEND : AppendMode.APPEND;
      for (PDPage page : pdf.getPages()) {
        try (PDPageContentStream content = new PDPageContentStream(pdf, page, mode, true, true)) {
          content.saveGraphicsState();
          content.setGraphicsStateParameters(transparency);
          if (font != null) {
            drawText(content, page.getMediaBox(), font, watermark);
          } else {
            drawImage(content, page.getMediaBox(), image, watermark);
          }
          content.restoreGraphicsState();
        }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      pdf.save(out);
      return new RenderedDocument(out.toByteArray(), OutputFormat.PDF, document.pageCount());
    } catch (IllegalArgumentException e) {
      throw new InvalidOptionsException(
          InvalidOptionsException.WATERMARK_INVALID,
          "The watermark text contains characters the embedded font cannot draw. Use letters,"
              + " digits and common punctuation.");
    } catch (IOException e) {
      throw new RenderException("The watermark could not be applied.", e);
    }
  }

  private static void drawText(
      PDPageContentStream content, PDRectangle box, PDFont font, Watermark watermark)
      throws IOException {
    String text = watermark.text();
    float unitWidth = font.getStringWidth(text) / 1000f;
    float size = (float) watermark.fontSize();
    if (size == 0) {
      double diagonal = Math.hypot(box.getWidth(), box.getHeight());
      size = (float) Math.min(MAX_AUTO_SIZE, diagonal * DIAGONAL_RATIO / Math.max(unitWidth, 0.1f));
    }
    float width = unitWidth * size;
    int rgb = Integer.parseInt(watermark.color().substring(1), 16);
    content.setNonStrokingColor(
        ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
    Matrix matrix =
        Matrix.getRotateInstance(
            Math.toRadians(watermark.angle()),
            box.getLowerLeftX() + box.getWidth() / 2,
            box.getLowerLeftY() + box.getHeight() / 2);
    matrix.translate(-width / 2, -size * 0.35f);
    content.beginText();
    content.setFont(font, size);
    content.setTextMatrix(matrix);
    content.showText(text);
    content.endText();
  }

  private static void drawImage(
      PDPageContentStream content, PDRectangle box, PDImageXObject image, Watermark watermark)
      throws IOException {
    float width = box.getWidth() * 0.5f;
    float height = width * image.getHeight() / image.getWidth();
    content.transform(
        Matrix.getRotateInstance(
            Math.toRadians(watermark.angle()),
            box.getLowerLeftX() + box.getWidth() / 2,
            box.getLowerLeftY() + box.getHeight() / 2));
    content.drawImage(image, -width / 2, -height / 2, width, height);
  }

  private PDImageXObject image(PDDocument pdf, String location) throws IOException {
    String relative = location.startsWith("/") ? location.substring(1) : location;
    boolean safe = TemplateName.isValid(relative.replace('.', '_')) && !relative.contains("..");
    try (InputStream in = safe ? classLoader.getResourceAsStream(root + "/" + relative) : null) {
      if (in == null) {
        throw new InvalidOptionsException(
            InvalidOptionsException.WATERMARK_INVALID,
            "Watermark image '"
                + location
                + "' was not found in the templates folder. Use a path such as"
                + " images/stamp.png.");
      }
      return PDImageXObject.createFromByteArray(pdf, in.readAllBytes(), relative);
    }
  }
}
