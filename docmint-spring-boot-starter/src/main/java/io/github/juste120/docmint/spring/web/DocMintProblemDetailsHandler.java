package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.CodeGenerationException;
import io.github.juste120.docmint.api.DocMintException;
import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.InvalidTemplateNameException;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.api.ResourceMissingException;
import io.github.juste120.docmint.api.ResourceRefusedException;
import io.github.juste120.docmint.api.TemplateNotFoundException;
import io.github.juste120.docmint.api.TemplateSyntaxException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns DocMint errors into RFC 9457 Problem Details ({@code application/problem+json}) with a
 * stable {@code code} member. Responses never contain stack traces or internal paths.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocMintProblemDetailsHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DocMintProblemDetailsHandler.class);
  private static final String DOCS =
      "https://github.com/Juste120/DocMint/blob/main/docs/user-guide.md#error-";

  private static final Map<String, String> TITLES =
      Map.ofEntries(
          Map.entry(TemplateNotFoundException.CODE, "Template not found"),
          Map.entry(TemplateSyntaxException.CODE, "Template syntax error"),
          Map.entry(RenderException.EVALUATION_CODE, "Template evaluation error"),
          Map.entry(InvalidTemplateNameException.CODE, "Invalid template name"),
          Map.entry(ResourceRefusedException.CODE, "Resource refused"),
          Map.entry(ResourceMissingException.CODE, "Resource missing"),
          Map.entry(LimitExceededException.CODE, "Limit exceeded"),
          Map.entry(InvalidReportMethodException.CODE, "Invalid @Report method"),
          Map.entry(ReportNotAcceptableException.CODE, "Not acceptable"),
          Map.entry(CodeGenerationException.CODE, "Invalid QR code or barcode"),
          Map.entry(InvalidOptionsException.PREVIEW_PAGE_OUT_OF_RANGE, "Preview page out of range"),
          Map.entry(InvalidOptionsException.PROTECTION_INVALID, "Invalid protection"),
          Map.entry(InvalidOptionsException.WATERMARK_INVALID, "Invalid watermark"));

  /**
   * Converts a DocMint error.
   *
   * @param error the error
   * @param request the current request
   * @return the problem response
   */
  @ExceptionHandler(DocMintException.class)
  public ResponseEntity<ProblemDetail> handle(DocMintException error, HttpServletRequest request) {
    HttpStatus status =
        error instanceof ReportNotAcceptableException
            ? HttpStatus.NOT_ACCEPTABLE
            : HttpStatus.INTERNAL_SERVER_ERROR;
    if (status.is5xxServerError()) {
      LOG.warn(
          "DocMint error on {}: [{}] {}",
          request.getRequestURI(),
          error.code(),
          error.getMessage());
      LOG.debug("DocMint error details", error);
    }
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, error.getMessage());
    problem.setTitle(TITLES.getOrDefault(error.code(), "Document generation failed"));
    problem.setType(URI.create(DOCS + error.code().toLowerCase(Locale.ROOT).replace('_', '-')));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", error.code());
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
