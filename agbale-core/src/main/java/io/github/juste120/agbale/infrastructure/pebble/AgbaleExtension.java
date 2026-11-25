package io.github.juste120.agbale.infrastructure.pebble;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import java.util.Map;

/** Registers Agbale's business filters: {@code fcfa}, {@code money}, {@code sum}, {@code date}. */
final class AgbaleExtension extends AbstractExtension {

  @Override
  public Map<String, Filter> getFilters() {
    return Map.of(
        FcfaFilter.NAME, new FcfaFilter(),
        MoneyFilter.NAME, new MoneyFilter(),
        SumFilter.NAME, new SumFilter(),
        DateFilter.NAME, new DateFilter());
  }
}
