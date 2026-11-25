package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.domain.OutputFormat;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/**
 * Renders the object returned by a controller method with a DocMint template and writes the PDF to
 * the HTTP response.
 *
 * <pre>{@code
 * @GetMapping("/invoices/{id}/pdf")
 * @Report(value = "invoice", filename = "invoice-{number}.pdf", disposition = Disposition.ATTACHMENT)
 * InvoiceResponse invoice(@PathVariable long id) {
 *   return invoiceService.find(id);
 * }
 * }</pre>
 *
 * <p>The method must return a data object (record, object with getters or map), never a JPA entity:
 * return a response DTO.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Report {

  /**
   * The template name, relative to the templates folder.
   *
   * @return for example {@code invoice}
   */
  @AliasFor("template")
  String value() default "";

  /**
   * Alias of {@link #value()}.
   *
   * @return the template name
   */
  @AliasFor("value")
  String template() default "";

  /**
   * The file name proposed to the browser. Placeholders such as {@code {number}} or {@code
   * {customer.code}} are replaced by properties of the returned object (and sanitised). Defaults to
   * {@code <template>.pdf}.
   *
   * @return the file name pattern
   */
  String filename() default "";

  /**
   * Whether the browser displays ({@link Disposition#INLINE}, default) or downloads the document.
   *
   * @return the disposition
   */
  Disposition disposition() default Disposition.INLINE;

  /**
   * The output format: {@link OutputFormat#PDF} (default), {@link OutputFormat#PNG} (preview of a
   * page), {@link OutputFormat#HTML}, {@link OutputFormat#CSV} or {@link OutputFormat#XLSX} (with
   * {@code docmint-excel}). With CSV or XLSX, a method returning a collection needs no template:
   * columns come from {@code @ExcelColumn}; a method returning a {@code java.util.stream.Stream}
   * with format CSV is streamed row by row. {@link OutputFormat#DOCX} needs {@code docmint-word}.
   * The method may also return a {@code CompletableFuture} of its result.
   *
   * @return the format
   */
  OutputFormat format() default OutputFormat.PDF;
}
