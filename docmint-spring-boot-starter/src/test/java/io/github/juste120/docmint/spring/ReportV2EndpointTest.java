package io.github.juste120.docmint.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.juste120.docmint.spring.testapp.TestApplication;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** V2 in Spring: ReportContent, PNG format, default watermark property. */
@SpringBootTest(classes = TestApplication.class, properties = "docmint.watermark.text=RECETTE")
@AutoConfigureMockMvc
class ReportV2EndpointTest {

  @Autowired private MockMvc mvc;

  private byte[] get200(String url) throws Exception {
    return mvc.perform(get(url))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsByteArray();
  }

  private static String text(byte[] pdf) throws Exception {
    try (var document = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(document).replaceAll("\\s", "");
    }
  }

  @Test
  void watermarkFromTheMethodAndDefaultWatermarkFromProperties() throws Exception {
    assertThat(text(get200("/v2/watermark"))).contains("PAYÉE").doesNotContain("RECETTE");
    assertThat(text(get200("/invoices/F-1/inline"))).contains("RECETTE");
  }

  @Test
  void protectedPdfWithExplicitFileName() throws Exception {
    mvc.perform(get("/v2/protected"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    startsWith("inline; filename=\"confidentiel.pdf\"")));
    byte[] pdf = get200("/v2/protected");
    assertThatThrownBy(() -> Loader.loadPDF(pdf)).isInstanceOf(InvalidPasswordException.class);
    assertThat(Loader.loadPDF(pdf, "1234").isEncrypted()).isTrue();
  }

  @Test
  void pngPreviewEndpoint() throws Exception {
    byte[] png =
        mvc.perform(get("/v2/preview"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("inline; filename=\"apercu-I-1.png\"")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(ImageIO.read(new ByteArrayInputStream(png)).getWidth()).isEqualTo(793);

    mvc.perform(get("/v2/preview").accept(MediaType.APPLICATION_PDF))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("image/png")));
    mvc.perform(get("/v2/preview-page-2"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("DOCMINT_PREVIEW_PAGE_OUT_OF_RANGE"))
        .andExpect(jsonPath("$.title").value("Preview page out of range"));
  }
}
