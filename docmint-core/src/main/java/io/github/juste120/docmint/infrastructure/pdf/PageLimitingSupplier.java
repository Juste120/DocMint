package io.github.juste120.docmint.infrastructure.pdf;

import com.openhtmltopdf.pdfboxout.PageSupplier;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Creates PDF pages and stops the layout as soon as the page limit is exceeded or the render thread
 * is interrupted (render time limit), instead of producing a huge document first.
 */
final class PageLimitingSupplier implements PageSupplier {

  /** Internal signal, translated into a public exception by the renderer. */
  static final class PageLimitReached extends RuntimeException {
    private static final long serialVersionUID = 1L;

    PageLimitReached() {
      super(null, null, false, false);
    }
  }

  /** Internal signal raised when the render thread was interrupted. */
  static final class RenderInterrupted extends RuntimeException {
    private static final long serialVersionUID = 1L;

    RenderInterrupted() {
      super(null, null, false, false);
    }
  }

  private final int maxPages;
  private int pages;

  PageLimitingSupplier(int maxPages) {
    this.maxPages = maxPages;
  }

  @Override
  public PDPage requestPage(
      PDDocument document, float width, float height, int pageNumber, int shadowPageNumber) {
    if (Thread.currentThread().isInterrupted()) {
      throw new RenderInterrupted();
    }
    if (++pages > maxPages) {
      throw new PageLimitReached();
    }
    PDPage page = new PDPage(new PDRectangle(width, height));
    document.addPage(page);
    return page;
  }

  int pages() {
    return pages;
  }
}
