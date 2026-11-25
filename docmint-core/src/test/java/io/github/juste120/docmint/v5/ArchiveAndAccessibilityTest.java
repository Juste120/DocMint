package io.github.juste120.docmint.v5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.PdfStandard;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.SignatureSettings;
import io.github.juste120.docmint.domain.Watermark;
import io.github.juste120.docmint.support.Fixtures;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** V5 user stories 2 and 3: PDF/A and accessibility (FR-402 to FR-405). */
class ArchiveAndAccessibilityTest {

  private final DocMint docMint = DocMint.createDefault();

  private byte[] render(String template, Object data, RenderOptions options) {
    return docMint.render(RenderRequest.of(template, data).withOptions(options)).content();
  }

  private static String xmp(PDDocument document) throws Exception {
    return new String(
        document.getDocumentCatalog().getMetadata().exportXMPMetadata().readAllBytes(),
        StandardCharsets.UTF_8);
  }

  @ParameterizedTest
  @EnumSource(PdfStandard.class)
  void pdfAMetadataAndOutputIntent(PdfStandard standard) throws Exception {
    byte[] pdf =
        render("invoice", Fixtures.invoice(), RenderOptions.defaults().withPdfStandard(standard));

    try (PDDocument document = Loader.loadPDF(pdf)) {
      String xmp = xmp(document);
      assertThat(xmp)
          .contains("<pdfaid:part>" + standard.part() + "</pdfaid:part>")
          .contains("<pdfaid:conformance>" + standard.conformance() + "</pdfaid:conformance>")
          .as("title from the template")
          .contains("Facture F-2026-001");
      assertThat(document.getDocumentCatalog().getOutputIntents()).isNotEmpty();
      assertThat(document.isEncrypted()).isFalse();
    }
  }

  @Test
  void pdfAWithWatermarkAndDefaultsFromTheBuilder() throws Exception {
    DocMint archive = DocMint.builder().pdfStandard(PdfStandard.PDF_A_3B).build();

    byte[] pdf =
        archive
            .render(
                RenderRequest.of("simple", Map.of("name", "Ama"))
                    .withOptions(RenderOptions.defaults().withWatermark(Watermark.text("COPIE"))))
            .content();

    try (PDDocument document = Loader.loadPDF(pdf)) {
      assertThat(xmp(document))
          .contains("<pdfaid:part>3</pdfaid:part>")
          .contains(">Simple<"); // title of the template
    }
    // Defaults of PDF documents do not break other formats.
    assertThat(archive.png("simple", Map.of("name", "Ama"))).isNotEmpty();
  }

  @Test
  void accessiblePdfIsTaggedWithLanguage() throws Exception {
    byte[] pdf = render("images", Map.of(), RenderOptions.defaults().withAccessible(true));

    try (PDDocument document = Loader.loadPDF(pdf)) {
      assertThat(document.getDocumentCatalog().getStructureTreeRoot()).isNotNull();
      assertThat(document.getDocumentCatalog().getMarkInfo().isMarked()).isTrue();
      assertThat(document.getDocumentCatalog().getLanguage()).isEqualTo("fr");
    }
    byte[] plain = render("images", Map.of(), RenderOptions.defaults());
    try (PDDocument document = Loader.loadPDF(plain)) {
      assertThat(document.getDocumentCatalog().getStructureTreeRoot()).isNull();
    }
  }

  @Test
  void incompatibleCombinationsAreRefused() {
    Protection protection = Protection.openPassword("1234".toCharArray());
    assertThatThrownBy(
            () ->
                render(
                    "simple-v2",
                    Map.of(),
                    RenderOptions.defaults()
                        .withPdfStandard(PdfStandard.PDF_A_3B)
                        .withProtection(protection)))
        .isInstanceOfSatisfying(
            InvalidOptionsException.class,
            e -> assertThat(e.code()).isEqualTo("DOCMINT_PROTECTION_INVALID"))
        .hasMessageContaining("PDF/A forbids encryption");

    SignatureSettings signature =
        SignatureSettings.pkcs12(new byte[] {1}, "secret-pwd".toCharArray());
    assertThatThrownBy(
            () -> render("simple-v2", Map.of(), RenderOptions.defaults().withSignature(signature)))
        .isInstanceOfSatisfying(
            InvalidOptionsException.class,
            e -> assertThat(e.code()).isEqualTo("DOCMINT_SIGNATURE_INVALID"))
        .hasMessageContaining("docmint-signature");
    assertThatThrownBy(
            () ->
                render(
                    "simple-v2",
                    Map.of(),
                    RenderOptions.defaults().withSignature(signature).withProtection(protection)))
        .hasMessageContaining("cannot also be password-protected");
    assertThatThrownBy(
            () ->
                docMint.render(
                    new RenderRequest(
                        "simple-v2",
                        Map.of(),
                        OutputFormat.PNG,
                        RenderOptions.defaults().withSignature(signature))))
        .hasMessageContaining("Only PDF documents can be signed");
    assertThat(signature.toString()).doesNotContain("secret-pwd");
    signature.clear();
    assertThat(signature.passwordCopy()).containsOnly('\0');
    assertThatThrownBy(() -> SignatureSettings.pkcs12(new byte[0], new char[] {'x'}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(PdfStandard.PDF_A_2U.part()).isEqualTo(2);
  }
}
