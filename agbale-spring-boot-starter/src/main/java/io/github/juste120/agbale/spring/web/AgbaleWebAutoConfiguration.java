package io.github.juste120.agbale.spring.web;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.spring.AgbaleAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/** Registers {@link Report @Report} support and Agbale error responses in Spring MVC. */
@AutoConfiguration(after = AgbaleAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestMappingHandlerAdapter.class)
@ConditionalOnBean(Agbale.class)
public class AgbaleWebAutoConfiguration {

  /**
   * Creates the {@code @Report} return value handler.
   *
   * @param agbale the Agbale bean
   * @return the handler
   */
  @Bean
  @ConditionalOnMissingBean
  public ReportReturnValueHandler agbaleReportReturnValueHandler(Agbale agbale) {
    return new ReportReturnValueHandler(agbale);
  }

  /**
   * Installs the handler before Spring MVC's built-in handlers.
   *
   * @param adapter Spring MVC's handler adapter
   * @param handler the {@code @Report} handler
   * @return the installer
   */
  @Bean
  static ReturnValueHandlerInstaller agbaleReturnValueHandlerInstaller(
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
      prefix = "agbale.web.problem-details",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public AgbaleProblemDetailsHandler agbaleProblemDetailsHandler() {
    return new AgbaleProblemDetailsHandler();
  }
}
