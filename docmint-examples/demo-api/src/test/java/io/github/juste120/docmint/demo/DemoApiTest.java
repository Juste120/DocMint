package io.github.juste120.docmint.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

@SpringBootTest
@AutoConfigureMockMvc
class DemoApiTest {

  @Autowired private MockMvc mvc;

  private static String text(byte[] pdf) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      return stripper.getText(document).replace('\u00A0', ' ').replaceAll("\\s+", " ");
    }
  }

  private static int pages(byte[] pdf) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return document.getNumberOfPages();
    }
  }

  /** Writes the PDF to target/demo-pdfs so that it can be opened and checked visually. */
  private static byte[] keep(String name, byte[] pdf) throws IOException {
    Path dir = Path.of("target", "demo-pdfs");
    Files.createDirectories(dir);
    Files.write(dir.resolve(name), pdf);
    return pdf;
  }

  @Test
  void invoicePdf() throws Exception {
    byte[] pdf =
        mvc.perform(get("/api/invoices/1/pdf"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("inline; filename=\"facture-F-2026-001.pdf\"")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    keep("facture.pdf", pdf);

    // 40 x 5 200 + 25 x 4 750 + 35 000 = 361 750 ; VAT 18 % = 65 115 ; total 426 865
    assertThat(text(pdf))
        .contains("Facture n° F-2026-001")
        .contains("Kossi Mensah")
        .contains("Transport Lomé – Kpalimé")
        .contains("361 750 FCFA")
        .contains("TVA (18 %) 65 115 FCFA")
        .contains("426 865 FCFA")
        .contains("24 novembre 2026")
        .contains("Quincaillerie du Golfe SARL");
  }

  @Test
  void longInvoiceSpansSeveralPagesWithHeaders() throws Exception {
    byte[] pdf =
        mvc.perform(get("/api/invoices/2/pdf")).andReturn().getResponse().getContentAsByteArray();

    assertThat(pages(pdf)).isGreaterThan(1);
    assertThat(text(pdf)).contains("Akossiwa Ɖogbe").contains("Page 2 / ");
  }

  @Test
  void receiptAndAttestation() throws Exception {
    byte[] receipt =
        mvc.perform(get("/api/payments/1/receipt"))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("inline; filename=\"recu-R-2026-001.pdf\"")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    keep("recu.pdf", receipt);
    assertThat(text(receipt))
        .contains("150 000 FCFA")
        .contains("24/11/2026 à 10:30")
        .contains("Mobile Money");

    byte[] attestation =
        mvc.perform(get("/api/employees/1/attestation").param("purpose", "une demande de visa"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    keep("attestation.pdf", attestation);
    assertThat(text(attestation))
        .contains("ATTESTATION DE TRAVAIL")
        .contains("Yawo Agbeko")
        .contains("1 mars 2021")
        .contains("une demande de visa");
  }

  @Test
  void listsAndErrors() throws Exception {
    mvc.perform(get("/api/invoices")).andExpect(jsonPath("$[0].number").value("F-2026-001"));
    mvc.perform(get("/api/invoices/999/pdf")).andExpect(status().isNotFound());
    mvc.perform(get("/api/invoices/1/pdf").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition"));
  }
}
