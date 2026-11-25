package io.github.juste120.docmint.v2;

import static io.github.juste120.docmint.support.PdfText.pageText;
import static io.github.juste120.docmint.support.PdfText.pages;
import static io.github.juste120.docmint.support.PdfText.sortedPageText;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.docmint.api.DocMint;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** V2 user story 1: headers and footers on every page (FR-101 to FR-104). */
class HeaderFooterTest {

  private final DocMint docMint = DocMint.createDefault();

  @Test
  void headerAndFooterAreRepeatedWithPageNumbers() {
    byte[] pdf = docMint.pdf("v2/header-footer", Map.of());

    assertThat(pages(pdf)).isEqualTo(3);
    for (int page = 1; page <= 3; page++) {
      assertThat(sortedPageText(pdf, page))
          .contains("Entete Societe")
          .contains("Pied legal — page " + page + " sur 3")
          .doesNotContain("Page " + page + " / 3"); // the V1 default footer is replaced
    }
  }

  @Test
  void firstPageVariants() {
    byte[] pdf = docMint.pdf("v2/first-page", Map.of());

    assertThat(pageText(pdf, 1))
        .contains("Grand entete premiere page")
        .doesNotContain("Petit entete");
    assertThat(pageText(pdf, 2)).contains("Petit entete suite").doesNotContain("Grand entete");
    // No custom footer: the V1 default footer stays.
    assertThat(pageText(pdf, 2)).contains("Page 2 / 2");
  }

  @Test
  void footerWrittenAtTheEndIsOnEveryPage() {
    byte[] pdf = docMint.pdf("v2/footer-at-end", Map.of());

    for (int page = 1; page <= 3; page++) {
      assertThat(sortedPageText(pdf, page)).contains("Pied final " + page + "/3");
    }
  }

  @Test
  void templatesWithoutHeaderRenderAsInV1() {
    assertThat(pageText(docMint.pdf("pages", Map.of()), 1)).contains("Page 1 / 3");
  }
}
