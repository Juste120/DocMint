package io.github.juste120.docmint.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.DocMintException;
import io.github.juste120.docmint.api.RenderException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-015: templates cannot call methods nor reach Java internals; undefined variables fail. */
class TemplateSandboxTest {

  private final DocMint docMint = DocMint.createDefault();

  /** A data object exposing a dangerous-looking method. */
  public record Dangerous(String name) {
    public Runtime getRuntime() {
      return Runtime.getRuntime();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"security/getclass", "security/classloader", "security/method-call"})
  void methodCallsAndClassAccessFail(String template) {
    assertThatThrownBy(() -> docMint.pdf(template, Map.of("name", "Ama")))
        .isInstanceOf(DocMintException.class)
        .hasMessageContaining(template);
  }

  @Test
  void javaObjectsAreNeverExposedToTemplates() {
    // getRuntime() is not a record component: it is not part of the template model.
    assertThatThrownBy(() -> docMint.pdf("security/classloader", new Dangerous("Ama")))
        .isInstanceOf(DocMintException.class);
  }

  @Test
  void loopVariablesWorkButCannotBeAbused() {
    String text =
        io.github.juste120.docmint.support.PdfText.text(
            docMint.pdf("loop", Map.of("items", java.util.List.of("a", "b", "c"))));

    assertThat(text.replaceAll("\\s+", " "))
        .contains("0/3 premier a")
        .contains("1/3 b")
        .contains("2/3 dernier c");
    assertThatThrownBy(
            () -> docMint.pdf("security/loop-escape", Map.of("items", java.util.List.of(1))))
        .isInstanceOf(DocMintException.class);
  }

  @Test
  void undefinedVariableIsAnError() {
    assertThatThrownBy(() -> docMint.pdf("undefined", Map.of("customer", Map.of("name", "Ama"))))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("undefined")
        .hasMessageContaining("phone");
  }
}
