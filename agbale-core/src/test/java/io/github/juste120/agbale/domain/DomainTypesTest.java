package io.github.juste120.agbale.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DomainTypesTest {

  @Test
  void resourceLimitsHaveSafeDefaults() {
    ResourceLimits limits = ResourceLimits.defaults();

    assertThat(limits.maxTemplateBytes()).isEqualTo(1024 * 1024);
    assertThat(limits.maxPages()).isEqualTo(500);
    assertThat(limits.maxRenderTime()).isEqualTo(Duration.ofSeconds(30));
    assertThat(limits.maxRenderedChars()).isEqualTo(8 * 1024 * 1024);
    assertThatThrownBy(() -> new ResourceLimits(Integer.MAX_VALUE, 1, Duration.ofSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResourceLimits.defaults().withMaxRenderTime(Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void pageSettingsValidateMargins() {
    assertThat(PageSettings.defaults().size()).isEqualTo(PageSettings.PageSize.A4);
    assertThatThrownBy(
            () ->
                new PageSettings(
                    PageSettings.PageSize.A4, PageSettings.Orientation.PORTRAIT, -1, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void policyNormalisesAndValidatesHosts() {
    ResourceAccessPolicy policy = new ResourceAccessPolicy(Set.of(" CDN.Example.com "));

    assertThat(policy.allowedHosts()).containsExactly("cdn.example.com");
    assertThat(policy.hasWildcard()).isFalse();
    assertThat(policy.allow("*.example.org").hasWildcard()).isTrue();
    assertThat(policy.allowsRemote(null)).isFalse();
    assertThat(policy.allowsRemote("https://cdn.example.com:8443/a")).isTrue();
    assertThat(policy.allowsRemote("not a uri")).isFalse();
    for (String invalid :
        new String[] {
          "", "https://x.com", "x.com/a", "x.com:80", "*", "*.", "a@b.com", ".x.com", "x.*.com"
        }) {
      assertThatThrownBy(() -> ResourceAccessPolicy.denyAllRemote().allow(invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void fontDefinitionsAreValidated() {
    assertThat(FontDefinition.regular("Inter", "/fonts/Inter.ttf").classpathLocation())
        .isEqualTo("fonts/Inter.ttf");
    for (String location : new String[] {" ", "../f.ttf", "file:/f.ttf", "a\\f.ttf"}) {
      assertThatThrownBy(() -> FontDefinition.regular("Inter", location))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThatThrownBy(() -> FontDefinition.regular(" ", "f.ttf"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FontDefinition("Inter", "f.ttf", 50, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void renderedDocumentCopiesItsContent() {
    byte[] bytes = {1, 2, 3};
    RenderedDocument document = new RenderedDocument(bytes, OutputFormat.PDF, 1);
    bytes[0] = 9;

    assertThat(document.content()).containsExactly(1, 2, 3);
    assertThat(document).isEqualTo(new RenderedDocument(new byte[] {1, 2, 3}, OutputFormat.PDF, 1));
    assertThat(document.hashCode())
        .isEqualTo(new RenderedDocument(new byte[] {1, 2, 3}, OutputFormat.PDF, 1).hashCode());
    assertThat(document.toString()).contains("size=3");
    assertThat(OutputFormat.PDF.mediaType()).isEqualTo("application/pdf");
    assertThat(OutputFormat.PDF.fileExtension()).isEqualTo("pdf");
  }
}
