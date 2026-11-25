package io.github.juste120.docmint.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.PivotTable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** V4 user story 2: pivot tables (FR-303). */
class PivotTest {

  private static final List<Map<String, Object>> SALES =
      List.of(
          Map.of("city", "Lomé", "month", "Fév", "amount", 200),
          Map.of("city", "Lomé", "month", "Jan", "amount", 100),
          Map.of("city", "Kara", "month", "Jan", "amount", 50),
          Map.of("city", "Lomé", "month", "Jan", "amount", 30));

  private String csv(String aggregate, boolean sort) {
    Map<String, Object> data = new HashMap<>();
    data.put("sales", SALES);
    data.put("aggregate", aggregate);
    data.put("sort", sort);
    byte[] csv =
        DocMint.createDefault()
            .render(new RenderRequest("v4/pivot", data, OutputFormat.CSV, null))
            .content();
    return new String(csv, StandardCharsets.UTF_8).substring(1);
  }

  @Test
  void sumInFirstAppearanceOrder() {
    assertThat(csv("sum", false).split("\r\n"))
        .containsExactly(
            "Ville;Fév;Jan;Total", "Lomé;200;130;330", "Kara;'-;50;50", "Total;200;180;380");
  }

  @Test
  void otherAggregatesAndSorting() {
    assertThat(csv("count", true).split("\r\n"))
        .containsExactly("Ville;Fév;Jan;Total", "Kara;'-;1;1", "Lomé;1;2;3", "Total;1;3;4");
    assertThat(csv("avg", true)).contains("Lomé;200.00;65.00;110.00");
    assertThat(csv("min", true)).contains("Total;200;30;30");
    assertThat(csv("max", true)).contains("Total;200;100;200");
  }

  @Test
  void javaApiAndLimits() {
    PivotTable table =
        PivotTable.compute(
            List.of("a:1", "a:2", "b:3"),
            s -> s.split(":")[0],
            s -> "x",
            s -> new BigDecimal(s.split(":")[1]),
            PivotTable.Aggregate.SUM,
            false);
    assertThat(table.rowKeys()).containsExactly("a", "b");
    assertThat(table.cells())
        .containsExactly(List.of(new BigDecimal("3")), List.of(new BigDecimal("3")));
    assertThat(table.total()).isEqualByComparingTo("6");

    List<Integer> tooMany = new ArrayList<>();
    for (int i = 0; i <= PivotTable.MAX_ROWS; i++) {
      tooMany.add(i);
    }
    assertThatThrownBy(
            () ->
                PivotTable.compute(
                    tooMany, i -> i, i -> i, i -> BigDecimal.ONE, PivotTable.Aggregate.SUM, false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PivotTable.Aggregate.parse("median"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> DocMint.createDefault().pdf("v4/pivot-bad", Map.of("sales", SALES)))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("unknown aggregate");
  }
}
