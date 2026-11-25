package io.github.juste120.docmint.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TemplateNameTest {

  @Test
  void normalisesTheExtensionAndExposesParts() {
    TemplateName name = TemplateName.of(" invoices/standard.html ");

    assertThat(name.value()).isEqualTo("invoices/standard");
    assertThat(name.path()).isEqualTo("invoices/standard.html");
    assertThat(name.folder()).isEqualTo("invoices/");
    assertThat(name.simpleName()).isEqualTo("standard");
    assertThat(name).hasToString("invoices/standard");
    assertThat(TemplateName.of("reçu").folder()).isEmpty();
    assertThat(TemplateName.of("v1.2-final_doc").value()).isEqualTo("v1.2-final_doc");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../x", "a/../b", "/x", "a\\b", "c:x", "a%2fb", "a//b", "a/", ".", "a/./b", "a b", "a\tb",
        "..."
      })
  void rejectsUnsafeNames(String raw) {
    assertThat(TemplateName.isValid(raw)).isFalse();
    assertThatThrownBy(() -> TemplateName.of(raw)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullEmptyAndTooLongNames() {
    assertThatThrownBy(() -> TemplateName.of(null)).hasMessageContaining("null");
    assertThatThrownBy(() -> new TemplateName(null)).hasMessageContaining("empty");
    assertThatThrownBy(() -> TemplateName.of("a".repeat(201))).hasMessageContaining("200");
    assertThat(TemplateName.isValid("a".repeat(200))).isTrue();
  }
}
