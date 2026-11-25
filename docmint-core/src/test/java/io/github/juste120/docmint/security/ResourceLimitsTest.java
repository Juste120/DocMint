package io.github.juste120.docmint.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.domain.ResourceLimits;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-016: template size, rendered size, page count and render time are limited. */
class ResourceLimitsTest {

  @Test
  void templateLargerThanLimitIsRejected() {
    DocMint docMint =
        DocMint.builder().limits(ResourceLimits.defaults().withMaxTemplateBytes(50)).build();

    assertThatThrownBy(() -> docMint.pdf("invoice", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.TEMPLATE_SIZE))
        .hasMessageContaining("docmint.limits.max-template-size");
  }

  @Test
  void runawayLoopIsStoppedByRenderedSizeLimit() {
    DocMint docMint =
        DocMint.builder().limits(ResourceLimits.defaults().withMaxTemplateBytes(10_000)).build();

    assertThatThrownBy(() -> docMint.pdf("security/infinite", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.RENDERED_SIZE));
  }

  @Test
  void documentWithTooManyPagesIsStopped() {
    DocMint docMint = DocMint.builder().limits(ResourceLimits.defaults().withMaxPages(5)).build();

    assertThatThrownBy(() -> docMint.pdf("security/many-pages", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> {
              assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.PAGES);
              assertThat(e.configuredValue()).isEqualTo("5");
            })
        .hasMessageContaining("docmint.limits.max-pages");
  }

  @Test
  void slowRenderIsStoppedByTimeLimit() {
    DocMint docMint =
        DocMint.builder()
            .limits(ResourceLimits.defaults().withMaxRenderTime(Duration.ofMillis(1)))
            .build();

    assertThatThrownBy(() -> docMint.pdf("security/many-pages", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.RENDER_TIME))
        .hasMessageContaining("docmint.limits.max-render-time");
  }

  @Test
  void limitsAreValidated() {
    assertThatThrownBy(() -> ResourceLimits.defaults().withMaxPages(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResourceLimits.defaults().withMaxTemplateBytes(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ResourceLimits.defaults().withMaxRenderTime(Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
