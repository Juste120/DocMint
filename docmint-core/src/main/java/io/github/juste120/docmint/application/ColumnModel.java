package io.github.juste120.docmint.application;

import io.github.juste120.docmint.api.ExcelColumn;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.port.TableData;
import io.github.juste120.docmint.port.TableData.Cell;
import io.github.juste120.docmint.port.TableData.CellType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a collection of objects into a table: columns from {@link ExcelColumn} annotations (or
 * every readable property when none is annotated), one row per object, typed cells.
 */
final class ColumnModel {

  private record Column(String header, int order, int position, String format, Accessor accessor) {}

  @FunctionalInterface
  private interface Accessor {
    Object read(Object target) throws ReflectiveOperationException;
  }

  private ColumnModel() {}

  static TableData toTable(Collection<?> rows, String sheetName) {
    Object sample = rows.stream().filter(r -> r != null).findFirst().orElse(null);
    List<Column> columns = sample == null ? List.of() : columns(sample);
    List<List<Cell>> table = new ArrayList<>();
    table.add(columns.stream().map(c -> Cell.text(c.header())).toList());
    for (Object row : rows) {
      if (row == null) {
        continue;
      }
      List<Cell> cells = new ArrayList<>(columns.size());
      for (Column column : columns) {
        cells.add(cell(read(column, row)));
      }
      table.add(cells);
    }
    return new TableData(sheetName, 1, table, columns.stream().map(Column::format).toList());
  }

  private static List<Column> columns(Object sample) {
    if (sample instanceof Map<?, ?> map) {
      List<Column> columns = new ArrayList<>();
      int position = 0;
      for (Object key : map.keySet()) {
        String name = String.valueOf(key);
        columns.add(
            new Column(name, Integer.MAX_VALUE, position++, "", t -> ((Map<?, ?>) t).get(key)));
      }
      return columns;
    }
    Class<?> type = sample.getClass();
    List<Column> annotated = new ArrayList<>();
    Map<String, Column> all = new LinkedHashMap<>();
    int position = 0;
    if (type.isRecord()) {
      for (RecordComponent component : type.getRecordComponents()) {
        Method accessor = accessible(component.getAccessor());
        ExcelColumn annotation = component.getAnnotation(ExcelColumn.class);
        Column column = column(component.getName(), annotation, position++, accessor::invoke);
        all.put(component.getName(), column);
        if (annotation != null) {
          annotated.add(column);
        }
      }
    } else {
      for (Field field : fields(type)) {
        ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
        if (annotation != null) {
          Method getter = getter(type, field.getName());
          AccessibleObject target = getter != null ? accessible(getter) : accessible(field);
          Accessor accessor = getter != null ? getter::invoke : t -> ((Field) target).get(t);
          annotated.add(column(field.getName(), annotation, position++, accessor));
        }
      }
      for (Method method : type.getMethods()) {
        String property = property(method);
        if (property == null) {
          continue;
        }
        ExcelColumn annotation = method.getAnnotation(ExcelColumn.class);
        Column column = column(property, annotation, position++, accessible(method)::invoke);
        all.putIfAbsent(property, column);
        if (annotation != null) {
          annotated.add(column);
        }
      }
    }
    if (annotated.isEmpty()) {
      List<Column> columns = new ArrayList<>(all.values());
      if (!type.isRecord()) {
        // getMethods() has no guaranteed order: sort bean properties by name to stay stable.
        columns.sort(Comparator.comparing(Column::header));
      }
      return columns;
    }
    List<Column> columns = new ArrayList<>(annotated);
    columns.sort(Comparator.comparingInt(Column::order).thenComparingInt(Column::position));
    return columns;
  }

  private static Column column(
      String property, ExcelColumn annotation, int position, Accessor accessor) {
    String header =
        annotation == null || annotation.name().isBlank() ? property : annotation.name();
    int order = annotation == null ? Integer.MAX_VALUE : annotation.order();
    String format = annotation == null ? "" : annotation.format();
    return new Column(header, order, position, format, accessor);
  }

  private static List<Field> fields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field field : c.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          fields.add(field);
        }
      }
    }
    return fields;
  }

  private static Method getter(Class<?> type, String name) {
    String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    for (String candidate : new String[] {"get" + suffix, "is" + suffix}) {
      try {
        return type.getMethod(candidate);
      } catch (NoSuchMethodException e) {
        // try the next form
      }
    }
    return null;
  }

  private static String property(Method method) {
    if (Modifier.isStatic(method.getModifiers())
        || method.getParameterCount() != 0
        || method.getDeclaringClass() == Object.class) {
      return null;
    }
    String name = method.getName();
    if (name.startsWith("get") && name.length() > 3) {
      return TemplateModelMapper.decapitalize(name.substring(3));
    }
    if (name.startsWith("is")
        && name.length() > 2
        && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
      return TemplateModelMapper.decapitalize(name.substring(2));
    }
    return null;
  }

  private static <T extends AccessibleObject> T accessible(T member) {
    member.trySetAccessible();
    return member;
  }

  private static Object read(Column column, Object row) {
    try {
      return column.accessor().read(row);
    } catch (InvocationTargetException e) {
      throw new RenderException(
          "Reading column '"
              + column.header()
              + "' failed with "
              + e.getCause().getClass().getSimpleName()
              + ".",
          e.getCause());
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new RenderException(
          "Cannot read column '" + column.header() + "': make the class and the member public.", e);
    }
  }

  static Cell cell(Object value) {
    if (value == null) {
      return Cell.text("");
    }
    if (value instanceof BigDecimal || value instanceof BigInteger || value instanceof Number) {
      BigDecimal number =
          value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
      return new Cell(number.toPlainString(), number, CellType.NUMBER, 1);
    }
    if (value instanceof Boolean bool) {
      return new Cell(bool.toString(), bool, CellType.BOOLEAN, 1);
    }
    if (value instanceof LocalDate || value instanceof LocalDateTime) {
      return new Cell(value.toString(), value, CellType.DATE, 1);
    }
    if (value instanceof OffsetDateTime offset) {
      return cell(offset.toLocalDateTime());
    }
    if (value instanceof ZonedDateTime zoned) {
      return cell(zoned.toLocalDateTime());
    }
    if (value instanceof Instant instant) {
      return cell(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
    if (value instanceof Date date) {
      return cell(Instant.ofEpochMilli(date.getTime()));
    }
    if (value instanceof Enum<?> e) {
      return Cell.text(e.name());
    }
    return Cell.text(value.toString());
  }
}
