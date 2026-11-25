package io.github.juste120.docmint.infrastructure;

import static io.github.juste120.docmint.support.PdfText.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.TemplateName;
import io.github.juste120.docmint.infrastructure.pebble.PebbleTemplateEngine;
import io.github.juste120.docmint.infrastructure.template.ClasspathTemplateResolver;
import io.github.juste120.docmint.port.TemplateResolver;
import io.github.juste120.docmint.port.TemplateSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** User story 3: fcfa, money, sum and date filters (FR-018 to FR-022). */
class FiltersTest {

  private static final char NBSP = ' ';

  /** Renders an inline template to HTML, without PDF, to check exact filter output. */
  private static String html(String template, Map<String, Object> model, Locale locale) {
    TemplateResolver inline =
        new TemplateResolver() {
          @Override
          public Optional<TemplateSource> resolve(TemplateName name) {
            return Optional.of(new TemplateSource(name, template));
          }

          @Override
          public List<String> availableTemplates() {
            return List.of();
          }

          @Override
          public String location() {
            return "inline";
          }
        };
    return new PebbleTemplateEngine(inline, null, ResourceLimits.defaults(), Locale.FRENCH)
        .render(TemplateName.of("t"), model, locale);
  }

  private static String html(String template, Object value) {
    Map<String, Object> model = new HashMap<>();
    model.put("v", value);
    return html(template, model, Locale.FRENCH).replace(NBSP, ' ');
  }

  @ParameterizedTest
  @CsvSource({
    "1250000, 1 250 000 FCFA",
    "0, 0 FCFA",
    "999, 999 FCFA",
    "1000, 1 000 FCFA",
    "-15000, -15 000 FCFA",
    "1250.5, 1 251 FCFA",
    "1250.49, 1 250 FCFA",
    "'12345678901234567890', 12 345 678 901 234 567 890 FCFA"
  })
  void fcfaFormatsWithoutDecimals(String input, String expected) {
    assertThat(html("{{ v | fcfa }}", new BigDecimal(input))).isEqualTo(expected);
  }

  @Test
  void fcfaAcceptsEveryNumberTypeAndNumericStrings() {
    assertThat(html("{{ v | fcfa }}", 1500)).isEqualTo("1 500 FCFA");
    assertThat(html("{{ v | fcfa }}", 1500L)).isEqualTo("1 500 FCFA");
    assertThat(html("{{ v | fcfa }}", 1500.4d)).isEqualTo("1 500 FCFA");
    assertThat(html("{{ v | fcfa }}", BigInteger.valueOf(2_000_000))).isEqualTo("2 000 000 FCFA");
    assertThat(html("{{ v | fcfa }}", "2500")).isEqualTo("2 500 FCFA");
    assertThat(html("{{ v | fcfa }}", null)).isEmpty();
    assertThat(html("{{ v | fcfa }}", BigDecimal.ONE).indexOf(NBSP)).isEqualTo(-1);
    assertThat(html("{{ v | fcfa }}", Map.of("v", 1000), Locale.FRENCH)).contains("" + NBSP);
  }

  @Test
  void fcfaRejectsNonNumbers() {
    assertThatThrownBy(() -> html("{{ v | fcfa }}", "abc"))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("fcfa");
    assertThatThrownBy(() -> html("{{ v | fcfa }}", Double.NaN))
        .isInstanceOf(RenderException.class);
  }

  @Test
  void moneyFollowsTheLocale() {
    assertThat(html("{{ v | money('EUR') }}", new BigDecimal("1234.5"))).isEqualTo("1 234,50 €");
    assertThat(html("{{ v | money('USD', 'en-US') }}", new BigDecimal("1234.5")))
        .isEqualTo("$1,234.50");
    assertThat(html("{{ v | money('XOF') }}", new BigDecimal("1250000.4")))
        .isEqualTo("1 250 000 F CFA");
    assertThat(
            html("{{ v | money('EUR') }}", Map.of("v", new BigDecimal("10")), Locale.GERMANY)
                .replace(NBSP, ' '))
        .isEqualTo("10,00 €");
    assertThat(html("{{ v | money('EUR') }}", null)).isEmpty();
  }

  @Test
  void moneyNeedsAValidCurrency() {
    assertThatThrownBy(() -> html("{{ v | money }}", 10))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("currency code");
    assertThatThrownBy(() -> html("{{ v | money('ZZZ1') }}", 10))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("ZZZ1");
  }

  @Test
  void sumIsExact() {
    assertThat(html("{{ v | sum }}", List.of(0.1, 0.2))).isEqualTo("0.3");
    assertThat(html("{{ v | sum }}", List.of(new BigDecimal("1000000"), 250000)))
        .isEqualTo("1250000");
    assertThat(html("{{ v | sum }}", List.of())).isEqualTo("0");
    assertThat(html("{{ v | sum }}", null)).isEqualTo("0");
    assertThat(html("{{ v | sum }}", Arrays.asList(1, null, 2))).isEqualTo("3");
  }

  @Test
  void sumReadsPropertiesOfItems() {
    List<Object> lines = new ArrayList<>();
    lines.add(Map.of("amount", 1000, "price", Map.of("total", 5)));
    lines.add(Map.of("amount", 250, "price", Map.of("total", 7)));
    Map<String, Object> withNull = new HashMap<>();
    withNull.put("amount", null);
    withNull.put("price", Map.of("total", 1));
    lines.add(withNull);
    lines.add(null);

    assertThat(html("{{ v | sum('amount') }}", lines)).isEqualTo("1250");
    assertThat(html("{{ v | sum('price.total') }}", lines)).isEqualTo("13");
    assertThatThrownBy(() -> html("{{ v | sum('missing') }}", lines))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("missing");
    assertThatThrownBy(() -> html("{{ v | sum }}", "text")).isInstanceOf(RenderException.class);
    assertThatThrownBy(() -> html("{{ v | sum('a') }}", List.of(1)))
        .isInstanceOf(RenderException.class);
  }

  @Test
  void dateUsesLongFrenchFormatByDefault() {
    LocalDate day = LocalDate.of(2026, 11, 24);

    assertThat(html("{{ v | date }}", day)).isEqualTo("24 novembre 2026");
    assertThat(html("{{ v | date('dd/MM/yyyy') }}", day)).isEqualTo("24/11/2026");
    assertThat(html("{{ v | date('d MMMM yyyy', 'en') }}", day)).isEqualTo("24 November 2026");
    assertThat(html("{{ v | date }}", Map.of("v", day), Locale.ENGLISH))
        .isEqualTo("November 24, 2026");
    assertThat(html("{{ v | date }}", null)).isEmpty();
  }

  @Test
  void dateAcceptsEveryDateType() {
    assertThat(html("{{ v | date('dd/MM/yyyy HH:mm') }}", LocalDateTime.of(2026, 11, 24, 9, 30)))
        .isEqualTo("24/11/2026 09:30");
    assertThat(
            html(
                "{{ v | date }}",
                ZonedDateTime.of(2026, 11, 24, 23, 0, 0, 0, ZoneId.of("Africa/Lome"))))
        .isEqualTo("24 novembre 2026");
    assertThat(html("{{ v | date }}", Instant.parse("2026-11-24T10:00:00Z")))
        .isEqualTo("24 novembre 2026");
    assertThat(html("{{ v | date('yyyy') }}", Date.from(Instant.parse("2026-11-24T10:00:00Z"))))
        .isEqualTo("2026");
    assertThat(html("{{ v | date }}", "2026-11-24")).isEqualTo("24 novembre 2026");
    assertThat(html("{{ v | date('HH:mm') }}", "2026-11-24T08:15:00")).isEqualTo("08:15");
  }

  @Test
  void dateRejectsInvalidValuesAndPatterns() {
    assertThatThrownBy(() -> html("{{ v | date }}", "hier")).isInstanceOf(RenderException.class);
    assertThatThrownBy(() -> html("{{ v | date }}", 42)).isInstanceOf(RenderException.class);
    assertThatThrownBy(() -> html("{{ v | date('HH:mm') }}", LocalDate.of(2026, 1, 1)))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("pattern");
  }

  @Test
  void filtersWorkInsideAPdf() {
    Map<String, Object> data =
        Map.of(
            "amount",
            1250000,
            "price",
            new BigDecimal("1234.5"),
            "day",
            LocalDate.of(2026, 11, 24),
            "lines",
            List.of(Map.of("amount", 1000), Map.of("amount", 500)));

    String text = text(DocMint.createDefault().pdf("formats", data));

    assertThat(text)
        .contains("F:1 250 000 FCFA")
        .contains("M:1 234,50 €")
        .contains("D:24 novembre 2026")
        .contains("P:24/11/2026")
        .contains("S:1 500 FCFA");
  }

  @Test
  void classpathResolverRootIsValidated() {
    assertThatThrownBy(() -> ClasspathTemplateResolver.normaliseRoot("../x"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ClasspathTemplateResolver.normaliseRoot("  "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(ClasspathTemplateResolver.normaliseRoot("classpath:/reports/")).isEqualTo("reports");
  }
}
