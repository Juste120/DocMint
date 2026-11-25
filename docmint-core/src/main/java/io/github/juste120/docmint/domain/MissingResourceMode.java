package io.github.juste120.docmint.domain;

/**
 * What to do when a template references an allowed resource (image, stylesheet, font) that cannot
 * be found or read. A <em>refused</em> resource (forbidden scheme or host) always fails, whatever
 * this mode.
 */
public enum MissingResourceMode {

  /** Fail the render with an error naming the resource (default). */
  FAIL,

  /** Log a warning naming the resource and render the document without it. */
  WARN
}
