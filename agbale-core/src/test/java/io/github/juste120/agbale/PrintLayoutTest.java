package io.github.juste120.agbale;

import static io.github.juste120.agbale.support.PdfText.firstPageSize;
import static io.github.juste120.agbale.support.PdfText.pageText;
import static io.github.juste120.agbale.support.PdfText.pages;
import static io.github.juste120.agbale.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.domain.PageSettings;
import java.util.Map;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

/** FR-026: default print stylesheet (A4, page numbers, page breaks, repeated headers). */
class PrintLayoutTest {

  private static final float MM = 72f / 25.4f;
  private final Agbale agbale = Agbale.createDefault();

  @Test
  void defaultPageIsA4PortraitWithPageNumbers() {
    byte[] pdf = agbale.pdf("simple", Map.of("name", "Ama"));

    PDRectangle size = firstPageSize(pdf);
    assertThat(size.getWidth()).isCloseTo(210 * MM, within(1f));
    assertThat(size.getHeight()).isCloseTo(297 * MM, within(1f));
    assertThat(text(pdf)).contains("Page 1 / 1");
  }

  @Test
  void pageBreakClassStartsANewPage() {
    byte[] pdf = agbale.pdf("pages", Map.of());

    assertThat(pages(pdf)).isEqualTo(3);
    assertThat(pageText(pdf, 1)).contains("Premier").contains("Page 1 / 3");
    assertThat(pageText(pdf, 2)).contains("Deuxième").contains("Page 2 / 3");
    assertThat(pageText(pdf, 3)).contains("Troisième").contains("Page 3 / 3");
  }

  @Test
  void tableHeaderIsRepeatedOnEveryPage() {
    byte[] pdf = agbale.pdf("long-table", Map.of());

    assertThat(pages(pdf)).isGreaterThan(1);
    for (int page = 1; page <= pages(pdf); page++) {
      assertThat(pageText(pdf, page)).contains("Entete Colonne");
    }
  }

  @Test
  void templateCssOverridesDefaults() {
    PDRectangle size = firstPageSize(agbale.pdf("custom-css", Map.of()));

    assertThat(size.getWidth()).isCloseTo(210 * MM, within(1f)); // A5 landscape: 210 x 148
    assertThat(size.getHeight()).isCloseTo(148 * MM, within(1f));
  }

  @Test
  void pageSettingsChangeTheDefaultLayout() {
    Agbale letter =
        Agbale.builder()
            .pageSettings(
                new PageSettings(
                    PageSettings.PageSize.LETTER,
                    PageSettings.Orientation.LANDSCAPE,
                    10,
                    10,
                    10,
                    10.5))
            .build();

    PDRectangle size = firstPageSize(letter.pdf("simple", Map.of("name", "Ama")));

    assertThat(size.getWidth()).isCloseTo(11 * 72f, within(1f));
    assertThat(size.getHeight()).isCloseTo(8.5f * 72f, within(1f));
  }
}
