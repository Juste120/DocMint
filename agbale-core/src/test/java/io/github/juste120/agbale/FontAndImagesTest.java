package io.github.juste120.agbale;

import static io.github.juste120.agbale.support.PdfText.fontsOnFirstPage;
import static io.github.juste120.agbale.support.PdfText.imagesOnFirstPage;
import static io.github.juste120.agbale.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.ResourceMissingException;
import io.github.juste120.agbale.domain.FontDefinition;
import io.github.juste120.agbale.domain.MissingResourceMode;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** FR-023 to FR-025, FR-034: embedded font, images, missing resources. */
class FontAndImagesTest {

  private static final String FRENCH =
      "Société Générale — Lomé, Kpalimé, à payer avant le 15/12 ; œuvre, Noël, ça, €";
  private static final String EWE = "Ɖ ɖ Ɛ ɛ Ƒ ƒ Ɣ ɣ Ŋ ŋ Ɔ ɔ Ʋ ʋ — Ŋlɔ̃ Agbalẽ Eʋegbe";

  private final Agbale agbale = Agbale.createDefault();

  @Test
  void frenchAndEweTextIsRenderedWithTheEmbeddedFont() {
    for (String sample : new String[] {FRENCH, EWE}) {
      byte[] pdf = agbale.pdf("accents", Map.of("text", sample));

      String text = text(pdf).replaceAll("\\s+", " ");
      // Four occurrences: default font, a font that is not installed (Arial), bold, italic.
      assertThat(text.split(Pattern.quote(sample), -1)).hasSize(5);
      assertThat(fontsOnFirstPage(pdf)).contains("NotoSans").doesNotContain("Helvetica");
    }
  }

  @Test
  void imagesFromClasspathAndDataUrisAreEmbedded() {
    byte[] pdf = agbale.pdf("images", Map.of());

    assertThat(text(pdf)).contains("Images");
    assertThat(imagesOnFirstPage(pdf)).isGreaterThanOrEqualTo(2);
  }

  @Test
  void missingImageFailsByDefault() {
    assertThatThrownBy(() -> agbale.pdf("missing-image", Map.of()))
        .isInstanceOf(ResourceMissingException.class)
        .hasMessageContaining("images/nope.png")
        .hasMessageNotContaining("agbale-classpath");
  }

  @Test
  void missingImageIsSkippedInWarnMode() {
    Agbale tolerant = Agbale.builder().missingResources(MissingResourceMode.WARN).build();

    assertThat(text(tolerant.pdf("missing-image", Map.of()))).contains("Sans logo");
  }

  @Test
  void malformedDataUriIsReportedAsMissing() {
    assertThatThrownBy(() -> agbale.pdf("bad-data-uri", Map.of()))
        .isInstanceOf(ResourceMissingException.class);
  }

  @Test
  void customFontCanBeRegistered() {
    Agbale custom =
        Agbale.builder()
            .font("Ma Police", "io/github/juste120/agbale/fonts/NotoSans-Bold.ttf")
            .font(
                new FontDefinition(
                    "Ma Police",
                    "/io/github/juste120/agbale/fonts/NotoSans-BoldItalic.ttf",
                    400,
                    true))
            .build();

    assertThat(text(custom.pdf("simple", Map.of("name", "Ama")))).contains("Bonjour Ama");
  }

  @Test
  void unknownCustomFontFailsAtBuildTime() {
    assertThatThrownBy(() -> Agbale.builder().font("X", "fonts/nope.ttf").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fonts/nope.ttf");
  }
}
