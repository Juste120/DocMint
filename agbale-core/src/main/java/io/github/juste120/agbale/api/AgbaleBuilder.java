package io.github.juste120.agbale.api;

import io.github.juste120.agbale.application.DefaultReportService;
import io.github.juste120.agbale.application.RendererRegistry;
import io.github.juste120.agbale.domain.FontDefinition;
import io.github.juste120.agbale.domain.MissingResourceMode;
import io.github.juste120.agbale.domain.PageSettings;
import io.github.juste120.agbale.domain.ResourceAccessPolicy;
import io.github.juste120.agbale.domain.ResourceLimits;
import io.github.juste120.agbale.infrastructure.pdf.FontRegistry;
import io.github.juste120.agbale.infrastructure.pdf.OpenHtmlPdfRenderer;
import io.github.juste120.agbale.infrastructure.pebble.PebbleTemplateEngine;
import io.github.juste120.agbale.infrastructure.template.ClasspathTemplateResolver;
import io.github.juste120.agbale.infrastructure.template.ConcurrentTemplateCache;
import io.github.juste120.agbale.port.DocumentRenderer;
import io.github.juste120.agbale.port.TemplateResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures an {@link Agbale} instance. Every setting is optional.
 *
 * <pre>{@code
 * Agbale agbale = Agbale.builder()
 *     .templatesPath("documents")          // default "reports"
 *     .locale(Locale.ENGLISH)              // default French
 *     .limits(ResourceLimits.defaults().withMaxPages(50))
 *     .build();
 * }</pre>
 *
 * <p>A builder is not thread-safe; the built instance is.
 */
public final class AgbaleBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(AgbaleBuilder.class);

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

  AgbaleBuilder() {}

  /**
   * Sets the class path folder containing the templates.
   *
   * @param classpathFolder for example {@code reports} (default) or {@code templates/documents}
   * @return this builder
   */
  public AgbaleBuilder templatesPath(String classpathFolder) {
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
  public AgbaleBuilder cache(boolean enabled) {
    this.cache = enabled;
    return this;
  }

  /**
   * Sets the default locale for dates and amounts (French by default).
   *
   * @param defaultLocale the locale
   * @return this builder
   */
  public AgbaleBuilder locale(Locale defaultLocale) {
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
  public AgbaleBuilder font(String family, String classpathLocation) {
    return font(FontDefinition.regular(family, classpathLocation));
  }

  /**
   * Adds a font (any weight or style) from the class path.
   *
   * @param font the font definition
   * @return this builder
   */
  public AgbaleBuilder font(FontDefinition font) {
    fonts.add(Objects.requireNonNull(font, "font"));
    return this;
  }

  /**
   * Sets the resource limits (template size, pages, render time).
   *
   * @param resourceLimits the limits
   * @return this builder
   */
  public AgbaleBuilder limits(ResourceLimits resourceLimits) {
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
  public AgbaleBuilder allowRemoteHost(String host) {
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
  public AgbaleBuilder missingResources(MissingResourceMode mode) {
    this.missingResources = Objects.requireNonNull(mode, "mode");
    return this;
  }

  /**
   * Sets the default page layout (A4 portrait by default). Template CSS can still override it.
   *
   * @param settings the page settings
   * @return this builder
   */
  public AgbaleBuilder pageSettings(PageSettings settings) {
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
  public AgbaleBuilder classLoader(ClassLoader loader) {
    this.classLoader = Objects.requireNonNull(loader, "classLoader");
    return this;
  }

  /**
   * Replaces the default class path template resolver (expert use).
   *
   * @param resolver the resolver
   * @return this builder
   */
  public AgbaleBuilder templateResolver(TemplateResolver resolver) {
    this.templateResolver = Objects.requireNonNull(resolver, "resolver");
    return this;
  }

  /**
   * Registers a document renderer, replacing the default one for its format (expert use).
   *
   * @param renderer the renderer
   * @return this builder
   */
  public AgbaleBuilder renderer(DocumentRenderer renderer) {
    customRenderers.add(Objects.requireNonNull(renderer, "renderer"));
    return this;
  }

  /**
   * Builds the instance.
   *
   * @return an immutable, thread-safe instance
   * @throws IllegalArgumentException if a setting is invalid (e.g. a font file does not exist)
   */
  public Agbale build() {
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
    return new Agbale(
        new DefaultReportService(
            resolver, engine, new RendererRegistry(renderers), locale, limits, loader));
  }

  private static ClassLoader defaultClassLoader() {
    ClassLoader context = Thread.currentThread().getContextClassLoader();
    return context != null ? context : AgbaleBuilder.class.getClassLoader();
  }
}
