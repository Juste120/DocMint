package io.github.juste120.docmint.spring.web;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.spring.DocMintAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/** Registers {@link Report @Report} support and DocMint error responses in Spring MVC. */
@AutoConfiguration(after = DocMintAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestMappingHandlerAdapter.class)
@ConditionalOnBean(DocMint.class)
public class DocMintWebAutoConfiguration {

  /**
   * Creates the {@code @Report} return value handler.
   *
   * @param docMint the DocMint bean
   * @return the handler
   */
  @Bean
  @ConditionalOnMissingBean
  public ReportReturnValueHandler docMintReportReturnValueHandler(DocMint docMint) {
    return new ReportReturnValueHandler(docMint);
  }

  /**
   * Installs the handler before Spring MVC's built-in handlers.
   *
   * @param adapter Spring MVC's handler adapter
   * @param handler the {@code @Report} handler
   * @return the installer
   */
  @Bean
  static ReturnValueHandlerInstaller docMintReturnValueHandlerInstaller(
      RequestMappingHandlerAdapter adapter, ReportReturnValueHandler handler) {
    return new ReturnValueHandlerInstaller(adapter, handler);
  }

  /**
   * Creates the RFC 9457 error handler.
   *
   * @return the handler
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "docmint.web.problem-details",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public DocMintProblemDetailsHandler docMintProblemDetailsHandler() {
    return new DocMintProblemDetailsHandler();
  }
}
