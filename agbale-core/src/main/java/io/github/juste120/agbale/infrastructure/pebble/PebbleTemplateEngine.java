package io.github.juste120.agbale.infrastructure.pebble;

import io.github.juste120.agbale.api.AgbaleException;
import io.github.juste120.agbale.api.LimitExceededException;
import io.github.juste120.agbale.api.RenderException;
import io.github.juste120.agbale.api.TemplateSyntaxException;
import io.github.juste120.agbale.domain.ResourceLimits;
import io.github.juste120.agbale.domain.TemplateName;
import io.github.juste120.agbale.port.TemplateCache;
import io.github.juste120.agbale.port.TemplateEngine;
import io.github.juste120.agbale.port.TemplateResolver;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.ParserException;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

/**
 * {@link TemplateEngine} based on Pebble, configured to be strict and safe:
 *
 * <ul>
 *   <li>HTML auto-escaping of every expression ({@code raw} filter to opt out explicitly);
 *   <li>strict variables: an undefined variable is an error, not an empty string;
 *   <li>no method call from templates, no override of core operators;
 *   <li>templates only loaded through the validating {@link ResolverLoader};
 *   <li>a maximum size of the produced HTML.
 * </ul>
 */
public final class PebbleTemplateEngine implements TemplateEngine {

  private static final String SIZE_ERROR_PREFIX = "Tried to write more than";

  private final PebbleEngine pebble;
  private final ResourceLimits limits;

  /**
   * Creates the engine.
   *
   * @param resolver where templates are read from
   * @param cache the compiled template cache, or {@code null} to disable caching
   * @param limits the resource limits
   * @param defaultLocale the default locale
   */
  public PebbleTemplateEngine(
      TemplateResolver resolver, TemplateCache cache, ResourceLimits limits, Locale defaultLocale) {
    this.limits = limits;
    PebbleEngine.Builder builder =
        new PebbleEngine.Builder()
            .loader(new ResolverLoader(resolver, limits.maxTemplateBytes()))
            .strictVariables(true)
            .autoEscaping(true)
            .defaultEscapingStrategy("html")
            .allowOverrideCoreOperators(false)
            .methodAccessValidator(new DenyAllMethodAccessValidator())
            .maxRenderedSize(limits.maxRenderedChars())
            .defaultLocale(defaultLocale)
            .extension(new AgbaleExtension());
    if (cache == null) {
      builder.cacheActive(false);
    } else {
      builder.templateCache(new PebbleCacheAdapter(cache));
    }
    this.pebble = builder.build();
  }

  @Override
  public String render(TemplateName name, Map<String, Object> model, Locale locale) {
    try {
      PebbleTemplate template = pebble.getTemplate(name.value());
      StringWriter html = new StringWriter();
      template.evaluate(html, model, locale);
      return html.toString();
    } catch (AgbaleException e) {
      throw e;
    } catch (ParserException e) {
      throw new TemplateSyntaxException(
          fileName(e, name), e.getLineNumber(), e.getPebbleMessage(), e);
    } catch (PebbleException e) {
      throw translate(e, name);
    } catch (IOException e) {
      throw new RenderException("Cannot write the HTML of template '" + name + "'.", e);
    } catch (StackOverflowError e) {
      // A template that extends or includes itself (directly or through others) recurses forever.
      throw new RenderException(
          "Template '"
              + name
              + "' recurses without end: a template probably extends or includes itself, directly"
              + " or through another template. Check its 'extends', 'include' and 'import' tags.",
          null);
    } catch (RuntimeException e) {
      AgbaleException agbale = findAgbaleCause(e);
      if (agbale != null) {
        throw agbale;
      }
      throw RenderException.evaluation(name.value(), null, e.getClass().getSimpleName(), e);
    }
  }

  private RuntimeException translate(PebbleException e, TemplateName name) {
    AgbaleException agbale = findAgbaleCause(e);
    if (agbale != null) {
      return agbale;
    }
    String message = e.getPebbleMessage() == null ? "" : e.getPebbleMessage();
    if (message.startsWith(SIZE_ERROR_PREFIX)) {
      return new LimitExceededException(
          name.value(),
          LimitExceededException.Limit.RENDERED_SIZE,
          limits.maxRenderedChars() + " characters");
    }
    if (e.getCause() instanceof ParserException parser) {
      return new TemplateSyntaxException(
          fileName(parser, name), parser.getLineNumber(), parser.getPebbleMessage(), e);
    }
    return RenderException.evaluation(fileName(e, name), e.getLineNumber(), message, e);
  }

  private static AgbaleException findAgbaleCause(Throwable error) {
    Throwable current = error;
    for (int depth = 0; current != null && depth < 10; depth++) {
      if (current instanceof AgbaleException agbale) {
        return agbale;
      }
      current = current.getCause();
    }
    return null;
  }

  private static String fileName(PebbleException e, TemplateName fallback) {
    String file = e.getFileName();
    return file == null || file.isBlank() ? fallback.value() : file;
  }

  @Override
  public void clearCache() {
    pebble.getTemplateCache().invalidateAll();
  }
}
