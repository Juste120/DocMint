package io.github.juste120.docmint.infrastructure.pebble;

import io.github.juste120.docmint.api.InvalidTemplateNameException;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.TemplateNotFoundException;
import io.github.juste120.docmint.domain.TemplateName;
import io.github.juste120.docmint.port.TemplateResolver;
import io.github.juste120.docmint.port.TemplateSource;
import io.pebbletemplates.pebble.loader.Loader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Pebble {@link Loader} backed by the {@link TemplateResolver} port.
 *
 * <p>Every name used by a template ({@code extends}, {@code include}, {@code import}) goes through
 * {@link TemplateName} validation, so no template can reach a file outside the templates root.
 * Names are always relative to the root; {@code ./} and {@code ../} are refused.
 */
final class ResolverLoader implements Loader<String> {

  private final TemplateResolver resolver;
  private final long maxTemplateBytes;

  ResolverLoader(TemplateResolver resolver, long maxTemplateBytes) {
    this.resolver = resolver;
    this.maxTemplateBytes = maxTemplateBytes;
  }

  @Override
  public Reader getReader(String cacheKey) {
    TemplateName name = TemplateName.of(cacheKey);
    TemplateSource source =
        resolver
            .resolve(name)
            .orElseThrow(
                () ->
                    new TemplateNotFoundException(
                        name.value(), resolver.location(), List.of(), null));
    // Also enforced here for custom resolvers that do not check the size themselves.
    if (source.content().getBytes(StandardCharsets.UTF_8).length > maxTemplateBytes) {
      throw new LimitExceededException(
          name.value(), LimitExceededException.Limit.TEMPLATE_SIZE, maxTemplateBytes + " bytes");
    }
    return new StringReader(source.content());
  }

  @Override
  public void setCharset(String charset) {
    // Templates are always UTF-8.
  }

  @Override
  public void setPrefix(String prefix) {
    // The root is owned by the resolver.
  }

  @Override
  public void setSuffix(String suffix) {
    // The .html suffix is owned by TemplateName.
  }

  @Override
  public String resolveRelativePath(String relativePath, String anchorPath) {
    return null; // no relative resolution: every name is relative to the templates root
  }

  @Override
  public String createCacheKey(String templateName) {
    try {
      return TemplateName.of(templateName).value();
    } catch (IllegalArgumentException e) {
      throw new InvalidTemplateNameException(templateName, e.getMessage());
    }
  }

  @Override
  public boolean resourceExists(String templateName) {
    return TemplateName.isValid(templateName)
        && resolver.resolve(TemplateName.of(templateName)).isPresent();
  }
}
