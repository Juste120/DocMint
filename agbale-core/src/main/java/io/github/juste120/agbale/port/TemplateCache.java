package io.github.juste120.agbale.port;

import java.util.function.Supplier;

/** Thread-safe cache of compiled templates. */
public interface TemplateCache {

  /**
   * Returns the cached value for a key, computing and caching it if absent.
   *
   * @param key the cache key
   * @param compiler computes the value when absent
   * @param <V> the value type
   * @return the cached or computed value
   */
  <V> V getOrCompute(Object key, Supplier<V> compiler);

  /** Removes every entry. */
  void clear();
}
