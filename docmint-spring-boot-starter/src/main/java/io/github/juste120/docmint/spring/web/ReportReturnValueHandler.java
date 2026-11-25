package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.api.RenderResponse;
import io.github.juste120.docmint.domain.OutputFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles the return value of {@link Report @Report} methods: renders it and writes the PDF with
 * the {@code Content-Type}, {@code Content-Length} and {@code Content-Disposition} headers.
 */
public final class ReportReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final DocMint docMint;

  /**
   * Creates the handler.
   *
   * @param docMint the DocMint instance used to render
   */
  public ReportReturnValueHandler(DocMint docMint) {
    this.docMint = docMint;
  }

  @Override
  public boolean supportsReturnType(MethodParameter returnType) {
    return AnnotatedElementUtils.hasAnnotation(returnType.getAnnotatedElement(), Report.class)
        || returnType.hasMethodAnnotation(Report.class);
  }

  @Override
  public void handleReturnValue(
      Object returnValue,
      MethodParameter returnType,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest)
      throws IOException {
    Report report =
        AnnotatedElementUtils.findMergedAnnotation(returnType.getAnnotatedElement(), Report.class);
    if (report == null) {
      report = returnType.getMethodAnnotation(Report.class);
    }
    String method = returnType.getExecutable().getName();
    checkReturnType(returnType, method);
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
    if (request == null || response == null) {
      throw new InvalidReportMethodException("@Report only works in servlet web applications.");
    }
    checkAccept(request, report.format());
    String template = report.value();
    boolean tabular = report.format() == OutputFormat.CSV || report.format() == OutputFormat.XLSX;
    Object value =
        returnValue instanceof Optional<?> optional ? optional.orElse(null) : returnValue;
    RenderOptions options = RenderOptions.defaults();
    Object data = value;
    if (value instanceof ReportContent content) {
      data = content.data();
      options = content.options();
    }
    if (data == null) {
      throw new RenderException(
          "@Report method '"
              + method
              + "' returned null: return a response object, or answer 404 before rendering.",
          null);
    }

    // A list exported as CSV or Excel needs no template: columns come from @ExcelColumn.
    boolean listExport = template.isBlank() && tabular && data instanceof Collection<?>;
    if (template.isBlank() && !listExport) {
      throw new InvalidReportMethodException(
          "@Report on method '"
              + method
              + "' has no template name: use @Report(\"invoice\"), or return a collection with"
              + " format = CSV or XLSX.");
    }
    String extension = "." + report.format().fileExtension();
    String baseName = listExport ? "export" : template;
    String fileName =
        options.fileName() != null
            ? ReportFileNameResolver.sanitise(options.fileName(), extension)
            : listExport && report.filename().isBlank()
                ? baseName + extension
                : ReportFileNameResolver.resolve(report.filename(), baseName, data, extension);
    RenderOptions effective = options.withFileName(fileName);
    RenderResponse document =
        listExport
            ? docMint.export((Collection<?>) data, report.format(), effective)
            : docMint.render(new RenderRequest(template, data, report.format(), effective));

    ContentDisposition disposition =
        (report.disposition() == Disposition.ATTACHMENT
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
            .filename(document.fileName(), StandardCharsets.UTF_8)
            .build();
    byte[] content = document.content();
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(document.mediaType());
    response.setContentLength(content.length);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
    response.getOutputStream().write(content);
    response.flushBuffer();
    mavContainer.setRequestHandled(true);
  }

  private static void checkReturnType(MethodParameter returnType, String method) {
    Class<?> type = returnType.getParameterType();
    if (type == void.class
        || type == Void.class
        || type == byte[].class
        || HttpEntity.class.isAssignableFrom(type)
        || CharSequence.class.isAssignableFrom(type)) {
      throw new InvalidReportMethodException(
          "@Report method '"
              + method
              + "' must return a data object (record, object with getters or Map), not "
              + type.getSimpleName()
              + ". DocMint renders that object and writes the PDF itself.");
    }
  }

  private static void checkAccept(HttpServletRequest request, OutputFormat format) {
    String accept = request.getHeader(HttpHeaders.ACCEPT);
    if (accept == null || accept.isBlank()) {
      return;
    }
    List<MediaType> accepted;
    try {
      accepted = MediaType.parseMediaTypes(accept);
    } catch (InvalidMediaTypeException e) {
      return;
    }
    MediaType produced = MediaType.parseMediaType(format.mediaType());
    if (accepted.stream().noneMatch(type -> type.isCompatibleWith(produced))) {
      throw new ReportNotAcceptableException(format.mediaType());
    }
  }
}
