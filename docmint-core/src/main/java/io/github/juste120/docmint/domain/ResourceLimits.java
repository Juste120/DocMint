package io.github.juste120.docmint.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Limits protecting the host application against abusive or runaway templates.
 *
 * @param maxTemplateBytes maximum size in bytes of each template file (layouts and includes too)
 * @param maxPages maximum number of pages of a generated document
 * @param maxRenderTime maximum duration of one render
 */
public record ResourceLimits(long maxTemplateBytes, int maxPages, Duration maxRenderTime) {

  /** Default maximum template size: 1 MiB. */
  public static final long DEFAULT_MAX_TEMPLATE_BYTES = 1024L * 1024L;

  /** Default maximum page count: 500. */
  public static final int DEFAULT_MAX_PAGES = 500;

  /** Default maximum render time: 30 seconds. */
  public static final Duration DEFAULT_MAX_RENDER_TIME = Duration.ofSeconds(30);

  /** Ratio between the maximum rendered HTML size and the maximum template size. */
  private static final int RENDERED_SIZE_RATIO = 8;

  /**
   * Creates limits, validating them.
   *
   * @throws IllegalArgumentException if a limit is not strictly positive
   */
  public ResourceLimits {
    Objects.requireNonNull(maxRenderTime, "maxRenderTime");
    if (maxTemplateBytes <= 0) {
      throw new IllegalArgumentException("maxTemplateBytes must be > 0");
    }
    if (maxTemplateBytes > Integer.MAX_VALUE / RENDERED_SIZE_RATIO) {
      throw new IllegalArgumentException("maxTemplateBytes is too large");
    }
    if (maxPages <= 0) {
      throw new IllegalArgumentException("maxPages must be > 0");
    }
    if (maxRenderTime.isNegative() || maxRenderTime.isZero()) {
      throw new IllegalArgumentException("maxRenderTime must be > 0");
    }
  }

  /**
   * Returns the default limits: 1 MiB per template, 500 pages, 30 seconds.
   *
   * @return the default limits
   */
  public static ResourceLimits defaults() {
    return new ResourceLimits(
        DEFAULT_MAX_TEMPLATE_BYTES, DEFAULT_MAX_PAGES, DEFAULT_MAX_RENDER_TIME);
  }

  /**
   * Returns the maximum number of characters of the HTML produced by the template engine, derived
   * from the template size limit. It stops runaway loops before the PDF step.
   *
   * @return the maximum rendered HTML size, in characters
   */
  public int maxRenderedChars() {
    return (int) (maxTemplateBytes * RENDERED_SIZE_RATIO);
  }

  /**
   * Returns a copy with another template size limit.
   *
   * @param bytes the new limit
   * @return the new limits
   */
  public ResourceLimits withMaxTemplateBytes(long bytes) {
    return new ResourceLimits(bytes, maxPages, maxRenderTime);
  }

  /**
   * Returns a copy with another page limit.
   *
   * @param pages the new limit
   * @return the new limits
   */
  public ResourceLimits withMaxPages(int pages) {
    return new ResourceLimits(maxTemplateBytes, pages, maxRenderTime);
  }

  /**
   * Returns a copy with another render time limit.
   *
   * @param duration the new limit
   * @return the new limits
   */
  public ResourceLimits withMaxRenderTime(Duration duration) {
    return new ResourceLimits(maxTemplateBytes, maxPages, duration);
  }
}
