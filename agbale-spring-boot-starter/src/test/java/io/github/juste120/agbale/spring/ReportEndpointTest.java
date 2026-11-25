package io.github.juste120.agbale.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.juste120.agbale.spring.testapp.TestApplication;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** User story 4: {@code @Report} endpoints and RFC 9457 errors (FR-029 to FR-032). */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class ReportEndpointTest {

  @Autowired private MockMvc mvc;

  private static String text(byte[] pdf) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(document).replace(' ', ' ');
    }
  }

  @Test
  void rendersInlineByDefault() throws Exception {
    MvcResult result =
        mvc.perform(get("/invoices/F-1/inline"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline; filename=\"invoice.pdf\";")))
            .andReturn();

    byte[] pdf = result.getResponse().getContentAsByteArray();
    assertThat(result.getResponse().getContentLength()).isEqualTo(pdf.length);
    assertThat(text(pdf))
        .contains("Facture F-1")
        .contains("Kossi Mensah")
        .contains("1 250 000 FCFA");
  }

  @Test
  void attachmentWithPlaceholdersInTheFileName() throws Exception {
    mvc.perform(get("/invoices/F-2026-001/download"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.startsWith(
                        "attachment; filename=\"facture-F-2026-001-C-01.pdf\";")));
  }

  @Test
  void addsTheExtensionAndEncodesNonAsciiNames() throws Exception {
    mvc.perform(get("/invoices/custom").param("number", "R-9"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.startsWith("inline; filename=\"R-9.pdf\";")));
    String disposition =
        mvc.perform(get("/invoices/accents"))
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.CONTENT_DISPOSITION);
    assertThat(disposition).contains("filename*=UTF-8''re%C3%A7u-%C3%A9l%C3%A8ve.pdf");
  }

  @Test
  void placeholderValuesCannotInjectHeadersOrPaths() throws Exception {
    MvcResult result =
        mvc.perform(get("/invoices/custom").param("number", "../../evil\"; x=\r\nSet-Cookie: a=b"))
            .andExpect(status().isOk())
            .andReturn();

    String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
    assertThat(disposition).doesNotContain("\r").doesNotContain("\n").doesNotContain("../");
    assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    assertThat(disposition).startsWith("inline; filename=\"_.._evil__ x=__Set-Cookie: a=b.pdf\";");
  }

  @Test
  void supportsMapsAndOptionals() throws Exception {
    mvc.perform(get("/invoices/map"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.startsWith("inline; filename=\"M-1.pdf\";")));
    mvc.perform(get("/invoices/optional")).andExpect(status().isOk());
  }

  @Test
  void nonReportEndpointsAreUntouched() throws Exception {
    mvc.perform(get("/json"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.number").value("J-1"));
  }

  @Test
  void unknownTemplateGivesProblemDetails() throws Exception {
    mvc.perform(get("/errors/unknown-template"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AGBALE_TEMPLATE_NOT_FOUND"))
        .andExpect(jsonPath("$.title").value("Template not found"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.instance").value("/errors/unknown-template"))
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.containsString("Did you mean 'invoice'?")))
        .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
  }

  @Test
  void errorsAreProblemDetailsEvenWhenTheClientAsksForPdf() throws Exception {
    mvc.perform(get("/errors/unknown-template").accept(MediaType.APPLICATION_PDF))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("AGBALE_TEMPLATE_NOT_FOUND"));
  }

  @Test
  void everyErrorHasAStableCodeAndNoInternalDetails() throws Exception {
    String[][] cases = {
      {"/errors/null", "AGBALE_RENDER_FAILED"},
      {"/errors/syntax", "AGBALE_TEMPLATE_SYNTAX"},
      {"/errors/remote", "AGBALE_RESOURCE_REFUSED"},
      {"/errors/bad-placeholder", "AGBALE_INVALID_REPORT_METHOD"},
      {"/errors/bytes", "AGBALE_INVALID_REPORT_METHOD"},
      {"/errors/no-template", "AGBALE_INVALID_REPORT_METHOD"}
    };
    for (String[] c : cases) {
      String body =
          mvc.perform(get(c[0]))
              .andExpect(status().isInternalServerError())
              .andExpect(jsonPath("$.code").value(c[1]))
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertThat(body)
          .doesNotContain("Exception")
          .doesNotContain("at io.")
          .doesNotContain("/home/")
          .doesNotContain("trace");
    }
  }

  @Test
  void clientRefusingPdfGets406() throws Exception {
    mvc.perform(get("/invoices/F-1/inline").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code").value("AGBALE_NOT_ACCEPTABLE"));
  }

  @Test
  void angularDefaultAcceptHeaderIsAccepted() throws Exception {
    mvc.perform(
            get("/invoices/F-1/inline")
                .header(HttpHeaders.ACCEPT, "application/json, text/plain, */*"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }
}
