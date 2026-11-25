package io.github.juste120.docmint.v2;

import static io.github.juste120.docmint.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.Permission;
import io.github.juste120.docmint.domain.PreviewSettings;
import io.github.juste120.docmint.domain.Protection;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.Watermark;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.Test;

/** V2 user stories 4, 5 and 6: watermark, protection, PNG preview (FR-114 to FR-124). */
class WatermarkProtectionPreviewTest {

  private final DocMint docMint = DocMint.createDefault();

  private byte[] render(String template, RenderOptions options) {
    return docMint.render(RenderRequest.of(template, Map.of()).withOptions(options)).content();
  }

  @Test
  void textWatermarkOnEveryPageKeepsContentReadable() {
    byte[] pdf = render("pages", RenderOptions.defaults().withWatermark(Watermark.text("PAYÉE")));

    String text = text(pdf);
    assertThat(text.replaceAll("\\s", "").split("PAYÉE", -1)).hasSize(4); // once per page, 3 pages
    assertThat(text).contains("Premier").contains("Deuxième").contains("Troisième");
  }

  @Test
  void watermarkOptionsAndDefaultWatermark() {
    Watermark stamp =
        Watermark.text("ANNULÉ")
            .withAngle(-30)
            .withOpacity(0.5)
            .withColor("#C00000")
            .withFontSize(60)
            .withLayer(Watermark.Layer.OVER);
    assertThat(
            text(render("simple-v2", RenderOptions.defaults().withWatermark(stamp)))
                .replaceAll("\\s", ""))
        .contains("ANNULÉ");

    DocMint staging = DocMint.builder().defaultWatermark(Watermark.text("TEST")).build();
    assertThat(text(staging.pdf("simple", Map.of("name", "Ama"))).replaceAll("\\s", ""))
        .contains("TEST");
  }

  @Test
  void imageWatermarkAndErrors() {
    byte[] pdf =
        render(
            "simple-v2",
            RenderOptions.defaults().withWatermark(Watermark.image("images/logo.png")));
    assertThat(V2Support.imagesOfFirstPage(pdf)).isNotEmpty();

    assertThatThrownBy(
            () ->
                render(
                    "simple-v2",
                    RenderOptions.defaults().withWatermark(Watermark.image("images/none.png"))))
        .isInstanceOfSatisfying(
            InvalidOptionsException.class,
            e -> assertThat(e.code()).isEqualTo("DOCMINT_WATERMARK_INVALID"));
    assertThatThrownBy(() -> Watermark.text("X").withOpacity(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Watermark.text("X").withColor("red"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Watermark.image("../secret.png"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new Watermark("X", "a.png", 0, 0.5, "#000000", 0, Watermark.Layer.OVER))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> render("simple-v2", RenderOptions.defaults().withWatermark(Watermark.text("日本"))))
        .isInstanceOf(InvalidOptionsException.class);
  }

  @Test
  void openPasswordIsRequired() throws Exception {
    char[] password = "Togo-2026!".toCharArray();
    byte[] pdf =
        render(
            "simple-v2",
            RenderOptions.defaults().withProtection(Protection.openPassword(password)));

    assertThatThrownBy(() -> Loader.loadPDF(pdf)).isInstanceOf(InvalidPasswordException.class);
    try (PDDocument document = Loader.loadPDF(pdf, "Togo-2026!")) {
      assertThat(document.isEncrypted()).isTrue();
      assertThat(document.getEncryption().getLength()).isEqualTo(256);
    }
  }

  @Test
  void permissionsAreApplied() throws Exception {
    byte[] pdf =
        render(
            "simple-v2",
            RenderOptions.defaults().withProtection(Protection.permissions(Permission.PRINT)));

    try (PDDocument document = Loader.loadPDF(pdf)) {
      AccessPermission permission = document.getCurrentAccessPermission();
      assertThat(permission.canPrint()).isTrue();
      assertThat(permission.canExtractContent()).isFalse();
      assertThat(permission.canModify()).isFalse();
      assertThat(permission.isOwnerPermission()).isFalse();
    }
  }

  @Test
  void passwordsNeverLeak() throws Exception {
    String secret = "Secret-Pwd-9876";
    Protection protection = Protection.openPassword(secret.toCharArray());
    assertThat(protection.toString()).doesNotContain(secret).contains("***");
    render("simple-v2", RenderOptions.defaults().withProtection(protection));

    assertThatThrownBy(
            () ->
                docMint.render(
                    new RenderRequest(
                        "simple-v2",
                        Map.of(),
                        OutputFormat.PNG,
                        RenderOptions.defaults().withProtection(protection))))
        .isInstanceOfSatisfying(
            InvalidOptionsException.class,
            e -> assertThat(e.code()).isEqualTo("DOCMINT_PROTECTION_INVALID"))
        .hasMessageNotContaining(secret);
    String logs = Files.readString(Path.of("target", "test-logs.txt"), StandardCharsets.UTF_8);
    assertThat(logs).doesNotContain(secret);

    protection.clear();
    assertThat(protection.openPasswordCopy()).containsOnly('\0');
    assertThatThrownBy(() -> Protection.openPassword(new char[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void pngPreviewOfAPage() throws Exception {
    byte[] png = docMint.png("simple", Map.of("name", "Ama"));

    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
    assertThat(image.getWidth()).isEqualTo(793); // A4 at 96 DPI: 595.3 pt = 793.7 px
    assertThat(image.getHeight()).isEqualTo(1122); // 841.9 pt = 1122.5 px, truncated

    RenderResponse page2 =
        docMint.render(
            new RenderRequest(
                "pages",
                Map.of(),
                OutputFormat.PNG,
                RenderOptions.defaults().withPreview(new PreviewSettings(2, 50))));
    assertThat(page2.mediaType()).isEqualTo("image/png");
    assertThat(page2.fileName()).isEqualTo("pages.png");
    assertThat(ImageIO.read(new ByteArrayInputStream(page2.content())).getWidth()).isEqualTo(413);
  }

  @Test
  void previewErrorsAndLimits() {
    assertThatThrownBy(
            () ->
                docMint.render(
                    new RenderRequest(
                        "simple-v2",
                        Map.of(),
                        OutputFormat.PNG,
                        RenderOptions.defaults().withPreview(new PreviewSettings(5, 96)))))
        .isInstanceOfSatisfying(
            InvalidOptionsException.class,
            e -> assertThat(e.code()).isEqualTo("DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE"));
    assertThatThrownBy(() -> new PreviewSettings(1, 600))
        .isInstanceOf(IllegalArgumentException.class);

    DocMint small =
        DocMint.builder().limits(ResourceLimits.defaults().withMaxImagePixels(10_000)).build();
    assertThatThrownBy(() -> small.png("simple", Map.of("name", "Ama")))
        .isInstanceOfSatisfying(
            LimitExceededException.class,
            e -> assertThat(e.limit()).isEqualTo(LimitExceededException.Limit.IMAGE_SIZE));
  }
}
