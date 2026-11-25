package io.github.juste120.docmint.infrastructure.pdf;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Turns the template's header and footer elements into repeated page headers and footers.
 *
 * <p>An element with class {@code docmint-header} (or {@code docmint-footer}) is moved to the top
 * (or bottom) margin of every page with CSS running elements. The modifier {@code
 * docmint-first-page} restricts it to page 1, {@code docmint-not-first-page} excludes page 1.
 * Without any custom footer, the default "Page X / Y" footer is used, exactly as in V1.
 */
final class PageMarginBoxes {

  static final String HEADER = "docmint-header";
  static final String FOOTER = "docmint-footer";
  static final String FIRST = "docmint-first-page";
  static final String NOT_FIRST = "docmint-not-first-page";

  private static final String DEFAULT_FOOTER =
      "@page { @bottom-center { content: \"Page \" counter(page) \" / \" counter(pages);"
          + " font-family: 'Noto Sans', sans-serif; font-size: 8pt; color: #555555; } }\n";

  private PageMarginBoxes() {}

  /**
   * Marks the header/footer elements as running elements and returns the matching page rules.
   *
   * @param document the parsed template output (modified in place)
   * @return the CSS to add to the default stylesheet
   */
  static String apply(Document document) {
    StringBuilder css = new StringBuilder();
    boolean customFooter = box(document, FOOTER, "bottom-center", "docmintFooter", css);
    box(document, HEADER, "top-center", "docmintHeader", css);
    if (!customFooter) {
      css.append(DEFAULT_FOOTER);
    }
    return css.toString();
  }

  private static boolean box(
      Document document, String type, String marginBox, String name, StringBuilder css) {
    Element all = first(document, type, null);
    Element onlyFirst = first(document, type, FIRST);
    Element notFirst = first(document, type, NOT_FIRST);
    if (all == null && onlyFirst == null && notFirst == null) {
      return false;
    }
    String other = notFirst != null ? run(notFirst, name + "Other") : run(all, name);
    String firstPage =
        onlyFirst != null ? run(onlyFirst, name + "First") : notFirst != null ? null : other;
    css.append(rule("@page", marginBox, other));
    css.append(rule("@page :first", marginBox, firstPage));
    return true;
  }

  /**
   * Returns the first element of a type with (or, if {@code modifier} is null, without) a modifier.
   */
  private static Element first(Document document, String type, String modifier) {
    for (Element element : document.getElementsByClass(type)) {
      boolean first = element.hasClass(FIRST);
      boolean notFirst = element.hasClass(NOT_FIRST);
      boolean matches = modifier == null ? !first && !notFirst : element.hasClass(modifier);
      if (matches) {
        return element;
      }
    }
    return null;
  }

  private static String run(Element element, String runningName) {
    if (element == null) {
      return null;
    }
    String style = element.attr("style");
    element.attr("style", "position: running(" + runningName + "); " + style);
    return runningName;
  }

  private static String rule(String selector, String marginBox, String runningName) {
    String content = runningName == null ? "none" : "element(" + runningName + ")";
    return selector + " { @" + marginBox + " { content: " + content + "; width: 100%; } }\n";
  }
}
