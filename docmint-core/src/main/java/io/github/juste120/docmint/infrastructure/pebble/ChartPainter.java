package io.github.juste120.docmint.infrastructure.pebble;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Draws bar, line, pie and doughnut charts with Java2D, using the embedded Noto Sans font, and
 * encodes them as PNG. Charts are drawn at three times their CSS size (about 288 DPI) so that they
 * stay sharp when printed.
 */
final class ChartPainter {

  /** Chart types. */
  enum Type {
    BAR,
    LINE,
    PIE,
    DOUGHNUT
  }

  /** A named series of values. */
  record Series(String name, List<Double> values) {}

  /** Everything needed to draw a chart. */
  record Spec(
      Type type,
      String title,
      List<String> labels,
      List<Series> series,
      List<Color> colors,
      int width,
      int height,
      Locale locale) {}

  static final List<Color> PALETTE =
      List.of(
          new Color(0x006A4E),
          new Color(0xF2A900),
          new Color(0x1F6FB2),
          new Color(0xC0392B),
          new Color(0x7D3C98),
          new Color(0x16A085),
          new Color(0xD35400),
          new Color(0x5D6D7E),
          new Color(0x27AE60),
          new Color(0x8E44AD));

  private static final Font BASE_FONT = loadFont();
  private static final Color AXIS = new Color(0x555555);
  private static final Color GRID = new Color(0xDDDDDD);

  private ChartPainter() {}

  static byte[] png(Spec spec) {
    BufferedImage image =
        new BufferedImage(spec.width(), spec.height(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, spec.width(), spec.height());
      float unit = spec.height() / 100f;
      float top = unit * 4;
      if (spec.title() != null && !spec.title().isBlank()) {
        g.setFont(BASE_FONT.deriveFont(Font.BOLD, unit * 6));
        g.setColor(Color.DARK_GRAY);
        drawCentered(g, spec.title(), spec.width() / 2f, top + g.getFontMetrics().getAscent());
        top += g.getFontMetrics().getHeight() + unit * 2;
      }
      boolean legend = spec.series().size() > 1 || isRound(spec.type());
      float bottom = spec.height() - unit * 3 - (legend && !isRound(spec.type()) ? unit * 9 : 0);
      switch (spec.type()) {
        case BAR, LINE -> axes(g, spec, unit, top, bottom);
        case PIE, DOUGHNUT -> pie(g, spec, unit, top, bottom);
        default -> throw new IllegalStateException();
      }
      if (legend && !isRound(spec.type())) {
        seriesLegend(g, spec, unit, spec.height() - unit * 8);
      }
    } finally {
      g.dispose();
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", out);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  private static boolean isRound(Type type) {
    return type == Type.PIE || type == Type.DOUGHNUT;
  }

  private static Color color(Spec spec, int index) {
    List<Color> colors = spec.colors().isEmpty() ? PALETTE : spec.colors();
    return colors.get(index % colors.size());
  }

  private static void axes(Graphics2D g, Spec spec, float unit, float top, float bottom) {
    double min = 0;
    double max = 0;
    for (Series series : spec.series()) {
      for (double value : series.values()) {
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
    }
    if (max == min) {
      max = min + 1;
    }
    double step = niceStep((max - min) / 5);
    double axisMin = Math.floor(min / step) * step;
    double axisMax = Math.ceil(max / step) * step;
    NumberFormat format = NumberFormat.getNumberInstance(spec.locale());
    format.setMaximumFractionDigits(step < 1 ? 2 : 0);

    g.setFont(BASE_FONT.deriveFont(Font.PLAIN, unit * 4));
    FontMetrics metrics = g.getFontMetrics();
    float labelsHeight = metrics.getHeight() + unit * 2;
    float plotBottom = bottom - labelsHeight;
    float left = unit * 2;
    for (double v = axisMin; v <= axisMax + step / 2; v += step) {
      left = Math.max(left, metrics.stringWidth(clean(format.format(v))) + unit * 3);
    }
    float right = spec.width() - unit * 3;
    float plotHeight = plotBottom - top;
    float plotWidth = right - left;

    for (double v = axisMin; v <= axisMax + step / 2; v += step) {
      float y = (float) (plotBottom - (v - axisMin) / (axisMax - axisMin) * plotHeight);
      g.setColor(GRID);
      g.setStroke(new BasicStroke(unit * 0.25f));
      g.draw(new Line2D.Float(left, y, right, y));
      g.setColor(AXIS);
      String label = clean(format.format(v));
      g.drawString(label, left - unit - metrics.stringWidth(label), y + metrics.getAscent() / 2.5f);
    }
    float zeroY = (float) (plotBottom - (0 - axisMin) / (axisMax - axisMin) * plotHeight);

    int count = spec.labels().size();
    float slot = plotWidth / count;
    int seriesCount = spec.series().size();
    for (int i = 0; i < count; i++) {
      String label = ellipsis(spec.labels().get(i), metrics, slot - unit);
      g.setColor(AXIS);
      drawCentered(
          g, label, left + slot * (i + 0.5f), plotBottom + unit * 1.5f + metrics.getAscent());
    }
    if (spec.type() == Type.BAR) {
      float groupWidth = slot * 0.75f;
      float barWidth = groupWidth / seriesCount;
      for (int s = 0; s < seriesCount; s++) {
        g.setColor(color(spec, s));
        List<Double> values = spec.series().get(s).values();
        for (int i = 0; i < count; i++) {
          float x = left + slot * i + (slot - groupWidth) / 2 + barWidth * s;
          float y =
              (float) (plotBottom - (values.get(i) - axisMin) / (axisMax - axisMin) * plotHeight);
          g.fill(
              new Rectangle2D.Float(x, Math.min(y, zeroY), barWidth * 0.92f, Math.abs(zeroY - y)));
        }
      }
    } else {
      for (int s = 0; s < seriesCount; s++) {
        g.setColor(color(spec, s));
        g.setStroke(new BasicStroke(unit * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Float path = new Path2D.Float();
        List<Double> values = spec.series().get(s).values();
        for (int i = 0; i < count; i++) {
          float x = left + slot * (i + 0.5f);
          float y =
              (float) (plotBottom - (values.get(i) - axisMin) / (axisMax - axisMin) * plotHeight);
          if (i == 0) {
            path.moveTo(x, y);
          } else {
            path.lineTo(x, y);
          }
          g.fill(new Ellipse2D.Float(x - unit, y - unit, unit * 2, unit * 2));
        }
        g.draw(path);
      }
    }
    g.setColor(AXIS);
    g.setStroke(new BasicStroke(unit * 0.4f));
    g.draw(new Line2D.Float(left, zeroY, right, zeroY));
  }

  private static void pie(Graphics2D g, Spec spec, float unit, float top, float bottom) {
    List<Double> values = spec.series().get(0).values();
    double total = values.stream().mapToDouble(Double::doubleValue).sum();
    float size = Math.min(bottom - top, spec.width() * 0.55f);
    float x = unit * 4;
    float y = top + (bottom - top - size) / 2;
    double start = 90;
    for (int i = 0; i < values.size(); i++) {
      double extent = total == 0 ? 0 : -values.get(i) / total * 360;
      g.setColor(color(spec, i));
      g.fill(new Arc2D.Double(x, y, size, size, start, extent, Arc2D.PIE));
      start += extent;
    }
    if (spec.type() == Type.DOUGHNUT) {
      g.setColor(Color.WHITE);
      float hole = size * 0.55f;
      g.fill(new Ellipse2D.Float(x + (size - hole) / 2, y + (size - hole) / 2, hole, hole));
    }
    NumberFormat percent = NumberFormat.getPercentInstance(spec.locale());
    percent.setMaximumFractionDigits(1);
    g.setFont(BASE_FONT.deriveFont(Font.PLAIN, unit * 4.5f));
    FontMetrics metrics = g.getFontMetrics();
    float legendX = x + size + unit * 6;
    float line = metrics.getHeight() * 1.3f;
    float legendY = top + (bottom - top - line * values.size()) / 2 + metrics.getAscent();
    for (int i = 0; i < values.size(); i++) {
      g.setColor(color(spec, i));
      g.fill(
          new Rectangle2D.Float(
              legendX, legendY + i * line - metrics.getAscent() * 0.8f, unit * 3, unit * 3));
      g.setColor(AXIS);
      String share = total == 0 ? "" : " (" + clean(percent.format(values.get(i) / total)) + ")";
      String label =
          ellipsis(spec.labels().get(i) + share, metrics, spec.width() - legendX - unit * 6);
      g.drawString(label, legendX + unit * 4.5f, legendY + i * line);
    }
  }

  private static void seriesLegend(Graphics2D g, Spec spec, float unit, float y) {
    g.setFont(BASE_FONT.deriveFont(Font.PLAIN, unit * 4));
    FontMetrics metrics = g.getFontMetrics();
    float x = unit * 4;
    for (int s = 0; s < spec.series().size(); s++) {
      String name = spec.series().get(s).name();
      g.setColor(color(spec, s));
      g.fill(new Rectangle2D.Float(x, y, unit * 3, unit * 3));
      g.setColor(AXIS);
      g.drawString(name, x + unit * 4.5f, y + unit * 2.6f);
      x += unit * 9 + metrics.stringWidth(name);
    }
  }

  /** Rounds a raw step to 1, 2 or 5 times a power of ten. */
  static double niceStep(double raw) {
    double power = Math.pow(10, Math.floor(Math.log10(raw)));
    double fraction = raw / power;
    double nice = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10;
    return nice * power;
  }

  private static void drawCentered(Graphics2D g, String text, float centerX, float baseline) {
    g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2f, baseline);
  }

  private static String ellipsis(String text, FontMetrics metrics, float width) {
    if (metrics.stringWidth(text) <= width) {
      return text;
    }
    String value = text;
    while (value.length() > 1 && metrics.stringWidth(value + "…") > width) {
      value = value.substring(0, value.length() - 1);
    }
    return value + "…";
  }

  /** Narrow no-break spaces are not in every font: use plain no-break spaces. */
  private static String clean(String text) {
    return text.replace(' ', ' ');
  }

  private static Font loadFont() {
    try (InputStream in =
        ChartPainter.class.getResourceAsStream(
            "/io/github/juste120/docmint/fonts/NotoSans-Regular.ttf")) {
      if (in == null) {
        throw new IllegalStateException("DocMint embedded font is missing");
      }
      return Font.createFont(Font.TRUETYPE_FONT, in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (FontFormatException e) {
      throw new IllegalStateException("DocMint embedded font is invalid", e);
    }
  }
}
