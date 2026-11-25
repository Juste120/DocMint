package io.github.juste120.docmint.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field, getter or record component as a column of CSV and Excel exports.
 *
 * <pre>{@code
 * record InvoiceRow(
 *     @ExcelColumn(name = "N°", order = 1) String number,
 *     @ExcelColumn(name = "Client", order = 2) String customer,
 *     @ExcelColumn(name = "Montant", order = 3, format = "#,##0") BigDecimal amount,
 *     String internalNote) {}            // not exported: other members are annotated
 * }</pre>
 *
 * <p>When no member of a class is annotated, every readable property is exported with its name as
 * header: in declaration order for records, in alphabetical order for other classes (whose method
 * order is not defined by Java).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface ExcelColumn {

  /**
   * The column header; defaults to the property name.
   *
   * @return the header
   */
  String name() default "";

  /**
   * The position of the column (smaller first); declaration order breaks ties.
   *
   * @return the order
   */
  int order() default Integer.MAX_VALUE;

  /**
   * The Excel number or date format, e.g. {@code #,##0} or {@code dd/mm/yyyy} (ignored in CSV).
   *
   * @return the format, or empty for the default
   */
  String format() default "";
}
