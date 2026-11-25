package io.github.juste120.docmint.api;

import io.github.juste120.docmint.application.DefaultReportService;
import io.github.juste120.docmint.application.RendererRegistry;
import io.github.juste120.docmint.domain.FontDefinition;
import io.github.juste120.docmint.domain.MissingResourceMode;
import io.github.juste120.docmint.domain.PageSettings;
import io.github.juste120.docmint.domain.ResourceAccessPolicy;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.Watermark;
import io.github.juste120.docmint.infrastructure.pdf.FontRegistry;
import io.github.juste120.docmint.infrastructure.pdf.OpenHtmlPdfRenderer;
import io.github.juste120.docmint.infrastructure.pdf.PdfProtectionPostProcessor;
import io.github.juste120.docmint.infrastructure.pdf.PdfWatermarkPostProcessor;
import io.github.juste120.docmint.infrastructure.pdf.PngPreviewPostProcessor;
import io.github.juste120.docmint.infrastructure.pebble.PebbleTemplateEngine;
import io.github.juste120.docmint.infrastructure.template.ClasspathTemplateResolver;
import io.github.juste120.docmint.infrastructure.template.ConcurrentTemplateCache;
import io.github.juste120.docmint.port.DocumentRenderer;
import io.github.juste120.docmint.port.PostProcessor;
import io.github.juste120.docmint.port.TemplateResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures an {@link DocMint} instance. Every setting is optional.
 *
 * <pre>{@code
 * DocMint docMint = DocMint.builder()
 *     .templatesPath("documents")          // default "reports"
 *     .locale(Locale.ENGLISH)              // default French
 *     .limits(ResourceLimits.defaults().withMaxPages(50))
 *     .build();
 * }</pre>
 *
 * <p>A builder is not thread-safe; the built instance is.
 */
public final class DocMintBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(DocMintBuilder.class);

  /** Default class path folder of templates. */
  public static final String DEFAULT_TEMPLATES_PATH = "reports";

  private String templatesPath = DEFAULT_TEMPLATES_PATH;
  private boolean cache = true;
  private Locale locale = Locale.FRENCH;
  private final List<FontDefinition> fonts = new ArrayList<>();
  private ResourceLimits limits = ResourceLimits.defaults();
  private ResourceAccessPolicy policy = ResourceAccessPolicy.denyAllRemote();
  private MissingResourceMode missingResources = MissingResourceMode.FAIL;
  private PageSettings pageSettings = PageSettings.defaults();
  private ClassLoader classLoader;
  private TemplateResolver templateResolver;
  private final List<DocumentRenderer> customRenderers = new ArrayList<>();
  private final List<PostProcessor> customPostProcessors = new ArrayList<>();
  private Watermark defaultWatermark;

  DocMintBuilder() {}

  /**
   * Sets the class path folder containing the templates.
   *
   * @param classpathFolder for example {@code reports} (default) or {@code templates/documents}
   * @return this builder
   */
  public DocMintBuilder templatesPath(String classpathFolder) {
    this.templatesPath = ClasspathTemplateResolver.normaliseRoot(classpathFolder);
    return this;
  }

  /**
   * Enables or disables the cache of compiled templates (enabled by default). Disable it during
   * development to see template changes without restarting.
   *
   * @param enabled whether to cache compiled templates
   * @return this builder
   */
  public DocMintBuilder cache(boolean enabled) {
    this.cache = enabled;
    return this;
  }

  /**
   * Sets the default locale for dates and amounts (French by default).
   *
   * @param defaultLocale the locale
   * @return this builder
   */
  public DocMintBuilder locale(Locale defaultLocale) {
    this.locale = Objects.requireNonNull(defaultLocale, "locale");
    return this;
  }

  /**
   * Adds a regular font from the class path, usable in CSS with {@code font-family: <family>}.
   *
   * @param family the CSS family name
   * @param classpathLocation the TrueType file, e.g. {@code fonts/Inter-Regular.ttf}
   * @return this builder
   */
  public DocMintBuilder font(String family, String classpathLocation) {
    return font(FontDefinition.regular(family, classpathLocation));
  }

  /**
   * Adds a font (any weight or style) from the class path.
   *
   * @param font the font definition
   * @return this builder
   */
  public DocMintBuilder font(FontDefinition font) {
    fonts.add(Objects.requireNonNull(font, "font"));
    return this;
  }

  /**
   * Sets the resource limits (template size, pages, render time).
   *
   * @param resourceLimits the limits
   * @return this builder
   */
  public DocMintBuilder limits(ResourceLimits resourceLimits) {
    this.limits = Objects.requireNonNull(resourceLimits, "limits");
    return this;
  }

  /**
   * Allows documents to load images, stylesheets and fonts over {@code https} from a host. By
   * default no remote host is allowed (protection against SSRF).
   *
   * @param host an exact host such as {@code cdn.example.com}, or {@code *.example.com} for its
   *     sub-domains
   * @return this builder
   */
  public DocMintBuilder allowRemoteHost(String host) {
    this.policy = policy.allow(host);
    return this;
  }

  /**
   * Sets what happens when an allowed resource cannot be found ({@link MissingResourceMode#FAIL} by
   * default). Refused resources always fail.
   *
   * @param mode the mode
   * @return this builder
   */
  public DocMintBuilder missingResources(MissingResourceMode mode) {
    this.missingResources = Objects.requireNonNull(mode, "mode");
    return this;
  }

  /**
   * Sets the default page layout (A4 portrait by default). Template CSS can still override it.
   *
   * @param settings the page settings
   * @return this builder
   */
  public DocMintBuilder pageSettings(PageSettings settings) {
    this.pageSettings = Objects.requireNonNull(settings, "pageSettings");
    return this;
  }

  /**
   * Sets the class loader used to find templates, images and fonts (by default the thread context
   * class loader at build time).
   *
   * @param loader the class loader
   * @return this builder
   */
  public DocMintBuilder classLoader(ClassLoader loader) {
    this.classLoader = Objects.requireNonNull(loader, "classLoader");
    return this;
  }

  /**
   * Replaces the default class path template resolver (expert use).
   *
   * @param resolver the resolver
   * @return this builder
   */
  public DocMintBuilder templateResolver(TemplateResolver resolver) {
    this.templateResolver = Objects.requireNonNull(resolver, "resolver");
    return this;
  }

  /**
   * Registers a document renderer, replacing the default one for its format (expert use).
   *
   * @param renderer the renderer
   * @return this builder
   */
  public DocMintBuilder renderer(DocumentRenderer renderer) {
    customRenderers.add(Objects.requireNonNull(renderer, "renderer"));
    return this;
  }

  /**
   * Sets a watermark applied to every document that does not specify one (e.g. {@code TEST} on a
   * staging server). None by default.
   *
   * @param watermark the watermark, or {@code null} for none
   * @return this builder
   */
  public DocMintBuilder defaultWatermark(Watermark watermark) {
    this.defaultWatermark = watermark;
    return this;
  }

  /**
   * Adds a post-processor, run after the built-in ones (expert use).
   *
   * @param postProcessor the post-processor
   * @return this builder
   */
  public DocMintBuilder postProcessor(PostProcessor postProcessor) {
    customPostProcessors.add(Objects.requireNonNull(postProcessor, "postProcessor"));
    return this;
  }

  /**
   * Builds the instance.
   *
   * @return an immutable, thread-safe instance
   * @throws IllegalArgumentException if a setting is invalid (e.g. a font file does not exist)
   */
  public DocMint build() {
    ClassLoader loader = classLoader != null ? classLoader : defaultClassLoader();
    TemplateResolver resolver =
        templateResolver != null
            ? templateResolver
            : new ClasspathTemplateResolver(templatesPath, loader, limits.maxTemplateBytes());
    var engine =
        new PebbleTemplateEngine(
            resolver, cache ? new ConcurrentTemplateCache() : null, limits, locale);
    List<DocumentRenderer> renderers = new ArrayList<>();
    renderers.add(
        new OpenHtmlPdfRenderer(
            templatesPath,
            loader,
            policy,
            missingResources,
            pageSettings,
            new FontRegistry(fonts, loader)));
    renderers.addAll(customRenderers);
    if (policy.hasWildcard()) {
      LOG.warn(
          "Remote resources are allowed for wildcard hosts {}: make sure every sub-domain is"
              + " trusted",
          policy.allowedHosts());
    }
    // Order matters: watermark before encryption; conversion to an image last.
    List<PostProcessor> postProcessors = new ArrayList<>();
    postProcessors.add(new PdfWatermarkPostProcessor(templatesPath, loader));
    postProcessors.add(new PdfProtectionPostProcessor());
    postProcessors.addAll(customPostProcessors);
    postProcessors.add(new PngPreviewPostProcessor());
    return new DocMint(
        new DefaultReportService(
            resolver,
            engine,
            new RendererRegistry(renderers),
            locale,
            limits,
            loader,
            postProcessors,
            defaultWatermark));
  }

  private static ClassLoader defaultClassLoader() {
    ClassLoader context = Thread.currentThread().getContextClassLoader();
    return context != null ? context : DocMintBuilder.class.getClassLoader();
  }
}
