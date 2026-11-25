package io.github.juste120.agbale.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.InvalidTemplateNameException;
import io.github.juste120.agbale.api.ResourceRefusedException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-010: template names and resource paths can never leave the templates folder. */
class PathTraversalTest {

  private final Agbale agbale = Agbale.createDefault();

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
    assertThatThrownBy(() -> agbale.pdf(name, Map.of()))
        .isInstanceOf(InvalidTemplateNameException.class)
        .hasMessageStartingWith("Invalid template name")
        .hasMessageNotContaining("\r")
        .hasMessageNotContaining("\u0000");
  }

  @Test
  void rejectsNullName() {
    assertThatThrownBy(() -> agbale.pdf(null, Map.of()))
        .isInstanceOf(InvalidTemplateNameException.class);
  }

  @Test
  void rejectsTraversalInIncludes() {
    assertThatThrownBy(() -> agbale.pdf("security/traversal-include", Map.of()))
        .isInstanceOf(InvalidTemplateNameException.class);
  }

  @Test
  void refusesResourcesOutsideTheTemplatesFolder() {
    assertThatThrownBy(() -> agbale.pdf("security/escape-root", Map.of()))
        .isInstanceOf(ResourceRefusedException.class)
        .hasMessageContaining("not allowed");
  }
}
