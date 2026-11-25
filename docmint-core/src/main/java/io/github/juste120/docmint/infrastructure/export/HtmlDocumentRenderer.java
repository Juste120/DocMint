package io.github.juste120.docmint.infrastructure.export;

import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.domain.TemplateName;
import io.github.juste120.docmint.port.DocumentRenderer;
import io.github.juste120.docmint.port.RenderContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Returns the HTML produced by the template, as UTF-8 bytes (base of CSV, Excel and Word exports).
 *
 * <p>Images referenced relatively ({@code images/logo.png} or {@code /images/logo.png}) are read
 * from the templates folder and inlined as {@code data:} URIs, so that the HTML is self-contained.
 * Only paths inside the templates folder are read; other references are left untouched (they are
 * never fetched).
 */
public final class HtmlDocumentRenderer implements DocumentRenderer {

  private static final Map<String, String> IMAGE_TYPES =
      Map.of("png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "gif", "image/gif");

  private final String root;
  private final ClassLoader classLoader;
  private final long maxBytes;

  /**
   * Creates the renderer.
   *
   * @param root the templates folder on the class path
   * @param classLoader the class loader to read images from
   * @param maxBytes the maximum size of an inlined image
   */
  public HtmlDocumentRenderer(String root, ClassLoader classLoader, long maxBytes) {
    this.root = root;
    this.classLoader = classLoader;
    this.maxBytes = maxBytes;
  }

  @Override
  public OutputFormat format() {
    return OutputFormat.HTML;
  }

  @Override
  public RenderedDocument render(String html, RenderContext context) {
    Document document = Jsoup.parse(html);
    boolean changed = false;
    for (Element image : document.select("img[src]")) {
      String inlined = inline(image.attr("src"), context.templateName());
      if (inlined != null) {
        image.attr("src", inlined);
        changed = true;
      }
    }
    String result = changed ? document.outerHtml() : html;
    return new RenderedDocument(result.getBytes(StandardCharsets.UTF_8), OutputFormat.HTML, 1);
  }

  private String inline(String src, TemplateName template) {
    String value = src.strip();
    if (value.isEmpty() || value.contains(":") || value.startsWith("//")) {
      return null; // data:, http:, file:... are never read here
    }
    String path;
    try {
      String base = value.startsWith("/") ? "/" : "/" + template.folder();
      path =
          new URI(null, null, base, null)
              .resolve(new URI(value.replace(" ", "%20")))
              .normalize()
              .getPath();
    } catch (URISyntaxException | IllegalArgumentException e) {
      return null;
    }
    if (path == null || path.contains("..") || path.contains("\\") || !path.startsWith("/")) {
      return null;
    }
    String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    String mediaType = IMAGE_TYPES.get(extension);
    if (mediaType == null) {
      return null;
    }
    try (InputStream in = classLoader.getResourceAsStream(root + path)) {
      if (in == null) {
        return null;
      }
      byte[] bytes = in.readNBytes((int) Math.min(Integer.MAX_VALUE, maxBytes + 1));
      if (bytes.length > maxBytes) {
        return null;
      }
      return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
