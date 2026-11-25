package io.github.juste120.docmint.infrastructure.pebble;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import java.util.Map;

/**
 * Registers DocMint's business filters ({@code fcfa}, {@code money}, {@code sum}, {@code date},
 * {@code words}) and functions ({@code qr}, {@code barcode}, {@code chart}, {@code pivot}).
 */
final class DocMintExtension extends AbstractExtension {

  @Override
  public Map<String, Filter> getFilters() {
    return Map.of(
        FcfaFilter.NAME, new FcfaFilter(),
        MoneyFilter.NAME, new MoneyFilter(),
        SumFilter.NAME, new SumFilter(),
        DateFilter.NAME, new DateFilter(),
        WordsFilter.NAME, new WordsFilter());
  }

  @Override
  public Map<String, Function> getFunctions() {
    return Map.of(
        CodeFunctions.Qr.NAME, new CodeFunctions.Qr(),
        CodeFunctions.Barcode.NAME, new CodeFunctions.Barcode(),
        ChartFunction.NAME, new ChartFunction(),
        PivotFunction.NAME, new PivotFunction());
  }
}
