package io.github.juste120.agbale;

import static io.github.juste120.agbale.support.PdfText.pages;
import static io.github.juste120.agbale.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.RenderRequest;
import io.github.juste120.agbale.api.RenderResponse;
import io.github.juste120.agbale.support.Fixtures;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** User story 1: a PDF from an HTML template in one line, without configuration. */
class OnePdfLineTest {

  private final Agbale agbale = Agbale.createDefault();

  @Test
  void rendersVariablesLoopsConditionsAndLayout() {
    byte[] pdf = agbale.pdf("invoice", Fixtures.invoice());

    assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    String text = text(pdf);
    assertThat(text)
        .contains("Facture F-2026-001")
        .contains("Kossi Mensah")
        .contains("Ciment 50 kg")
        .contains("Transport Lomé - Kpalimé")
        .contains("Total : 1 250 000 FCFA")
        .contains("Société Agbalẽ SARL — Lomé") // layout header
        .contains("Merci de votre confiance") // layout footer
        .doesNotContain("Remise");
  }

  @Test
  void showsConditionalBlockWhenValuePresent() {
    var base = Fixtures.invoice();
    var withDiscount =
        new Fixtures.Invoice(
            base.number(), base.customer(), base.lines(), new BigDecimal("50000"), base.date());

    assertThat(text(agbale.pdf("invoice", withDiscount))).contains("Remise : 50 000 FCFA");
  }

  @Test
  void recordPojoAndMapGiveTheSameText() {
    Map<String, Object> customer = Map.of("name", "Kossi Mensah");
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("number", "F-2026-001");
    map.put("customer", customer);
    map.put("lines", Fixtures.invoice().lines());
    map.put("discount", null);

    String fromRecord = text(agbale.pdf("invoice", Fixtures.invoice()));
    String fromBean = text(agbale.pdf("invoice", new Fixtures.InvoiceBean()));
    String fromMap = text(agbale.pdf("invoice.html", map));

    assertThat(fromBean).isEqualTo(fromRecord);
    assertThat(fromMap).isEqualTo(fromRecord);
  }

  @Test
  void exposesNonObjectRootAsData() {
    assertThat(text(agbale.pdf("list", List.of("un", "deux"))))
        .contains("Item un")
        .contains("Item deux");
  }

  @Test
  void acceptsPlainHtml5AndEmptyTemplates() {
    assertThat(text(agbale.pdf("simple", Map.of("name", "Ama")))).contains("Bonjour Ama");
    assertThat(pages(agbale.pdf("empty", Map.of()))).isEqualTo(1);
  }

  @Test
  void advancedApiReturnsMetadata() {
    RenderResponse response =
        agbale.render(RenderRequest.of("invoice", Fixtures.invoice()));

    assertThat(response.mediaType()).isEqualTo("application/pdf");
    assertThat(response.fileName()).isEqualTo("invoice.pdf");
    assertThat(response.pageCount()).isEqualTo(1);
    assertThat(response.content()).isNotEmpty();
  }

  @Test
  void listsAvailableTemplates() {
    assertThat(agbale.availableTemplates()).contains("invoice", "layouts/base", "simple");
  }
}
