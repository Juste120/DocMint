package io.github.juste120.docmint.word;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.OutputFormat;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

/** V4 user story 1: Word export (FR-301, FR-302, FR-306). */
class WordExportTest {

  private static final String PAYLOAD = "{ MACROBUTTON x } <script>alert(1)</script>";

  private RenderResponse render() {
    return DocMint.createDefault()
        .render(
            new RenderRequest(
                "contrat",
                Map.of("client", "Kossi Mensah", "signature", PAYLOAD),
                OutputFormat.DOCX,
                RenderOptions.defaults()));
  }

  @Test
  void templateBecomesAWordDocument() throws Exception {
    RenderResponse response = render();

    assertThat(response.mediaType()).isEqualTo(OutputFormat.DOCX.mediaType());
    assertThat(response.fileName()).isEqualTo("contrat.docx");
    try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(response.content()))) {
      List<String> texts = docx.getParagraphs().stream().map(XWPFParagraph::getText).toList();
      assertThat(texts).contains("Contrat de prestation", "• Premier point", "1. Un", "2. Deux");
      XWPFParagraph title =
          docx.getParagraphs().stream()
              .filter(p -> p.getText().equals("Contrat de prestation"))
              .findFirst()
              .orElseThrow();
      assertThat(title.getRuns().get(0).isBold()).isTrue();
      assertThat(title.getRuns().get(0).getFontSizeAsDouble()).isEqualTo(20d);
      XWPFParagraph intro =
          docx.getParagraphs().stream()
              .filter(p -> p.getText().startsWith("Entre"))
              .findFirst()
              .orElseThrow();
      assertThat(intro.getRuns().stream().filter(r -> r.isBold()).map(r -> r.text()))
          .contains("Kossi Mensah");
      assertThat(intro.getText()).contains("ligne deux");

      XWPFTable table = docx.getTables().get(0);
      assertThat(table.getRow(0).getCell(0).getText()).isEqualTo("Désignation");
      assertThat(table.getRow(0).getCell(0).getParagraphs().get(0).getRuns().get(0).isBold())
          .isTrue();
      assertThat(table.getRow(1).getCell(1).getText().replace((char) 160, (char) 32))
          .isEqualTo("150 000 FCFA");
      assertThat(table.getRow(2).getTableCells()).hasSize(1);
      assertThat(table.getRow(2).getCell(0).getText()).contains("cent cinquante mille francs CFA");

      assertThat(docx.getAllPictures()).hasSize(1); // QR code in the body
      assertThat(docx.getHeaderList().get(0).getAllPictures()).hasSize(1); // logo in the header
      assertThat(docx.getHeaderList().get(0).getText()).contains("Société du Golfe");
      assertThat(docx.getFooterList().get(0).getParagraphs().get(0).getCTP().getFldSimpleList())
          .extracting(f -> f.getInstr())
          .containsExactly("PAGE", "NUMPAGES");
      assertThat(texts).contains("Signature : " + PAYLOAD);
    }
  }

  @Test
  void documentHasNoMacroAndNoFieldFromData() throws Exception {
    byte[] docx = render().content();

    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
      for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
        assertThat(entry.getName()).doesNotContain("vbaProject");
        if (entry.getName().equals("word/document.xml")) {
          String xml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
          assertThat(xml).doesNotContain("<w:fldSimple").doesNotContain("<w:instrText");
          assertThat(xml).contains("{ MACROBUTTON x } &lt;script>"); // plain text, escaped
        }
      }
    }
  }
}
