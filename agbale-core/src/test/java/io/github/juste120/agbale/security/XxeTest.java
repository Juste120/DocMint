package io.github.juste120.agbale.security;

import static io.github.juste120.agbale.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.agbale.api.Agbale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-013: external XML entities and DTDs are never resolved. */
class XxeTest {

  @Test
  void externalEntityIsNotResolved() throws Exception {
    byte[] pdf = Agbale.createDefault().pdf("security/xxe", Map.of());

    String text = text(pdf);
    assertThat(text).contains("Entity:");
    Path hostname = Path.of("/etc/hostname");
    if (Files.isReadable(hostname)) {
      String secret = Files.readString(hostname).strip();
      if (!secret.isEmpty()) {
        assertThat(text).doesNotContain(secret);
      }
    }
  }
}
