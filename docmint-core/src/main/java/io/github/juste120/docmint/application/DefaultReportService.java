package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.InvalidTemplateNameException;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.api.TemplateNotFoundException;
import io.github.juste120.docmint.domain.DocumentOptions;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.TemplateName;
import io.github.juste120.docmint.domain.Watermark;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import io.github.juste120.docmint.port.RenderContext;
import io.github.juste120.docmint.port.TemplateEngine;
import io.github.juste120.docmint.port.TemplateResolver;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default use case implementation: validate the name, convert the data, produce HTML with the
 * template engine, then the document with the renderer of the requested format, within the time
 * limit.
 */
public final class DefaultReportService implements ReportService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultReportService.class);

  private final TemplateResolver resolver;
  private final TemplateEngine engine;
  private final RendererRegistry renderers;
  private final Locale defaultLocale;
  private final ResourceLimits limits;
  private final TemplateModelMapper modelMapper = new TemplateModelMapper();
  private final RenderResponseMapper responseMapper = new RenderResponseMapper();
  private final TimeLimiter timeLimiter;
  private final List<PostProcessor> postProcessors;
  private final Watermark defaultWatermark;

  /**
   * Creates the service.
   *
   * @param resolver the template resolver (used for listings and suggestions)
   * @param engine the template engine
   * @param renderers the renderers by format
   * @param defaultLocale the locale used when a request does not specify one
   * @param limits the resource limits
   * @param classLoader the class loader set as context class loader during renders
   * @param postProcessors the post-processors, in order
   * @param defaultWatermark the watermark applied when a request has none, or {@code null}
   */
  public DefaultReportService(
      TemplateResolver resolver,
      TemplateEngine engine,
      RendererRegistry renderers,
      Locale defaultLocale,
      ResourceLimits limits,
      ClassLoader classLoader,
      List<PostProcessor> postProcessors,
      Watermark defaultWatermark) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.engine = Objects.requireNonNull(engine, "engine");
    this.renderers = Objects.requireNonNull(renderers, "renderers");
    this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.timeLimiter = new TimeLimiter(classLoader);
    this.postProcessors = List.copyOf(postProcessors);
    this.defaultWatermark = defaultWatermark;
  }

  @Override
  public RenderResponse render(RenderRequest request) {
    Objects.requireNonNull(request, "request");
    TemplateName name = parse(request.templateName());
    Locale locale = request.options().locale() == null ? defaultLocale : request.options().locale();
    DocumentOptions options = request.options().documentOptions(defaultWatermark);
    if (options.protection() != null && request.format() != OutputFormat.PDF) {
      throw new InvalidOptionsException(
          InvalidOptionsException.PROTECTION_INVALID,
          "Password protection only applies to PDF documents, not to "
              + request.format()
              + ". Remove the protection from this request.");
    }
    var renderer = renderers.get(request.format().baseFormat());
    PostProcessContext postContext =
        new PostProcessContext(name, request.format(), options, limits);
    long start = System.nanoTime();
    RenderedDocument document;
    try {
      document =
          timeLimiter.call(
              () -> {
                var model = modelMapper.toModel(request.data());
                String html = engine.render(name, model, locale);
                RenderedDocument rendered =
                    renderer.render(html, new RenderContext(name, locale, limits, options));
                return postProcess(rendered, postContext);
              },
              limits.maxRenderTime(),
              () ->
                  new LimitExceededException(
                      name.value(),
                      LimitExceededException.Limit.RENDER_TIME,
                      limits.maxRenderTime().toString()));
    } catch (TemplateNotFoundException e) {
      throw withSuggestions(e);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(
          "Rendered template '{}' as {} in {} ms ({} pages)",
          name.value(),
          request.format(),
          (System.nanoTime() - start) / 1_000_000,
          document.pageCount());
    }
    return responseMapper.toResponse(document, name, request.options());
  }

  private RenderedDocument postProcess(RenderedDocument rendered, PostProcessContext context) {
    RenderedDocument document = rendered;
    for (PostProcessor postProcessor : postProcessors) {
      if (postProcessor.appliesTo(document, context)) {
        document = postProcessor.process(document, context);
      }
    }
    if (document.format() != context.requestedFormat()) {
      throw new RenderException(
          "No post-processor converts "
              + document.format()
              + " to "
              + context.requestedFormat()
              + ". Register a PostProcessor for this format.",
          null);
    }
    return document;
  }

  @Override
  public List<String> availableTemplates() {
    return resolver.availableTemplates();
  }

  @Override
  public void clearCache() {
    engine.clearCache();
  }

  private static TemplateName parse(String raw) {
    try {
      return TemplateName.of(raw);
    } catch (IllegalArgumentException e) {
      throw new InvalidTemplateNameException(raw, e.getMessage());
    }
  }

  private TemplateNotFoundException withSuggestions(TemplateNotFoundException e) {
    List<String> available = resolver.availableTemplates();
    String suggestion = NameSuggester.suggest(e.templateName(), available);
    return new TemplateNotFoundException(
        e.templateName(), resolver.location(), available, suggestion);
  }
}
