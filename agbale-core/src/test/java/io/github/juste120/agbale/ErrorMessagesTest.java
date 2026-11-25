package io.github.juste120.agbale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.AgbaleException;
import io.github.juste120.agbale.api.RenderException;
import io.github.juste120.agbale.api.TemplateNotFoundException;
import io.github.juste120.agbale.api.TemplateSyntaxException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** User story 5: errors tell what went wrong and what to do, without sensitive details. */
class ErrorMessagesTest {

  private final Agbale agbale = Agbale.createDefault();

  @Test
  void unknownTemplateListsAvailableTemplatesAndSuggestsOne() {
    assertThatThrownBy(() -> agbale.pdf("invoce", Map.of()))
        .isInstanceOfSatisfying(
            TemplateNotFoundException.class,
            e -> {
              assertThat(e.code()).isEqualTo("AGBALE_TEMPLATE_NOT_FOUND");
              assertThat(e.templateName()).isEqualTo("invoce");
              assertThat(e.availableTemplates()).contains("invoice", "simple");
              assertThat(e.suggestion()).contains("invoice");
            })
        .hasMessageContaining("Template 'invoce' not found in class path folder 'reports'")
        .hasMessageContaining("Available templates:")
        .hasMessageContaining("Did you mean 'invoice'?");
  }

  @Test
  void unknownTemplatesFolderExplainsWhereToPutFiles() {
    Agbale elsewhere = Agbale.builder().templatesPath("nothing-here").build();

    assertThatThrownBy(() -> elsewhere.pdf("invoice", Map.of()))
        .isInstanceOf(TemplateNotFoundException.class)
        .hasMessageContaining("src/main/resources/nothing-here/");
  }

  @Test
  void missingLayoutIsNamed() {
    assertThatThrownBy(() -> agbale.pdf("missing-layout", Map.of()))
        .isInstanceOfSatisfying(
            TemplateNotFoundException.class,
            e -> assertThat(e.templateName()).isEqualTo("layouts/does-not-exist"));
  }

  @Test
  void syntaxErrorGivesTemplateAndLine() {
    assertThatThrownBy(() -> agbale.pdf("syntax-error", Map.of("items", java.util.List.of(1))))
        .isInstanceOfSatisfying(
            TemplateSyntaxException.class,
            e -> {
              assertThat(e.templateName()).isEqualTo("syntax-error");
              assertThat(e.line()).isPresent();
              assertThat(e.code()).isEqualTo("AGBALE_TEMPLATE_SYNTAX");
            })
        .hasMessageContaining("syntax-error")
        .hasMessageContaining("line");
  }

  @Test
  void selfInheritanceFailsWithoutLooping() {
    assertThatThrownBy(() -> agbale.pdf("self-extends", Map.of()))
        .isInstanceOf(AgbaleException.class);
  }

  @Test
  void nullDataIsRefusedWithAHint() {
    assertThatThrownBy(() -> agbale.pdf("simple", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("empty Map");
  }

  @Test
  void messagesNeverContainAbsolutePathsOrStackTraces() {
    String[] templates = {
      "invoce", "syntax-error", "undefined", "missing-image", "security/escape-root"
    };
    for (String template : templates) {
      try {
        agbale.pdf(template, Map.of("items", java.util.List.of(1), "customer", Map.of()));
      } catch (AgbaleException e) {
        assertThat(e.getMessage())
            .doesNotContain(System.getProperty("user.dir"))
            .doesNotContain("/home/")
            .doesNotContain("file:")
            .doesNotContain("at io.")
            .doesNotContain("Exception:");
        assertThat(e.code()).startsWith("AGBALE_");
        continue;
      }
      throw new AssertionError("expected an error for " + template);
    }
  }

  @Test
  void genericRenderExceptionHasStableCode() {
    assertThat(new RenderException("x", null).code()).isEqualTo("AGBALE_RENDER_FAILED");
  }
}
