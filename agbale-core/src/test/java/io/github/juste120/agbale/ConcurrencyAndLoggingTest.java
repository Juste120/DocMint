package io.github.juste120.agbale;

import static io.github.juste120.agbale.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.support.Fixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** SC-004, SC-005, FR-008, FR-009, FR-047. */
class ConcurrencyAndLoggingTest {

  private final Agbale agbale = Agbale.createDefault();

  @Test
  void parallelRendersGiveTheSameResultAsSequentialOnes() throws Exception {
    String invoice = text(agbale.pdf("invoice", Fixtures.invoice()));
    String simple = text(agbale.pdf("simple", Map.of("name", "Ama")));

    ExecutorService pool = Executors.newFixedThreadPool(16);
    try {
      List<Future<String[]>> results = new ArrayList<>();
      for (int i = 0; i < 50; i++) {
        boolean even = i % 2 == 0;
        results.add(
            pool.submit(
                () ->
                    even
                        ? new String[] {"invoice", text(agbale.pdf("invoice", Fixtures.invoice()))}
                        : new String[] {
                          "simple", text(agbale.pdf("simple", Map.of("name", "Ama")))
                        }));
      }
      for (Future<String[]> result : results) {
        String[] pair = result.get();
        assertThat(pair[1]).isEqualTo(pair[0].equals("invoice") ? invoice : simple);
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void warmRenderOfATypicalInvoiceIsFast() {
    agbale.pdf("invoice", Fixtures.invoice());
    long start = System.nanoTime();
    agbale.pdf("invoice", Fixtures.invoice());
    long millis = (System.nanoTime() - start) / 1_000_000;

    assertThat(millis).isLessThan(1000);
  }

  @Test
  void cacheCanBeDisabledAndCleared() {
    Agbale uncached = Agbale.builder().cache(false).build();

    assertThat(text(uncached.pdf("simple", Map.of("name", "Ama")))).contains("Ama");
    agbale.clearCache();
    assertThat(text(agbale.pdf("simple", Map.of("name", "Kofi")))).contains("Kofi");
  }

  @Test
  void renderIsLoggedAtDebugWithoutDataValues() throws Exception {
    String secret = "Secret-Client-4242";
    agbale.pdf("simple", Map.of("name", secret));
    Agbale.builder().allowRemoteHost("*.example.com").build();

    Path log = Path.of("target", "test-logs.txt");
    String logs = Files.readString(log, StandardCharsets.UTF_8);
    assertThat(logs).contains("Rendered template 'simple' as PDF");
    assertThat(logs).contains("wildcard hosts");
    assertThat(logs).doesNotContain(secret);
  }
}
