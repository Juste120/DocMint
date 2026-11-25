package io.github.juste120.docmint.infrastructure.pebble;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.github.juste120.docmint.api.CodeGenerationException;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Template functions {@code qr(...)} and {@code barcode(...)}.
 *
 * <p>The code is computed locally with ZXing and embedded as a PNG {@code data:} URI (sharp black
 * and white modules, scaled without smoothing). The content is only ever encoded as data; the
 * optional human-readable text is HTML-escaped, so user data cannot inject markup.
 */
final class CodeFunctions {

  static final int MAX_QR_CHARS = 2000;
  static final int MAX_BARCODE_CHARS = 80;
  private static final int QR_SCALE = 8;
  private static final int BAR_SCALE = 3;
  private static final int BAR_HEIGHT = 60;
  private static final Pattern SIZE = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,2})?)(mm|cm|in|pt|px)");

  private CodeFunctions() {}

  /** {@code qr(content, size="30mm", level="M")}. */
  static final class Qr implements Function {
    static final String NAME = "qr";

    @Override
    public List<String> getArgumentNames() {
      return List.of("content", "size", "level");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int line) {
      String template = self.getName();
      String content = content(args.get("content"), MAX_QR_CHARS, template, line);
      String size = size(args.getOrDefault("size", "30mm"), template, line);
      ErrorCorrectionLevel level = level(args.getOrDefault("level", "M"), template, line);
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.ERROR_CORRECTION, level);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.MARGIN, 2);
      try {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
        String png = png(matrix, QR_SCALE, QR_SCALE);
        return new SafeString(
            "<img class=\"docmint-qr\" alt=\"QR code\" src=\"data:image/png;base64,"
                + png
                + "\" style=\"width: "
                + size
                + "; height: "
                + size
                + ";\"/>");
      } catch (WriterException | IllegalArgumentException e) {
        throw new CodeGenerationException(
            template, line, "the QR code content is too long or invalid for level " + level);
      }
    }
  }

  /** {@code barcode(content, format="code128", size="50mm", text=false)}. */
  static final class Barcode implements Function {
    static final String NAME = "barcode";

    @Override
    public List<String> getArgumentNames() {
      return List.of("content", "format", "size", "text");
    }

    @Override
    public Object execute(
        Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int line) {
      String template = self.getName();
      String content = content(args.get("content"), MAX_BARCODE_CHARS, template, line);
      String format =
          String.valueOf(args.getOrDefault("format", "code128")).toLowerCase(Locale.ROOT);
      String size = size(args.getOrDefault("size", "50mm"), template, line);
      boolean text = Boolean.parseBoolean(String.valueOf(args.getOrDefault("text", "false")));
      BitMatrix matrix;
      try {
        matrix =
            switch (format) {
              case "code128" ->
                  new Code128Writer().encode(content, BarcodeFormat.CODE_128, 0, BAR_HEIGHT);
              case "ean13" ->
                  new EAN13Writer().encode(content, BarcodeFormat.EAN_13, 0, BAR_HEIGHT);
              default ->
                  throw new CodeGenerationException(
                      template,
                      line,
                      "unknown barcode format '" + escape(format) + "'; supported: code128, ean13");
            };
      } catch (IllegalArgumentException e) {
        throw new CodeGenerationException(
            template,
            line,
            "the content is not valid for "
                + format
                + ("ean13".equals(format)
                    ? " (12 or 13 digits with a correct check digit)"
                    : " (printable ASCII characters)"));
      }
      String png = png(matrix, BAR_SCALE, 1);
      StringBuilder html =
          new StringBuilder(
                  "<span class=\"docmint-barcode\" style=\"display: inline-block; width: ")
              .append(size)
              .append("; text-align: center;\"><img alt=\"Barcode\" src=\"data:image/png;base64,")
              .append(png)
              .append("\" style=\"width: 100%; height: 12mm;\"/>");
      if (text) {
        html.append("<br/><span style=\"font-size: 8pt; letter-spacing: 1pt;\">")
            .append(escape(content))
            .append("</span>");
      }
      return new SafeString(html.append("</span>").toString());
    }
  }

  private static String content(Object value, int max, String template, int line) {
    if (value == null || value.toString().isEmpty()) {
      throw new CodeGenerationException(template, line, "the code content is empty");
    }
    String content = value.toString();
    if (content.length() > max) {
      throw new CodeGenerationException(
          template, line, "the code content is longer than " + max + " characters");
    }
    return content;
  }

  private static String size(Object value, String template, int line) {
    Matcher matcher = SIZE.matcher(String.valueOf(value).strip());
    if (!matcher.matches() || Double.parseDouble(matcher.group(1)) <= 0) {
      throw new CodeGenerationException(
          template, line, "invalid size '" + escape(String.valueOf(value)) + "'; use e.g. 30mm");
    }
    return matcher.group(1) + matcher.group(2);
  }

  private static ErrorCorrectionLevel level(Object value, String template, int line) {
    try {
      return ErrorCorrectionLevel.valueOf(String.valueOf(value).strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new CodeGenerationException(
          template,
          line,
          "invalid QR level '" + escape(String.valueOf(value)) + "'; use L, M, Q or H");
    }
  }

  /** Draws the matrix as a black and white PNG and returns it in Base64. */
  static String png(BitMatrix matrix, int scaleX, int scaleY) {
    int width = matrix.getWidth() * scaleX;
    int height = matrix.getHeight() * scaleY;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, matrix.get(x / scaleX, y / scaleY) ? 0xFF000000 : 0xFFFFFFFF);
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", out);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }

  static String escape(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (char c : text.toCharArray()) {
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        case '\'' -> out.append("&#39;");
        default -> out.append(c);
      }
    }
    return out.toString();
  }
}
