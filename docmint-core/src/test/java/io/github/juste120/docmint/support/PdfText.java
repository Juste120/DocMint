package io.github.juste120.docmint.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;

/** Reads generated PDFs in tests: text, pages, sizes, images. */
public final class PdfText {

  private PdfText() {}

  /** Returns the whole text, with no-break spaces normalised to plain spaces. */
  public static String text(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return normalise(new PDFTextStripper().getText(document));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Returns the text of one page (1-based). */
  public static String pageText(byte[] pdf, int page) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setStartPage(page);
      stripper.setEndPage(page);
      return normalise(stripper.getText(document));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Returns the text of one page (1-based), sorted by position on the page. */
  public static String sortedPageText(byte[] pdf, int page) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      stripper.setStartPage(page);
      stripper.setEndPage(page);
      return normalise(stripper.getText(document));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Returns the number of pages. */
  public static int pages(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return document.getNumberOfPages();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Returns the size of the first page, in PDF points. */
  public static PDRectangle firstPageSize(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return document.getPage(0).getMediaBox();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Returns the number of images drawn on the first page. */
  public static int imagesOnFirstPage(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDPage page = document.getPage(0);
      int count = 0;
      for (var name : page.getResources().getXObjectNames()) {
        if (page.getResources().isImageXObject(name)) {
          count++;
        }
      }
      return count;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Returns the names of the fonts used on the first page. */
  public static String fontsOnFirstPage(byte[] pdf) {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDPage page = document.getPage(0);
      StringBuilder names = new StringBuilder();
      for (var name : page.getResources().getFontNames()) {
        names.append(page.getResources().getFont(name).getName()).append(' ');
      }
      return names.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String normalise(String text) {
    return text.replace(' ', ' ').replace(' ', ' ');
  }
}
