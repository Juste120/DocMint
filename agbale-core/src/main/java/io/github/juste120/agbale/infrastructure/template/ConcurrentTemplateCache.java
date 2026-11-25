package io.github.juste120.agbale.infrastructure.template;

import io.github.juste120.agbale.port.TemplateCache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/** {@link TemplateCache} backed by a {@link ConcurrentHashMap}: each key is computed once. */
public final class ConcurrentTemplateCache implements TemplateCache {

  private final ConcurrentMap<Object, Object> entries = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getOrCompute(Object key, Supplier<V> compiler) {
    return (V) entries.computeIfAbsent(key, k -> compiler.get());
  }

  @Override
  public void clear() {
    entries.clear();
  }

  /**
   * Returns the number of cached entries.
   *
   * @return the size
   */
  public int size() {
    return entries.size();
  }
}
