package io.github.juste120.docmint;

import static io.github.juste120.docmint.support.PdfText.fontsOnFirstPage;
import static io.github.juste120.docmint.support.PdfText.imagesOnFirstPage;
import static io.github.juste120.docmint.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.ResourceMissingException;
import io.github.juste120.docmint.domain.FontDefinition;
import io.github.juste120.docmint.domain.MissingResourceMode;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** FR-023 to FR-025, FR-034: embedded font, images, missing resources. */
class FontAndImagesTest {

  private static final String FRENCH =
      "Société Générale — Lomé, Kpalimé, à payer avant le 15/12 ; œuvre, Noël, ça, €";
  private static final String EWE = "Ɖ ɖ Ɛ ɛ Ƒ ƒ Ɣ ɣ Ŋ ŋ Ɔ ɔ Ʋ ʋ — Ŋlɔ̃ Agbalẽ Eʋegbe";

  private final DocMint docMint = DocMint.createDefault();

  @Test
  void frenchAndEweTextIsRenderedWithTheEmbeddedFont() {
    for (String sample : new String[] {FRENCH, EWE}) {
      byte[] pdf = docMint.pdf("accents", Map.of("text", sample));

      String text = text(pdf).replaceAll("\\s+", " ");
      // Four occurrences: default font, a font that is not installed (Arial), bold, italic.
      assertThat(text.split(Pattern.quote(sample), -1)).hasSize(5);
      assertThat(fontsOnFirstPage(pdf)).contains("NotoSans").doesNotContain("Helvetica");
    }
  }

  @Test
  void imagesFromClasspathAndDataUrisAreEmbedded() {
    byte[] pdf = docMint.pdf("images", Map.of());

    assertThat(text(pdf)).contains("Images");
    assertThat(imagesOnFirstPage(pdf)).isGreaterThanOrEqualTo(2);
  }

  @Test
  void missingImageFailsByDefault() {
    assertThatThrownBy(() -> docMint.pdf("missing-image", Map.of()))
        .isInstanceOf(ResourceMissingException.class)
        .hasMessageContaining("images/nope.png")
        .hasMessageNotContaining("docmint-classpath");
  }

  @Test
  void missingImageIsSkippedInWarnMode() {
    DocMint tolerant = DocMint.builder().missingResources(MissingResourceMode.WARN).build();

    assertThat(text(tolerant.pdf("missing-image", Map.of()))).contains("Sans logo");
  }

  @Test
  void malformedDataUriIsReportedAsMissing() {
    assertThatThrownBy(() -> docMint.pdf("bad-data-uri", Map.of()))
        .isInstanceOf(ResourceMissingException.class);
  }

  @Test
  void customFontCanBeRegistered() {
    DocMint custom =
        DocMint.builder()
            .font("Ma Police", "io/github/juste120/docmint/fonts/NotoSans-Bold.ttf")
            .font(
                new FontDefinition(
                    "Ma Police",
                    "/io/github/juste120/docmint/fonts/NotoSans-BoldItalic.ttf",
                    400,
                    true))
            .build();

    assertThat(text(custom.pdf("simple", Map.of("name", "Ama")))).contains("Bonjour Ama");
  }

  @Test
  void unknownCustomFontFailsAtBuildTime() {
    assertThatThrownBy(() -> DocMint.builder().font("X", "fonts/nope.ttf").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fonts/nope.ttf");
  }
}
