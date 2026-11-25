package io.github.juste120.docmint.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.InvalidTemplateNameException;
import io.github.juste120.docmint.api.ResourceRefusedException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-010: template names and resource paths can never leave the templates folder. */
class PathTraversalTest {

  private final DocMint docMint = DocMint.createDefault();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../../etc/passwd",
        "../reports/invoice",
        "layouts/../../secret",
        "/etc/passwd",
        "%2e%2e%2fsecret",
        "..%2Fsecret",
        "layouts\\..\\secret",
        "file:/etc/passwd",
        "classpath:reports/invoice",
        "invoice\u0000.html",
        "invoice\r\nX-Injected: 1",
        "./invoice",
        "layouts//base",
        "",
        "   ",
        ".html"
      })
  void rejectsNamesLeavingTheTemplatesFolder(String name) {
    assertThatThrownBy(() -> docMint.pdf(name, Map.of()))
        .isInstanceOf(InvalidTemplateNameException.class)
        .hasMessageStartingWith("Invalid template name")
        .hasMessageNotContaining("\r")
        .hasMessageNotContaining("\u0000");
  }

  @Test
  void rejectsNullName() {
    assertThatThrownBy(() -> docMint.pdf(null, Map.of()))
        .isInstanceOf(InvalidTemplateNameException.class);
  }

  @Test
  void rejectsTraversalInIncludes() {
    assertThatThrownBy(() -> docMint.pdf("security/traversal-include", Map.of()))
        .isInstanceOf(InvalidTemplateNameException.class);
  }

  @Test
  void refusesResourcesOutsideTheTemplatesFolder() {
    assertThatThrownBy(() -> docMint.pdf("security/escape-root", Map.of()))
        .isInstanceOf(ResourceRefusedException.class)
        .hasMessageContaining("not allowed");
  }
}
