package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.RenderException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts the data given by the developer (record, object with getters, map) into the template
 * model: an immutable tree made only of maps, lists and simple values.
 *
 * <p>This is a security boundary: templates never receive a reference to an arbitrary Java object,
 * so they cannot call its methods or navigate to sensitive classes ({@code Class}, {@code
 * ClassLoader}, services). Allowed leaf values are {@code String}, {@code Boolean}, JDK numbers,
 * {@code java.time} values and {@code null}; enums become their name.
 *
 * <p>The properties of the root object become top-level variables. A root that is a collection, an
 * array or a simple value is exposed under the variable {@value #DATA_VARIABLE}.
 */
public final class TemplateModelMapper {

  /** Name of the variable holding a root that is not an object. */
  public static final String DATA_VARIABLE = "data";

  static final int MAX_DEPTH = 32;
  static final int MAX_NODES = 100_000;

  private static final Map<Class<?>, List<Property>> PROPERTIES = new ConcurrentHashMap<>();

  /**
   * Converts the root data object into template variables.
   *
   * @param data the data, not null
   * @return the immutable variables
   * @throws RenderException if the data cannot be converted (cycle, too deep, failing getter)
   */
  public Map<String, Object> toModel(Object data) {
    Conversion conversion = new Conversion();
    Object converted = conversion.convert(data, 0);
    if (converted instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> model = (Map<String, Object>) map;
      return model;
    }
    Map<String, Object> model = new LinkedHashMap<>();
    model.put(DATA_VARIABLE, converted);
    return Collections.unmodifiableMap(model);
  }

  /** State of one conversion: ancestors for cycle detection and a node counter. */
  private static final class Conversion {
    private final Set<Object> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
    private int nodes;

    Object convert(Object value, int depth) {
      if (++nodes > MAX_NODES) {
        throw new RenderException(
            "The data passed to the template is too large (more than "
                + MAX_NODES
                + " values). Pass only what the document needs.",
            null);
      }
      if (value == null) {
        return null;
      }
      Object simple = simpleValue(value);
      if (simple != null) {
        return simple;
      }
      if (value instanceof Optional<?> optional) {
        return convert(optional.orElse(null), depth);
      }
      if (depth >= MAX_DEPTH) {
        throw new RenderException(
            "The data passed to the template is nested more than "
                + MAX_DEPTH
                + " levels deep. Flatten your response object.",
            null);
      }
      if (!ancestors.add(value)) {
        throw new RenderException(
            "The data passed to the template contains a reference cycle (an object of type '"
                + value.getClass().getSimpleName()
                + "' contains itself). Use a response object without back-references, never a"
                + " JPA entity.",
            null);
      }
      try {
        return convertComposite(value, depth + 1);
      } finally {
        ancestors.remove(value);
      }
    }

    private Object convertComposite(Object value, int depth) {
      if (value instanceof Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          result.put(String.valueOf(entry.getKey()), convert(entry.getValue(), depth));
        }
        return Collections.unmodifiableMap(result);
      }
      if (value instanceof Collection<?> collection) {
        List<Object> result = new ArrayList<>(collection.size());
        for (Object element : collection) {
          result.add(convert(element, depth));
        }
        return Collections.unmodifiableList(result);
      }
      if (value.getClass().isArray()) {
        int length = Array.getLength(value);
        List<Object> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
          result.add(convert(Array.get(value, i), depth));
        }
        return Collections.unmodifiableList(result);
      }
      if (isJdkType(value.getClass())) {
        // Never introspect JDK classes: their getters reach class loaders, threads, files...
        return value.toString();
      }
      Map<String, Object> result = new LinkedHashMap<>();
      for (Property property : properties(value.getClass())) {
        result.put(property.name(), convert(property.read(value), depth));
      }
      return Collections.unmodifiableMap(result);
    }
  }

  private static Object simpleValue(Object value) {
    if (value instanceof String || value instanceof Boolean) {
      return value;
    }
    if (value instanceof CharSequence || value instanceof Character) {
      return value.toString();
    }
    if (value instanceof Number number) {
      return number(number);
    }
    if (value instanceof Enum<?> e) {
      return e.name();
    }
    if (value instanceof Date date) {
      // Also covers java.sql.Date/Timestamp without depending on the java.sql module.
      return Instant.ofEpochMilli(date.getTime());
    }
    if (value instanceof Calendar calendar) {
      return ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
    }
    Package pkg = value.getClass().getPackage();
    if (pkg != null && pkg.getName().equals("java.time")) {
      return value; // immutable value types; templates cannot call their methods
    }
    if (value instanceof Locale || value instanceof UUID || value instanceof Currency) {
      return value.toString();
    }
    return null;
  }

  private static Object number(Number number) {
    if (number instanceof BigDecimal
        || number instanceof BigInteger
        || number instanceof Integer
        || number instanceof Long
        || number instanceof Double
        || number instanceof Float
        || number instanceof Short
        || number instanceof Byte) {
      return number;
    }
    try {
      return new BigDecimal(number.toString());
    } catch (NumberFormatException e) {
      return number.toString();
    }
  }

  private static boolean isJdkType(Class<?> type) {
    String name = type.getName();
    return name.startsWith("java.")
        || name.startsWith("javax.")
        || name.startsWith("jdk.")
        || name.startsWith("sun.")
        || name.startsWith("com.sun.");
  }

  private static List<Property> properties(Class<?> type) {
    return PROPERTIES.computeIfAbsent(type, TemplateModelMapper::introspect);
  }

  private static List<Property> introspect(Class<?> type) {
    List<Property> properties = new ArrayList<>();
    if (type.isRecord()) {
      for (RecordComponent component : type.getRecordComponents()) {
        properties.add(new Property(component.getName(), accessible(component.getAccessor())));
      }
      return List.copyOf(properties);
    }
    Map<String, Method> getters = new LinkedHashMap<>();
    for (Method method : type.getMethods()) {
      String propertyName = propertyName(method);
      if (propertyName != null) {
        getters.putIfAbsent(propertyName, method);
      }
    }
    getters.forEach((name, method) -> properties.add(new Property(name, accessible(method))));
    return List.copyOf(properties);
  }

  private static String propertyName(Method method) {
    if (Modifier.isStatic(method.getModifiers())
        || method.getParameterCount() != 0
        || method.getReturnType() == void.class
        || method.getDeclaringClass() == Object.class) {
      return null;
    }
    String name = method.getName();
    if (name.startsWith("get") && name.length() > 3) {
      return decapitalize(name.substring(3));
    }
    if (name.startsWith("is")
        && name.length() > 2
        && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
      return decapitalize(name.substring(2));
    }
    return null;
  }

  /** Same rule as JavaBeans: {@code URL} stays {@code URL}, {@code Name} becomes {@code name}. */
  static String decapitalize(String name) {
    if (name.length() > 1
        && Character.isUpperCase(name.charAt(0))
        && Character.isUpperCase(name.charAt(1))) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  private static Method accessible(Method method) {
    if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
      method.trySetAccessible();
    }
    return method;
  }

  /** A readable property of a data class. */
  private record Property(String name, Method getter) {
    Object read(Object target) {
      try {
        return getter.invoke(target);
      } catch (IllegalAccessException e) {
        throw new RenderException(
            "Cannot read property '"
                + name
                + "' of '"
                + target.getClass().getSimpleName()
                + "': make the class public, or pass a Map.",
            e);
      } catch (InvocationTargetException e) {
        throw new RenderException(
            "Reading property '"
                + name
                + "' of '"
                + target.getClass().getSimpleName()
                + "' failed with "
                + e.getCause().getClass().getSimpleName()
                + ". Fix the getter or prepare the value before rendering.",
            e.getCause());
      }
    }
  }
}
