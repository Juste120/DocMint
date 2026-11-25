package io.github.juste120.docmint.security;

import static io.github.juste120.docmint.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.docmint.api.DocMint;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-014: variables are HTML-escaped by default; raw output only through the explicit filter. */
class HtmlEscapingTest {

  private final DocMint docMint = DocMint.createDefault();

  @Test
  void escapesMarkupInData() {
    String text =
        text(docMint.pdf("text", Map.of("text", "<b>gras</b> <script>alert(1)</script>")));

    assertThat(text).contains("<b>gras</b>").contains("<script>alert(1)</script>");
  }

  @Test
  void injectedImageTagIsNotInterpreted() {
    String payload = "<img src=\"file:///etc/passwd\">";

    String text = text(docMint.pdf("text", Map.of("text", payload)));

    assertThat(text).contains(payload);
  }

  @Test
  void rawFilterInterpretsMarkupExplicitly() {
    String text = text(docMint.pdf("raw", Map.of("text", "<b>gras</b>")));

    assertThat(text).contains("gras").doesNotContain("<b>");
  }
}
