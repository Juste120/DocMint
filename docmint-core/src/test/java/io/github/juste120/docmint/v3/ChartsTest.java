package io.github.juste120.docmint.v3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** V3 user story 1: charts (FR-201, FR-202). */
class ChartsTest {

  private static final int GREEN = 0x006A4E;
  private final DocMint docMint = DocMint.createDefault();

  static BufferedImage firstImage(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDPage page = document.getPage(0);
      for (COSName name : page.getResources().getXObjectNames()) {
        if (page.getResources().getXObject(name) instanceof PDImageXObject image) {
          return image.getImage();
        }
      }
      throw new AssertionError("no image");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static long count(BufferedImage image, int rgb) {
    long n = 0;
    for (int y = 0; y < image.getHeight(); y += 2) {
      for (int x = 0; x < image.getWidth(); x += 2) {
        if ((image.getRGB(x, y) & 0xFFFFFF) == rgb) {
          n++;
        }
      }
    }
    return n;
  }

  private Map<String, Object> data(String type, List<?> values) {
    Map<String, Object> data = new HashMap<>();
    data.put("type", type);
    data.put("labels", List.of("Janvier", "Février", "Mars", "Avril"));
    data.put("values", values);
    return data;
  }

  @ParameterizedTest
  @ValueSource(strings = {"bar", "line", "pie", "doughnut"})
  void everyTypeIsDrawnAtTheRequestedResolution(String type) {
    byte[] pdf =
        docMint.pdf("v3/charts", data(type, List.of(1_250_000, 980_000, 1_500_000, 400_000)));

    BufferedImage image = firstImage(pdf);
    // 120 mm x 60 mm at 96 DPI x 3
    assertThat(image.getWidth()).isEqualTo(1361);
    assertThat(image.getHeight()).isEqualTo(680);
    assertThat(count(image, GREEN)).as("first series colour").isPositive();
  }

  @Test
  void pieSlicesAreProportional() {
    BufferedImage half = firstImage(docMint.pdf("v3/charts", data("pie", List.of(50, 50, 0, 0))));
    BufferedImage quarter =
        firstImage(docMint.pdf("v3/charts", data("pie", List.of(25, 75, 0, 0))));

    double ratio = (double) count(quarter, GREEN) / count(half, GREEN);
    assertThat(ratio).isBetween(0.45, 0.55);
  }

  @Test
  void barsGrowWithValues() {
    BufferedImage small =
        firstImage(docMint.pdf("v3/charts", data("bar", List.of(10, 10, 10, 10))));
    BufferedImage big = firstImage(docMint.pdf("v3/charts", data("bar", List.of(10, 10, 10, 40))));

    assertThat(count(big, GREEN))
        .isLessThan(count(small, GREEN)); // other bars shrink on a larger axis
  }

  @Test
  void multipleSeriesUseTheirColours() {
    BufferedImage image = firstImage(docMint.pdf("v3/multi", Map.of()));

    assertThat(count(image, 0x123456)).isPositive();
    assertThat(count(image, 0xABCDEF)).isPositive();
  }

  @Test
  void invalidArgumentsAreClearErrors() {
    Map<String, Object> base = new HashMap<>(data("bar", List.of(1, 2, 3, 4)));
    base.put("width", "100mm");
    assertThat(docMint.pdf("v3/chart-args", base)).isNotEmpty();

    Map<String, Object> wrongType = new HashMap<>(base);
    wrongType.put("type", "radar");
    assertThatThrownBy(() -> docMint.pdf("v3/chart-args", wrongType))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("unknown chart type");

    Map<String, Object> mismatch = new HashMap<>(base);
    mismatch.put("values", List.of(1, 2));
    assertThatThrownBy(() -> docMint.pdf("v3/chart-args", mismatch))
        .hasMessageContaining("2 values for 4 labels");

    Map<String, Object> negativePie = new HashMap<>(base);
    negativePie.put("type", "pie");
    negativePie.put("values", List.of(1, -2, 3, 4));
    assertThatThrownBy(() -> docMint.pdf("v3/chart-args", negativePie))
        .hasMessageContaining("negative");

    Map<String, Object> badSize = new HashMap<>(base);
    badSize.put("width", "100mm;background:url(x)");
    assertThatThrownBy(() -> docMint.pdf("v3/chart-args", badSize))
        .hasMessageContaining("invalid chart size");

    Map<String, Object> notNumbers = new HashMap<>(base);
    notNumbers.put("values", List.of("a", "b", "c", "d"));
    assertThatThrownBy(() -> docMint.pdf("v3/chart-args", notNumbers))
        .hasMessageContaining("not a number");

    List<String> labels = new ArrayList<>();
    List<Integer> values = new ArrayList<>();
    for (int i = 0; i < 1001; i++) {
      labels.add("l" + i);
      values.add(i);
    }
    Map<String, Object> tooMany = new HashMap<>(base);
    tooMany.put("labels", labels);
    tooMany.put("values", values);
    assertThatThrownBy(() -> docMint.pdf("v3/chart-args", tooMany))
        .hasMessageContaining("1 to 1000");
  }
}
