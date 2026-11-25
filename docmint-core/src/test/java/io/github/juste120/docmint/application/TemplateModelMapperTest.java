package io.github.juste120.docmint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.RenderException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TemplateModelMapperTest {

  private final TemplateModelMapper mapper = new TemplateModelMapper();

  enum Status {
    PAID
  }

  record Item(String label, int quantity, Status status, Optional<String> note) {}

  record Order(String id, List<Item> items, Map<Integer, String> tags, int[] scores) {}

  public static class Bean {
    public String getName() {
      return "Ama";
    }

    public boolean isActive() {
      return true;
    }

    public String getURL() {
      return "u";
    }

    public static String getStatic() {
      return "no";
    }

    public String getWithArg(int i) {
      return "no";
    }

    public void getNothing() {}

    public String compute() {
      return "no";
    }
  }

  @Test
  void convertsRecordsToImmutableMaps() {
    Order order =
        new Order(
            "C1",
            List.of(new Item("Riz", 2, Status.PAID, Optional.empty())),
            Map.of(1, "urgent"),
            new int[] {4, 5});

    Map<String, Object> model = mapper.toModel(order);

    assertThat(model).containsEntry("id", "C1").containsEntry("tags", Map.of("1", "urgent"));
    assertThat(model.get("scores")).isEqualTo(List.of(4, 5));
    @SuppressWarnings("unchecked")
    Map<String, Object> item = (Map<String, Object>) ((List<?>) model.get("items")).get(0);
    assertThat(item).containsEntry("status", "PAID").containsEntry("quantity", 2);
    assertThat(item.get("note")).isNull();
    assertThatThrownBy(() -> model.put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void convertsJavaBeansUsingPublicGettersOnly() {
    Map<String, Object> model = mapper.toModel(new Bean());

    assertThat(model)
        .containsOnlyKeys("name", "active", "URL")
        .containsEntry("name", "Ama")
        .containsEntry("active", true);
  }

  @Test
  void exposesNonObjectRootsAsData() {
    assertThat(mapper.toModel(List.of(1, 2))).containsEntry("data", List.of(1, 2));
    assertThat(mapper.toModel("texte")).containsEntry("data", "texte");
    assertThat(mapper.toModel(LocalDate.of(2026, 1, 1)))
        .containsEntry("data", LocalDate.of(2026, 1, 1));
  }

  @Test
  void keepsSafeScalarsAndConvertsOthers() {
    Map<String, Object> source = new HashMap<>();
    source.put("decimal", new BigDecimal("1.5"));
    source.put("big", BigInteger.TEN);
    source.put("atomic", new AtomicInteger(7));
    source.put("date", new Date(0));
    source.put("chars", new StringBuilder("sb"));
    source.put("letter", 'c');
    source.put("uuid", UUID.fromString("00000000-0000-0000-0000-000000000001"));
    source.put("locale", Locale.FRANCE);
    source.put("path", Path.of("/tmp/x"));
    Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.setTimeInMillis(0);
    source.put("calendar", calendar);
    source.put("thread", Thread.currentThread());

    Map<String, Object> model = mapper.toModel(source);

    assertThat(model.get("decimal")).isEqualTo(new BigDecimal("1.5"));
    assertThat(model.get("big")).isEqualTo(BigInteger.TEN);
    assertThat(model.get("atomic")).isEqualTo(new BigDecimal("7"));
    assertThat(model.get("date")).isEqualTo(Instant.EPOCH);
    assertThat(model.get("chars")).isEqualTo("sb");
    assertThat(model.get("letter")).isEqualTo("c");
    assertThat(model.get("uuid")).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(model.get("locale")).isEqualTo("fr_FR");
    assertThat(model.get("path")).isInstanceOf(String.class);
    assertThat(model.get("calendar")).isInstanceOf(ZonedDateTime.class);
    // JDK objects are never introspected: no access to class loaders or threads.
    assertThat(model.get("thread")).isInstanceOf(String.class);
  }

  @Test
  void detectsCycles() {
    List<Object> list = new ArrayList<>();
    list.add(list);

    assertThatThrownBy(() -> mapper.toModel(Map.of("self", list)))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("cycle");
  }

  @Test
  void sharedObjectsAreNotCycles() {
    Map<String, Object> shared = Map.of("v", 1);

    assertThat(mapper.toModel(Map.of("a", shared, "b", shared))).containsKeys("a", "b");
  }

  @Test
  void limitsDepthAndSize() {
    Object deep = "leaf";
    for (int i = 0; i < 40; i++) {
      deep = List.of(deep);
    }
    Object tooDeep = deep;
    assertThatThrownBy(() -> mapper.toModel(Map.of("d", tooDeep))).hasMessageContaining("nested");

    List<Integer> huge = new ArrayList<>();
    for (int i = 0; i < TemplateModelMapper.MAX_NODES + 1; i++) {
      huge.add(i);
    }
    assertThatThrownBy(() -> mapper.toModel(huge)).hasMessageContaining("too large");
  }

  public static class FailingBean {
    public String getValue() {
      throw new IllegalStateException("boom");
    }
  }

  @Test
  void reportsFailingGetters() {
    assertThatThrownBy(() -> mapper.toModel(new FailingBean()))
        .isInstanceOf(RenderException.class)
        .hasMessageContaining("value")
        .hasMessageContaining("IllegalStateException");
  }

  @Test
  void decapitalizesLikeJavaBeans() {
    assertThat(TemplateModelMapper.decapitalize("Name")).isEqualTo("name");
    assertThat(TemplateModelMapper.decapitalize("URL")).isEqualTo("URL");
    assertThat(TemplateModelMapper.decapitalize("X")).isEqualTo("x");
  }
}
