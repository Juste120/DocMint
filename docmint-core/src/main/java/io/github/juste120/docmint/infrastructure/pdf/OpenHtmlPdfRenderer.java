package io.github.juste120.docmint.infrastructure.pdf;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.github.juste120.docmint.api.DocMintException;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.ResourceMissingException;
import io.github.juste120.docmint.api.ResourceRefusedException;
import io.github.juste120.docmint.domain.MissingResourceMode;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.PageSettings;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.domain.ResourceAccessPolicy;
import io.github.juste120.docmint.port.DocumentRenderer;
import io.github.juste120.docmint.port.RenderContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DocumentRenderer} producing PDF with OpenHTMLtoPDF (PDFBox).
 *
 * <p>HTML is parsed with jsoup, like a browser would, so templates can be written in plain HTML5.
 * jsoup is not an XML parser: it never reads DTDs or external entities, which makes XXE attacks
 * impossible. DocMint's default print stylesheet is inserted first in {@code <head>}, so template
 * styles override it.
 */
public final class OpenHtmlPdfRenderer implements DocumentRenderer {

  private static final Logger LOG = LoggerFactory.getLogger(OpenHtmlPdfRenderer.class);

  static {
    Slf4jXrLogger.installIfDefault();
  }

  private final String root;
  private final ClassLoader classLoader;
  private final ResourceAccessPolicy policy;
  private final MissingResourceMode missingMode;
  private final String defaultCss;
  private final FontRegistry fonts;

  /**
   * Creates the renderer.
   *
   * @param root the templates root on the class path (relative resources are read below it)
   * @param classLoader the class loader to read resources from
   * @param policy the remote resource policy
   * @param missingMode what to do with missing resources
   * @param pageSettings the default page layout
   * @param fonts the fonts to embed
   */
  public OpenHtmlPdfRenderer(
      String root,
      ClassLoader classLoader,
      ResourceAccessPolicy policy,
      MissingResourceMode missingMode,
      PageSettings pageSettings,
      FontRegistry fonts) {
    this.root = Objects.requireNonNull(root, "root");
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    this.policy = Objects.requireNonNull(policy, "policy");
    this.missingMode = Objects.requireNonNull(missingMode, "missingMode");
    this.defaultCss = DefaultStylesheet.css(Objects.requireNonNull(pageSettings, "pageSettings"));
    this.fonts = Objects.requireNonNull(fonts, "fonts");
  }

  @Override
  public OutputFormat format() {
    return OutputFormat.PDF;
  }

  @Override
  public RenderedDocument render(String html, RenderContext context) {
    String template = context.templateName().value();
    ResourceGuard guard = new ResourceGuard(root, context.templateName(), classLoader, policy);
    PageLimitingSupplier pages = new PageLimitingSupplier(context.limits().maxPages());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    PdfRendererBuilder builder = new PdfRendererBuilder();
    builder.withProducer("DocMint");
    builder.withW3cDocument(toW3c(html, defaultCss, guard.baseUri()), guard.baseUri());
    builder.useUriResolver(guard.uriResolver());
    builder.useProtocolsStreamImplementation(guard.classpathStreams(), ResourceGuard.SCHEME);
    builder.useExternalResourceAccessControl(
        guard::allows, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
    builder.withDiagnosticConsumer(guard.diagnostics());
    builder.usePageSupplier(pages);
    fonts.registerOn(builder);
    builder.toStream(out);

    try {
      builder.run();
    } catch (PageLimitingSupplier.PageLimitReached e) {
      throw pageLimit(template, context);
    } catch (PageLimitingSupplier.RenderInterrupted e) {
      throw new LimitExceededException(
          template,
          LimitExceededException.Limit.RENDER_TIME,
          context.limits().maxRenderTime().toString());
    } catch (IOException | RuntimeException e) {
      throw translate(e, template, context);
    }

    checkResources(guard, template);
    return new RenderedDocument(out.toByteArray(), OutputFormat.PDF, pages.pages());
  }

  private static org.w3c.dom.Document toW3c(String html, String css, String baseUri) {
    Document document = Jsoup.parse(html, baseUri);
    Element style = new Element("style").attr("data-docmint", "default");
    style.appendChild(new DataNode(css));
    document.head().prependChild(style);
    return new W3CDom().fromJsoup(document);
  }

  private void checkResources(ResourceGuard guard, String template) {
    List<String> refused = guard.refused();
    if (!refused.isEmpty()) {
      throw new ResourceRefusedException(template, refused);
    }
    List<String> missing = guard.missing();
    if (!missing.isEmpty()) {
      if (missingMode == MissingResourceMode.FAIL) {
        throw new ResourceMissingException(template, missing);
      }
      LOG.warn("Template '{}' rendered without missing resources: {}", template, missing);
    }
  }

  private static LimitExceededException pageLimit(String template, RenderContext context) {
    return new LimitExceededException(
        template,
        LimitExceededException.Limit.PAGES,
        Integer.toString(context.limits().maxPages()));
  }

  private static RuntimeException translate(Exception e, String template, RenderContext context) {
    Throwable current = e;
    for (int depth = 0; current != null && depth < 10; depth++) {
      if (current instanceof PageLimitingSupplier.PageLimitReached) {
        return pageLimit(template, context);
      }
      if (current instanceof PageLimitingSupplier.RenderInterrupted) {
        return new LimitExceededException(
            template,
            LimitExceededException.Limit.RENDER_TIME,
            context.limits().maxRenderTime().toString());
      }
      if (current instanceof DocMintException docMint) {
        return docMint;
      }
      current = current.getCause();
    }
    return new RenderException(
        "The PDF of template '"
            + template
            + "' could not be produced ("
            + e.getClass().getSimpleName()
            + "). Check the HTML and CSS of the template.",
        e);
  }
}
