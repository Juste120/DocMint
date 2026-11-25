package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.RenderException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Runs a task with a maximum duration on a daemon thread.
 *
 * <p>On timeout the task is cancelled (interrupted). The JVM cannot force a busy thread to stop, so
 * the other limits (pages, rendered size) bound the work that may still run after the caller has
 * received its error.
 */
final class TimeLimiter {

  private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
  private static final ThreadFactory THREADS =
      runnable -> {
        Thread thread = new Thread(runnable, "docmint-render-" + THREAD_COUNTER.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      };
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(THREADS);

  private final ClassLoader classLoader;

  TimeLimiter(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  <T> T call(Callable<T> task, Duration timeout, Supplier<RuntimeException> onTimeout) {
    Future<T> future =
        EXECUTOR.submit(
            () -> {
              Thread current = Thread.currentThread();
              ClassLoader previous = current.getContextClassLoader();
              current.setContextClassLoader(classLoader);
              try {
                return task.call();
              } finally {
                current.setContextClassLoader(previous);
              }
            });
    try {
      return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw onTimeout.get();
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw new RenderException("The render was interrupted.", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RenderException("The render failed: " + cause.getClass().getSimpleName(), cause);
    }
  }
}
