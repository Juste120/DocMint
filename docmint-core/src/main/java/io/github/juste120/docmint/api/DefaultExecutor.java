package io.github.juste120.docmint.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared default executor of {@link DocMint#renderAsync}: daemon threads, one per processor. */
final class DefaultExecutor {

  private static final AtomicInteger COUNTER = new AtomicInteger();
  private static final ThreadFactory THREADS =
      runnable -> {
        Thread thread = new Thread(runnable, "docmint-async-" + COUNTER.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      };

  static final ExecutorService INSTANCE =
      Executors.newFixedThreadPool(
          Math.max(2, Runtime.getRuntime().availableProcessors()), THREADS);

  private DefaultExecutor() {}
}
