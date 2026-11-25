package io.github.juste120.agbale.spring.web;

/** How the browser should handle the document ({@code Content-Disposition} type). */
public enum Disposition {

  /** Display the document in the browser (default). */
  INLINE,

  /** Download the document as a file. */
  ATTACHMENT
}
