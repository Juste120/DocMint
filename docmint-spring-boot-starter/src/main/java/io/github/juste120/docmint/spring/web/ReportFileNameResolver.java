package io.github.juste120.docmint.spring.web;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the download file name of a {@link Report @Report} endpoint.
 *
 * <p>Placeholders ({@code {number}}, {@code {customer.code}}) are read from the returned object:
 * record components, public getters or map keys. Resolved values are sanitised because they end up
 * in an HTTP header: path separators, quotes, semicolons and control characters (including CR and
 * LF, which would allow header injection) are replaced by {@code _}. The result is at most {@value
 * #MAX_LENGTH} characters and always ends with the extension of the format ({@code .pdf}, {@code
 * .png}).
 */
final class ReportFileNameResolver {

  static final int MAX_LENGTH = 150;

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_][\\w.]*)}");
  private static final String PDF = ".pdf";

  private ReportFileNameResolver() {}

  static String resolve(String pattern, String templateName, Object data) {
    return resolve(pattern, templateName, data, PDF);
  }

  static String resolve(String pattern, String templateName, Object data, String extension) {
    String fallback = templateName.substring(templateName.lastIndexOf('/') + 1) + extension;
    if (pattern == null || pattern.isBlank()) {
      return sanitise(fallback, extension);
    }
    Matcher matcher = PLACEHOLDER.matcher(pattern);
    StringBuilder name = new StringBuilder();
    while (matcher.find()) {
      Object value = read(data, matcher.group(1));
      matcher.appendReplacement(
          name, Matcher.quoteReplacement(value == null ? "" : value.toString()));
    }
    matcher.appendTail(name);
    String result = sanitise(name.toString(), extension);
    if (result.isEmpty() || result.equalsIgnoreCase(extension)) {
      return sanitise(fallback, extension);
    }
    return result;
  }

  static String sanitise(String raw) {
    return sanitise(raw, PDF);
  }

  static String sanitise(String raw, String extension) {
    StringBuilder safe = new StringBuilder();
    raw.codePoints()
        .forEach(
            cp -> {
              boolean forbidden =
                  Character.isISOControl(cp)
                      || cp == '/'
                      || cp == '\\'
                      || cp == '"'
                      || cp == ';'
                      || cp == ' '
                      || cp == ' ';
              safe.appendCodePoint(forbidden ? '_' : cp);
            });
    String name = safe.toString().strip();
    while (name.startsWith(".")) {
      name = name.substring(1);
    }
    if (!name.toLowerCase(Locale.ROOT).endsWith(extension) && !name.isEmpty()) {
      name = name + extension;
    }
    if (name.length() > MAX_LENGTH) {
      name = name.substring(0, MAX_LENGTH - extension.length()) + extension;
    }
    return name;
  }

  private static Object read(Object data, String path) {
    Object current = data;
    for (String property : path.split("\\.")) {
      if (current == null) {
        return null;
      }
      current = property(current, property, path);
    }
    return current;
  }

  private static Object property(Object target, String name, String path) {
    if (target instanceof Map<?, ?> map) {
      if (!map.containsKey(name)) {
        throw unknown(path);
      }
      return map.get(name);
    }
    Class<?> type = target.getClass();
    try {
      if (type.isRecord()) {
        for (RecordComponent component : type.getRecordComponents()) {
          if (component.getName().equals(name)) {
            return invoke(component.getAccessor(), target);
          }
        }
        throw unknown(path);
      }
      String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
      for (String getter : new String[] {"get" + suffix, "is" + suffix}) {
        try {
          Method method = type.getMethod(getter);
          if (!Modifier.isStatic(method.getModifiers())) {
            return invoke(method, target);
          }
        } catch (NoSuchMethodException e) {
          // try the next form
        }
      }
    } catch (ReflectiveOperationException e) {
      throw new InvalidReportMethodException(
          "Cannot read property '"
              + path
              + "' for the @Report file name: "
              + e.getClass().getSimpleName());
    }
    throw unknown(path);
  }

  private static Object invoke(Method method, Object target) throws ReflectiveOperationException {
    if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
      method.trySetAccessible();
    }
    return method.invoke(target);
  }

  private static InvalidReportMethodException unknown(String path) {
    return new InvalidReportMethodException(
        "The @Report file name uses '{"
            + path
            + "}', but the returned object has no such property. Use a property of the response"
            + " object, e.g. '{number}'.");
  }
}
