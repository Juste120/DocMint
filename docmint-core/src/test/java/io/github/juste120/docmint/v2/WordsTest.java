package io.github.juste120.docmint.v2;

import static io.github.juste120.docmint.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** V2 user story 3 in a real document. Spelling rules are tested in NumberSpellerTest. */
class WordsTest {

  @Test
  void amountInWordsOnAnInvoice() {
    String text =
        text(DocMint.createDefault().pdf("v2/words", Map.of("total", new BigDecimal("1250000"))));

    assertThat(text.replaceAll("\\s+", " "))
        .contains("à la somme de un million deux cent cinquante mille francs CFA.");
  }

  @Test
  void outOfRangeIsAClearError() {
    assertThatThrownBy(
            () ->
                DocMint.createDefault()
                    .pdf("v2/words", Map.of("total", new BigDecimal("1000000000000000"))))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("words");
  }
}
