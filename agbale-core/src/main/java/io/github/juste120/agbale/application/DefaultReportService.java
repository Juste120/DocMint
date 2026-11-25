package io.github.juste120.agbale.application;

import io.github.juste120.agbale.api.InvalidTemplateNameException;
import io.github.juste120.agbale.api.LimitExceededException;
import io.github.juste120.agbale.api.RenderRequest;
import io.github.juste120.agbale.api.RenderResponse;
import io.github.juste120.agbale.api.TemplateNotFoundException;
import io.github.juste120.agbale.domain.RenderedDocument;
import io.github.juste120.agbale.domain.ResourceLimits;
import io.github.juste120.agbale.domain.TemplateName;
import io.github.juste120.agbale.port.RenderContext;
import io.github.juste120.agbale.port.TemplateEngine;
import io.github.juste120.agbale.port.TemplateResolver;
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

  /**
   * Creates the service.
   *
   * @param resolver the template resolver (used for listings and suggestions)
   * @param engine the template engine
   * @param renderers the renderers by format
   * @param defaultLocale the locale used when a request does not specify one
   * @param limits the resource limits
   * @param classLoader the class loader set as context class loader during renders
   */
  public DefaultReportService(
      TemplateResolver resolver,
      TemplateEngine engine,
      RendererRegistry renderers,
      Locale defaultLocale,
      ResourceLimits limits,
      ClassLoader classLoader) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.engine = Objects.requireNonNull(engine, "engine");
    this.renderers = Objects.requireNonNull(renderers, "renderers");
    this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.timeLimiter = new TimeLimiter(classLoader);
  }

  @Override
  public RenderResponse render(RenderRequest request) {
    Objects.requireNonNull(request, "request");
    TemplateName name = parse(request.templateName());
    Locale locale = request.options().locale() == null ? defaultLocale : request.options().locale();
    var renderer = renderers.get(request.format());
    long start = System.nanoTime();
    RenderedDocument document;
    try {
      document =
          timeLimiter.call(
              () -> {
                var model = modelMapper.toModel(request.data());
                String html = engine.render(name, model, locale);
                return renderer.render(html, new RenderContext(name, locale, limits));
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
