package io.github.juste120.agbale.infrastructure.pebble;

import io.github.juste120.agbale.port.TemplateCache;
import io.pebbletemplates.pebble.cache.PebbleCache;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.util.function.Function;

/** Bridges Pebble's template cache to the {@link TemplateCache} port. */
final class PebbleCacheAdapter implements PebbleCache<Object, PebbleTemplate> {

  private final TemplateCache cache;

  PebbleCacheAdapter(TemplateCache cache) {
    this.cache = cache;
  }

  @Override
  public PebbleTemplate computeIfAbsent(
      Object key, Function<? super Object, ? extends PebbleTemplate> mappingFunction) {
    return cache.getOrCompute(key, () -> mappingFunction.apply(key));
  }

  @Override
  public void invalidateAll() {
    cache.clear();
  }
}
