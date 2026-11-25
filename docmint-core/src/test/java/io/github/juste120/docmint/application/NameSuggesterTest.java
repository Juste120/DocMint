package io.github.juste120.docmint.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NameSuggesterTest {

  @Test
  void suggestsTheClosestName() {
    List<String> names = List.of("facture", "recu", "attestation");

    assertThat(NameSuggester.suggest("factur", names)).isEqualTo("facture");
    assertThat(NameSuggester.suggest("Facture", names)).isEqualTo("facture");
    assertThat(NameSuggester.suggest("attestaion", names)).isEqualTo("attestation");
    assertThat(NameSuggester.suggest("zzzzzzzz", names)).isNull();
    assertThat(NameSuggester.suggest("x", List.of())).isNull();
    assertThat(NameSuggester.distance("kitten", "sitting")).isEqualTo(3);
  }
}
