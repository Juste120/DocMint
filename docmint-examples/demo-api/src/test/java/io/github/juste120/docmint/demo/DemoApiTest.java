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

  private static String text(byte[] pdf, String password) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf, password)) {
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
        .contains("Quincaillerie du Golfe SARL")
        .contains("quatre cent vingt-six mille huit cent soixante-cinq francs CFA")
        .doesNotContain("NON PAYÉE");
  }

  @Test
  void unpaidInvoiceIsWatermarkedAndPreviewIsAnImage() throws Exception {
    byte[] unpaid =
        mvc.perform(get("/api/invoices/2/pdf")).andReturn().getResponse().getContentAsByteArray();
    assertThat(text(unpaid).replace(" ", "")).contains("NONPAYÉE");

    mvc.perform(get("/api/invoices/1/preview"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    startsWith("inline; filename=\"apercu-F-2026-001.png\"")));
  }

  @Test
  void longInvoiceSpansSeveralPagesWithHeaders() throws Exception {
    byte[] pdf =
        keep(
            "facture-longue.pdf",
            mvc.perform(get("/api/invoices/2/pdf"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray());

    assertThat(pages(pdf)).isGreaterThan(1);
    int pages = pages(pdf);
    assertThat(text(pdf)).contains("Akossiwa Ɖogbe");
    // Header and footer of the layout are repeated on every page (compared without whitespace:
    // the diagonal watermark splits the extracted words).
    try (PDDocument document = Loader.loadPDF(pdf)) {
      for (int page = 1; page <= pages; page++) {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        String text = stripper.getText(document).replaceAll("[\\s\\u00A0]", "");
        assertThat(text)
            .as("page %d", page)
            .contains("NIF1000000000")
            .contains("Capital10000000FCFA");
      }
    }
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
    assertThat(text(attestation, "MAT-0042"))
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

  @Test
  void salesStatementWithChartsAndExports() throws Exception {
    byte[] pdf =
        keep(
            "releve-ventes.pdf",
            mvc.perform(get("/api/reports/sales/pdf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
    assertThat(pages(pdf)).isEqualTo(2);
    assertThat(text(pdf)).contains("Relevé des ventes").contains("1 250 000 FCFA");

    byte[] xlsx =
        mvc.perform(get("/api/reports/sales/xlsx"))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("inline; filename=\"releve-ventes-2026.xlsx\"")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (var workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
      assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("Ventes 2026");
      assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getNumericCellValue())
          .isEqualTo(1_250_000d);
      assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("Paiements");
    }

    mvc.perform(get("/api/invoices/export.xlsx"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    startsWith("attachment; filename=\"factures.xlsx\"")));
    String csv =
        new String(
            mvc.perform(get("/api/invoices/export.csv"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray(),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(csv)
        .startsWith("\uFEFFN° facture;Client;Date\r\nF-2026-001;Kossi Mensah;2026-11-24");
  }

  @Test
  void wordAttestationAndStreamedCsv() throws Exception {
    byte[] docx =
        mvc.perform(get("/api/employees/1/attestation.docx"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("inline; filename=\"attestation-MAT-0042.docx\"")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (var word =
        new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
      String text =
          word.getParagraphs().stream()
              .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
              .reduce("", (a, b) -> a + "\n" + b);
      assertThat(text).contains("ATTESTATION DE TRAVAIL").contains("Yawo Agbeko");
      assertThat(word.getHeaderList()).isNotEmpty();
    }
    String csv =
        new String(
            mvc.perform(get("/api/invoices/stream.csv"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray(),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(csv).contains("F-2026-002;Akossiwa Ɖogbe");
  }
}
