package io.github.juste120.docmint.domain;

/** What a reader may do with a protected PDF without the owner password. */
public enum Permission {
  /** Print the document. */
  PRINT,
  /** Print at full quality (otherwise degraded printing only). */
  PRINT_HIGH_QUALITY,
  /** Copy text and images. */
  COPY,
  /** Modify the content. */
  MODIFY,
  /** Add or modify annotations. */
  ANNOTATE,
  /** Fill form fields. */
  FILL_FORMS,
  /** Extract content for accessibility tools. */
  EXTRACT_FOR_ACCESSIBILITY,
  /** Insert, rotate or delete pages. */
  ASSEMBLE
}
