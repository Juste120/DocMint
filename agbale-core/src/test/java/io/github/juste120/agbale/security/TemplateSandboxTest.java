package io.github.juste120.agbale.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.AgbaleException;
import io.github.juste120.agbale.api.RenderException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-015: templates cannot call methods nor reach Java internals; undefined variables fail. */
class TemplateSandboxTest {

  private final Agbale agbale = Agbale.createDefault();

  /** A data object exposing a dangerous-looking method. */
  public record Dangerous(String name) {
    public Runtime getRuntime() {
      return Runtime.getRuntime();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"security/getclass", "security/classloader", "security/method-call"})
  void methodCallsAndClassAccessFail(String template) {
    assertThatThrownBy(() -> agbale.pdf(template, Map.of("name", "Ama")))
        .isInstanceOf(AgbaleException.class)
        .hasMessageContaining(template);
  }

  @Test
  void javaObjectsAreNeverExposedToTemplates() {
    // getRuntime() is not a record component: it is not part of the template model.
    assertThatThrownBy(() -> agbale.pdf("security/classloader", new Dangerous("Ama")))
        .isInstanceOf(AgbaleException.class);
  }

  @Test
  void undefinedVariableIsAnError() {
    assertThatThrownBy(() -> agbale.pdf("undefined", Map.of("customer", Map.of("name", "Ama"))))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("undefined")
        .hasMessageContaining("phone");
  }
}
