package io.github.juste120.agbale.infrastructure.pebble;

import java.util.Locale;

/** Locale arguments of filters. */
final class Locales {

  private Locales() {}

  static Locale resolve(Object argument, Locale fallback) {
    if (argument == null || argument.toString().isBlank()) {
      return fallback == null ? Locale.FRENCH : fallback;
    }
    return Locale.forLanguageTag(argument.toString().strip().replace('_', '-'));
  }
}
