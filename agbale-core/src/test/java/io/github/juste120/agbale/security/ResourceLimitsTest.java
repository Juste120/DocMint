package io.github.juste120.agbale.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.LimitExceededException;
import io.github.juste120.agbale.domain.ResourceLimits;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-016: template size, rendered size, page count and render time are limited. */
class ResourceLimitsTest {

  @Test
  void templateLargerThanLimitIsRejected() {
    Agbale agbale =
        Agbale.builder().limits(ResourceLimits.defaults().withMaxTemplateBytes(50)).build();

    assertThatThrownBy(() -> agbale.pdf("invoice", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.TEMPLATE_SIZE))
        .hasMessageContaining("agbale.limits.max-template-size");
  }

  @Test
  void runawayLoopIsStoppedByRenderedSizeLimit() {
    Agbale agbale =
        Agbale.builder().limits(ResourceLimits.defaults().withMaxTemplateBytes(10_000)).build();

    assertThatThrownBy(() -> agbale.pdf("security/infinite", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.RENDERED_SIZE));
  }

  @Test
  void documentWithTooManyPagesIsStopped() {
    Agbale agbale = Agbale.builder().limits(ResourceLimits.defaults().withMaxPages(5)).build();

    assertThatThrownBy(() -> agbale.pdf("security/many-pages", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> {
              assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.PAGES);
              assertThat(e.configuredValue()).isEqualTo("5");
            })
        .hasMessageContaining("agbale.limits.max-pages");
  }

  @Test
  void slowRenderIsStoppedByTimeLimit() {
    Agbale agbale =
        Agbale.builder()
            .limits(ResourceLimits.defaults().withMaxRenderTime(Duration.ofMillis(1)))
            .build();

    assertThatThrownBy(() -> agbale.pdf("security/many-pages", Map.of()))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.RENDER_TIME))
        .hasMessageContaining("agbale.limits.max-render-time");
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
