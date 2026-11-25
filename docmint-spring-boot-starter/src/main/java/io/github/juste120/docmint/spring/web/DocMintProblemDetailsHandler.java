package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.DocMintException;
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
      Map.of(
          TemplateNotFoundException.CODE, "Template not found",
          TemplateSyntaxException.CODE, "Template syntax error",
          RenderException.EVALUATION_CODE, "Template evaluation error",
          InvalidTemplateNameException.CODE, "Invalid template name",
          ResourceRefusedException.CODE, "Resource refused",
          ResourceMissingException.CODE, "Resource missing",
          LimitExceededException.CODE, "Limit exceeded",
          InvalidReportMethodException.CODE, "Invalid @Report method",
          ReportNotAcceptableException.CODE, "Not acceptable");

  /**
   * Converts an DocMint error.
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
