package io.github.juste120.docmint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.DocMintException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.TemplateNotFoundException;
import io.github.juste120.docmint.api.TemplateSyntaxException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** User story 5: errors tell what went wrong and what to do, without sensitive details. */
class ErrorMessagesTest {

  private final DocMint docMint = DocMint.createDefault();

  @Test
  void unknownTemplateListsAvailableTemplatesAndSuggestsOne() {
    assertThatThrownBy(() -> docMint.pdf("invoce", Map.of()))
        .isInstanceOfSatisfying(
            TemplateNotFoundException.class,
            e -> {
              assertThat(e.code()).isEqualTo("DOCMINT_TEMPLATE_NOT_FOUND");
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
    DocMint elsewhere = DocMint.builder().templatesPath("nothing-here").build();

    assertThatThrownBy(() -> elsewhere.pdf("invoice", Map.of()))
        .isInstanceOf(TemplateNotFoundException.class)
        .hasMessageContaining("src/main/resources/nothing-here/");
  }

  @Test
  void missingLayoutIsNamed() {
    assertThatThrownBy(() -> docMint.pdf("missing-layout", Map.of()))
        .isInstanceOfSatisfying(
            TemplateNotFoundException.class,
            e -> assertThat(e.templateName()).isEqualTo("layouts/does-not-exist"));
  }

  @Test
  void syntaxErrorGivesTemplateAndLine() {
    assertThatThrownBy(() -> docMint.pdf("syntax-error", Map.of("items", java.util.List.of(1))))
        .isInstanceOfSatisfying(
            TemplateSyntaxException.class,
            e -> {
              assertThat(e.templateName()).isEqualTo("syntax-error");
              assertThat(e.line()).isPresent();
              assertThat(e.code()).isEqualTo("DOCMINT_TEMPLATE_SYNTAX");
            })
        .hasMessageContaining("syntax-error")
        .hasMessageContaining("line");
  }

  @Test
  void selfInheritanceFailsWithoutLooping() {
    assertThatThrownBy(() -> docMint.pdf("self-extends", Map.of()))
        .isInstanceOf(DocMintException.class);
  }

  @Test
  void nullDataIsRefusedWithAHint() {
    assertThatThrownBy(() -> docMint.pdf("simple", null))
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
        docMint.pdf(template, Map.of("items", java.util.List.of(1), "customer", Map.of()));
      } catch (DocMintException e) {
        assertThat(e.getMessage())
            .doesNotContain(System.getProperty("user.dir"))
            .doesNotContain("/home/")
            .doesNotContain("file:")
            .doesNotContain("at io.")
            .doesNotContain("Exception:");
        assertThat(e.code()).startsWith("DOCMINT_");
        continue;
      }
      throw new AssertionError("expected an error for " + template);
    }
  }

  @Test
  void genericRenderExceptionHasStableCode() {
    assertThat(new RenderException("x", null).code()).isEqualTo("DOCMINT_RENDER_FAILED");
  }
}
