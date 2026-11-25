package io.github.juste120.docmint.v2;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Helpers for V2 tests: images in PDFs and code decoding. */
final class V2Support {

  private V2Support() {}

  static List<BufferedImage> imagesOfFirstPage(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDPage page = document.getPage(0);
      List<BufferedImage> images = new ArrayList<>();
      for (COSName name : page.getResources().getXObjectNames()) {
        if (page.getResources().getXObject(name) instanceof PDImageXObject image) {
          images.add(image.getImage());
        }
      }
      return images;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Decodes a QR code or barcode, or returns null. */
  static String decode(BufferedImage image) {
    String pure = decode(image, Map.of(DecodeHintType.PURE_BARCODE, true));
    return pure != null ? pure : decode(frame(image, 40), Map.of(DecodeHintType.TRY_HARDER, true));
  }

  private static String decode(BufferedImage image, Map<DecodeHintType, Object> hints) {
    Map<DecodeHintType, Object> all = new java.util.EnumMap<>(DecodeHintType.class);
    all.putAll(hints);
    all.put(DecodeHintType.CHARACTER_SET, "UTF-8");
    int[] pixels =
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    var source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
    try {
      return new MultiFormatReader()
          .decode(new BinaryBitmap(new HybridBinarizer(source)), all)
          .getText();
    } catch (NotFoundException e) {
      return null;
    }
  }

  /** Adds a white border: decoders need a quiet zone around the code. */
  private static BufferedImage frame(BufferedImage image, int border) {
    BufferedImage framed =
        new BufferedImage(
            image.getWidth() + 2 * border,
            image.getHeight() + 2 * border,
            BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < framed.getHeight(); y++) {
      for (int x = 0; x < framed.getWidth(); x++) {
        boolean inside =
            x >= border
                && y >= border
                && x < border + image.getWidth()
                && y < border + image.getHeight();
        framed.setRGB(x, y, inside ? image.getRGB(x - border, y - border) : 0xFFFFFF);
      }
    }
    return framed;
  }
}
