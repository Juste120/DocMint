package io.github.juste120.docmint.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.api.TemplateNotFoundException;
import io.github.juste120.docmint.domain.CsvSettings;
import io.github.juste120.docmint.domain.OutputFormat;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** V4 user stories 3 and 4: streaming and asynchronous generation (FR-304, FR-305). */
class StreamingAndAsyncTest {

  private final DocMint docMint = DocMint.createDefault();

  record Row(String name, BigDecimal amount) {}

  @Test
  void renderIntoAStream() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    RenderResponse metadata =
        docMint.render(RenderRequest.of("simple", Map.of("name", "Ama")), out);

    assertThat(out.toByteArray()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
    assertThat(metadata.fileName()).isEqualTo("simple.pdf");
  }

  @Test
  void csvStreamingWritesRowsLazily() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    long count =
        docMint.exportCsv(
            Stream.of(new Row("=1+1", BigDecimal.ONE), new Row("Ama", BigDecimal.TEN)),
            out,
            CsvSettings.rfc4180());

    assertThat(count).isEqualTo(2);
    assertThat(out.toString(StandardCharsets.UTF_8))
        .isEqualTo("name,amount\r\n'=1+1,1\r\n'Ama,10\r\n".replace("'Ama", "Ama"));
    ByteArrayOutputStream empty = new ByteArrayOutputStream();
    assertThat(docMint.exportCsv(Stream.empty(), empty, null)).isZero();
  }

  @Test
  void twoHundredThousandRowsAreReadWhileWriting() {
    // Streaming means rows are produced while the output is written, never all loaded first.
    AtomicLong produced = new AtomicLong();
    AtomicLong producedAtFirstWrite = new AtomicLong(-1);
    long[] written = {0};
    OutputStream counting =
        new OutputStream() {
          @Override
          public void write(int b) {
            write(new byte[] {(byte) b}, 0, 1);
          }

          @Override
          public void write(byte[] b, int off, int len) {
            producedAtFirstWrite.compareAndSet(-1, produced.get());
            written[0] += len;
          }
        };

    long count =
        docMint.exportCsv(
            IntStream.range(0, 200_000)
                .mapToObj(i -> new Row("client-" + i, BigDecimal.valueOf(i)))
                .peek(row -> produced.incrementAndGet()),
            counting,
            null);

    assertThat(count).isEqualTo(200_000);
    assertThat(written[0]).isGreaterThan(3_000_000);
    assertThat(producedAtFirstWrite.get()).as("rows read before the first write").isLessThan(1_000);
  }

  @Test
  void asyncRenderCompletesOnTheExecutor() throws Exception {
    var executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "my-docs"));
    try {
      DocMint custom = DocMint.builder().executor(executor).build();
      AtomicReference<String> thread = new AtomicReference<>();

      RenderResponse response =
          custom
              .renderAsync(RenderRequest.of("simple", Map.of("name", "Ama")))
              .whenComplete((r, e) -> thread.set(Thread.currentThread().getName()))
              .get(30, TimeUnit.SECONDS);

      assertThat(response.content()).isNotEmpty();
      assertThat(thread.get()).isEqualTo("my-docs");
    } finally {
      executor.shutdownNow();
    }
    assertThatThrownBy(() -> docMint.renderAsync(RenderRequest.of("nope", Map.of())).join())
        .isInstanceOf(CompletionException.class)
        .hasCauseInstanceOf(TemplateNotFoundException.class);
  }

  @Test
  void htmlOutputInlinesTemplateImagesOnly() {
    String html =
        new String(
            docMint
                .render(
                    new RenderRequest(
                        "v4/inline-image", Map.of(), OutputFormat.HTML, RenderOptions.defaults()))
                .content(),
            StandardCharsets.UTF_8);

    assertThat(html.split("data:image/png;base64,", -1)).hasSize(3); // ../images and /images
    assertThat(html).contains("https://evil.example.com/x.png").contains("/../secret.png");
  }
}
