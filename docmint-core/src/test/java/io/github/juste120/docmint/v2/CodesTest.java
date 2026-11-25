package io.github.juste120.docmint.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.CodeGenerationException;
import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.support.PdfText;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** V2 user story 2: QR codes and barcodes (FR-105 to FR-109). */
class CodesTest {

  private final DocMint docMint = DocMint.createDefault();

  @Test
  void codesDecodeToTheirContent() {
    byte[] pdf =
        docMint.pdf(
            "v2/codes",
            Map.of(
                "url", "https://verify.example.com/F-2026-001?k=Ŋɔ",
                "number", "F-2026-001",
                "ean", "5901234123457"));

    List<String> decoded =
        V2Support.imagesOfFirstPage(pdf).stream().map(V2Support::decode).toList();
    assertThat(decoded)
        .contains("https://verify.example.com/F-2026-001?k=Ŋɔ", "F-2026-001", "5901234123457");
    assertThat(PdfText.text(pdf)).contains("5901234123457"); // human-readable text
  }

  @Test
  void qrCodeIsPrintedAtTheRequestedSize() {
    byte[] pdf = docMint.pdf("v2/qr-only", Map.of("content", "DocMint"));

    BufferedImage image = V2Support.imagesOfFirstPage(pdf).get(0);
    assertThat(image.getWidth()).isEqualTo(image.getHeight()).isGreaterThanOrEqualTo(200);
    assertThat(V2Support.decode(image)).isEqualTo("DocMint");
  }

  @ParameterizedTest
  @CsvSource({
    "5901234123458, ean13, check digit",
    "12345, ean13, ean13",
    "abc, qr128, unknown barcode format",
    "'é€', code128, code128"
  })
  void invalidBarcodeContentIsAClearError(String content, String format, String message) {
    assertThatThrownBy(
            () -> docMint.pdf("v2/barcode-only", Map.of("content", content, "format", format)))
        .isInstanceOf(CodeGenerationException.class)
        .hasMessageContaining("v2/barcode-only")
        .hasMessageContaining(message);
  }

  @Test
  void invalidQrArgumentsAndContentAreClearErrors() {
    assertThatThrownBy(() -> docMint.pdf("v2/qr-only", Map.of("content", "x".repeat(2001))))
        .isInstanceOf(CodeGenerationException.class)
        .hasMessageContaining("longer than 2000");
    assertThatThrownBy(() -> docMint.pdf("v2/qr-only", Map.of("content", "")))
        .hasMessageContaining("empty");
    assertThatThrownBy(
            () -> docMint.pdf("v2/qr-args", Map.of("size", "30px;color:red", "level", "M")))
        .isInstanceOf(CodeGenerationException.class)
        .hasMessageContaining("invalid size");
    assertThatThrownBy(() -> docMint.pdf("v2/qr-args", Map.of("size", "30mm", "level", "Z")))
        .hasMessageContaining("invalid QR level");
    assertThatThrownBy(() -> docMint.pdf("v2/qr-only", Map.of("content", "é".repeat(1500))))
        .isInstanceOf(CodeGenerationException.class);
  }

  @Test
  void contentNeverInjectsMarkup() {
    String payload = "\"><script>alert(1)</script><img src=\"file:///etc/passwd";

    byte[] pdf = docMint.pdf("v2/barcode-only", Map.of("content", payload, "format", "code128"));

    assertThat(PdfText.text(pdf).replaceAll("\\s", "")).contains("<script>alert(1)</script>");
    assertThat(V2Support.decode(V2Support.imagesOfFirstPage(pdf).get(0))).isEqualTo(payload);
  }
}
