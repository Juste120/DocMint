package io.github.juste120.docmint.word;

import io.github.juste120.docmint.api.RenderException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Converts the HTML produced by a template into a Word document.
 *
 * <p>Supported: headings, paragraphs and blocks, bold/italic/underline, line breaks, lists, tables
 * (bold header rows, {@code colspan}), {@code data:} images (logos, QR codes, charts), {@code
 * .page-break}, and {@code docmint-header} / {@code docmint-footer} with {@code docmint-page} /
 * {@code docmint-pages} numbers. Other elements keep their text. Data is always inserted as text:
 * the document contains no macro and no field built from data.
 */
final class HtmlToDocx {

  private static final double[] HEADING_SIZES = {20, 16, 14, 12, 11, 11};
  private static final Pattern LENGTH = Pattern.compile("(\\d+(?:\\.\\d+)?)(mm|cm|in|pt|px)");
  private static final Pattern DATA_URI =
      Pattern.compile("data:image/(png|jpe?g|gif);base64,(.+)", Pattern.DOTALL);
  private static final double MAX_WIDTH_PT = 450;

  private record Style(boolean bold, boolean italic, boolean underline, double size) {
    Style with(String tag) {
      return switch (tag) {
        case "b", "strong", "th" -> new Style(true, italic, underline, size);
        case "i", "em" -> new Style(bold, true, underline, size);
        case "u" -> new Style(bold, italic, true, size);
        default -> this;
      };
    }
  }

  private static final Style NORMAL = new Style(false, false, false, 0);

  byte[] convert(String html) {
    Document source = Jsoup.parse(html);
    try (XWPFDocument docx = new XWPFDocument()) {
      for (Element header : source.select(".docmint-header")) {
        marginBox(docx.createHeader(HeaderFooterType.DEFAULT), header);
        header.remove();
        break;
      }
      for (Element footer : source.select(".docmint-footer")) {
        marginBox(docx.createFooter(HeaderFooterType.DEFAULT), footer);
        footer.remove();
        break;
      }
      source.select(".docmint-header, .docmint-footer, head, script, style, template").remove();
      blocks(source.body(), docx::createParagraph, docx);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      docx.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RenderException("The Word document could not be written.", e);
    }
  }

  private void marginBox(XWPFHeaderFooter box, Element element) {
    XWPFParagraph paragraph = box.createParagraph();
    for (Node child : element.childNodes()) {
      if (child instanceof Element e && e.hasClass("docmint-page")) {
        paragraph.createRun();
        paragraph.getCTP().addNewFldSimple().setInstr("PAGE");
      } else if (child instanceof Element e && e.hasClass("docmint-pages")) {
        paragraph.getCTP().addNewFldSimple().setInstr("NUMPAGES");
      } else if (child instanceof Element e && e.tagName().equals("img")) {
        image(paragraph, e);
      } else if (child instanceof Element e) {
        inline(paragraph, e, NORMAL);
      } else if (child instanceof TextNode text) {
        run(paragraph, normalize(text.getWholeText()), NORMAL);
      }
    }
  }

  /** Converts the children of a block element. */
  private void blocks(Element parent, Supplier<XWPFParagraph> newParagraph, XWPFDocument docx) {
    XWPFParagraph current = null;
    for (Node child : parent.childNodes()) {
      if (child instanceof TextNode text) {
        String value = normalize(text.getWholeText());
        if (!value.isBlank()) {
          current = current == null ? newParagraph.get() : current;
          run(current, value, NORMAL);
        }
        continue;
      }
      if (!(child instanceof Element element)) {
        continue;
      }
      String tag = element.tagName().toLowerCase(Locale.ROOT);
      if (!element.isBlock() && !tag.equals("table") && !tag.equals("img")) {
        current = current == null ? newParagraph.get() : current;
        inline(current, element, NORMAL);
        continue;
      }
      current = null;
      if (element.hasClass("page-break")) {
        newParagraph.get().createRun().addBreak(BreakType.PAGE);
      } else if (tag.matches("h[1-6]")) {
        XWPFParagraph heading = newParagraph.get();
        double size = HEADING_SIZES[tag.charAt(1) - '1'];
        inline(heading, element, new Style(true, false, false, size));
      } else if (tag.equals("ul") || tag.equals("ol")) {
        list(element, tag.equals("ol"), newParagraph);
      } else if (tag.equals("table") && docx != null) {
        table(docx, element);
      } else if (tag.equals("table")) {
        newParagraph.get().createRun().setText(normalize(element.text()));
      } else if (tag.equals("img")) {
        image(newParagraph.get(), element);
      } else if (tag.equals("br") || tag.equals("hr")) {
        newParagraph.get();
      } else if (hasBlockChildren(element)) {
        blocks(element, newParagraph, docx);
      } else {
        inline(newParagraph.get(), element, NORMAL);
      }
    }
  }

  private static boolean hasBlockChildren(Element element) {
    for (Element child : element.children()) {
      if (child.isBlock() || child.tagName().equals("table") || child.tagName().equals("img")) {
        return true;
      }
    }
    return false;
  }

  private void list(Element list, boolean ordered, Supplier<XWPFParagraph> newParagraph) {
    int number = 1;
    for (Element item : list.children()) {
      if (!item.tagName().equals("li")) {
        continue;
      }
      XWPFParagraph paragraph = newParagraph.get();
      paragraph.setIndentationLeft(360);
      run(paragraph, ordered ? (number++) + ". " : "• ", NORMAL);
      inline(paragraph, item, NORMAL);
    }
  }

  private void table(XWPFDocument docx, Element table) {
    List<Element> rows = table.select("> thead > tr, > tbody > tr, > tr, > tfoot > tr");
    if (rows.isEmpty()) {
      return;
    }
    int columns = 1;
    for (Element row : rows) {
      int width = 0;
      for (Element cell : row.select("> th, > td")) {
        width += span(cell);
      }
      columns = Math.max(columns, width);
    }
    XWPFTable result = docx.createTable(rows.size(), columns);
    result.setWidth("100%");
    for (int r = 0; r < rows.size(); r++) {
      Element row = rows.get(r);
      Element section = row.parent();
      boolean header = section != null && section.tagName().equals("thead");
      XWPFTableRow target = result.getRow(r);
      int column = 0;
      for (Element cell : row.select("> th, > td")) {
        if (column >= target.getTableCells().size()) {
          break;
        }
        XWPFTableCell targetCell = target.getCell(column);
        int span = span(cell);
        if (span > 1) {
          targetCell.getCTTc().addNewTcPr().addNewGridSpan().setVal(BigInteger.valueOf(span));
          for (int extra = 1; extra < span && column + 1 < target.getTableCells().size(); extra++) {
            target.removeCell(column + 1);
          }
        }
        XWPFParagraph paragraph = targetCell.getParagraphs().get(0);
        Style style = header || cell.tagName().equals("th") ? NORMAL.with("b") : NORMAL;
        if (cell.select("img").isEmpty()) {
          inline(paragraph, cell, style);
        } else {
          for (Element image : cell.select("img")) {
            image(paragraph, image);
          }
        }
        column++;
      }
    }
  }

  private static int span(Element cell) {
    try {
      return Math.max(1, Math.min(50, Integer.parseInt(cell.attr("colspan").strip())));
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private void inline(XWPFParagraph paragraph, Element element, Style style) {
    Style current = style.with(element.tagName().toLowerCase(Locale.ROOT));
    for (Node child : element.childNodes()) {
      if (child instanceof TextNode text) {
        String value = normalize(text.getWholeText());
        if (!value.isEmpty()) {
          run(paragraph, value, current);
        }
      } else if (child instanceof Element e) {
        switch (e.tagName().toLowerCase(Locale.ROOT)) {
          case "br" -> paragraph.createRun().addBreak();
          case "img" -> image(paragraph, e);
          case "table", "ul", "ol" -> run(paragraph, normalize(e.text()), current);
          default -> inline(paragraph, e, current);
        }
      }
    }
  }

  private static void run(XWPFParagraph paragraph, String text, Style style) {
    XWPFRun run = paragraph.createRun();
    run.setText(text);
    run.setBold(style.bold());
    run.setItalic(style.italic());
    if (style.underline()) {
      run.setUnderline(UnderlinePatterns.SINGLE);
    }
    if (style.size() > 0) {
      run.setFontSize(style.size());
    }
  }

  private static void image(XWPFParagraph paragraph, Element img) {
    Matcher data = DATA_URI.matcher(img.attr("src").strip());
    if (!data.matches()) {
      return; // only inlined images (data: URIs) are embedded; nothing is ever fetched
    }
    byte[] bytes;
    try {
      bytes = Base64.getMimeDecoder().decode(data.group(2));
    } catch (IllegalArgumentException e) {
      return;
    }
    double[] size = size(img, bytes);
    PictureType type =
        switch (data.group(1)) {
          case "png" -> PictureType.PNG;
          case "gif" -> PictureType.GIF;
          default -> PictureType.JPEG;
        };
    try {
      paragraph
          .createRun()
          .addPicture(
              new ByteArrayInputStream(bytes),
              type,
              "image",
              Units.toEMU(size[0]),
              Units.toEMU(size[1]));
    } catch (IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
      // unreadable image: skipped, like a browser would
    }
  }

  /** Width and height in points, from the style, the attributes or the image itself. */
  private static double[] size(Element img, byte[] bytes) {
    double width = points(styleValue(img, "width"), img.attr("width"));
    double height = points(styleValue(img, "height"), img.attr("height"));
    double ratio = 1;
    try {
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image != null) {
        ratio = (double) image.getHeight() / image.getWidth();
        if (width <= 0 && height <= 0) {
          width = image.getWidth() * 0.75;
        }
      }
    } catch (IOException e) {
      // keep defaults
    }
    if (width <= 0) {
      width = height > 0 ? height / ratio : 100;
    }
    if (height <= 0) {
      height = width * ratio;
    }
    if (width > MAX_WIDTH_PT) {
      height = height * MAX_WIDTH_PT / width;
      width = MAX_WIDTH_PT;
    }
    return new double[] {width, height};
  }

  private static String styleValue(Element element, String property) {
    for (String declaration : element.attr("style").split(";")) {
      String[] parts = declaration.split(":", 2);
      if (parts.length == 2 && parts[0].strip().equalsIgnoreCase(property)) {
        return parts[1].strip();
      }
    }
    return "";
  }

  private static double points(String css, String attribute) {
    String value = !css.isEmpty() ? css : attribute.isBlank() ? "" : attribute.strip() + "px";
    Matcher matcher = LENGTH.matcher(value);
    if (!matcher.matches()) {
      return 0;
    }
    double number = Double.parseDouble(matcher.group(1));
    return switch (matcher.group(2)) {
      case "mm" -> number * 72 / 25.4;
      case "cm" -> number * 72 / 2.54;
      case "in" -> number * 72;
      case "px" -> number * 0.75;
      default -> number;
    };
  }

  private static String normalize(String text) {
    return text.replaceAll("\\s+", " ");
  }
}
