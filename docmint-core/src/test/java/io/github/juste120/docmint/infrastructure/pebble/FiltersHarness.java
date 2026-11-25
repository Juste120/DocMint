package io.github.juste120.docmint.infrastructure.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;

/** Calls a filter directly, without a template. */
final class FiltersHarness {

  private FiltersHarness() {}

  static String apply(Filter filter, Object input, Map<String, Object> args, Locale locale) {
    PebbleTemplate template =
        (PebbleTemplate)
            Proxy.newProxyInstance(
                FiltersHarness.class.getClassLoader(),
                new Class<?>[] {PebbleTemplate.class},
                (proxy, method, arguments) -> "getName".equals(method.getName()) ? "test" : null);
    EvaluationContext context =
        new EvaluationContext() {
          @Override
          public boolean isStrictVariables() {
            return true;
          }

          @Override
          public Locale getLocale() {
            return locale;
          }

          @Override
          public Object getVariable(String key) {
            return null;
          }
        };
    return String.valueOf(filter.apply(input, args, template, context, 1));
  }
}
