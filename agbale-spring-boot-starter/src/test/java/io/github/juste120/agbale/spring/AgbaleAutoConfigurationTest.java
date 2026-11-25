package io.github.juste120.agbale.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.LimitExceededException;
import io.github.juste120.agbale.spring.web.AgbaleProblemDetailsHandler;
import io.github.juste120.agbale.spring.web.ReportReturnValueHandler;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FR-027, FR-028, FR-033: zero-configuration bean, properties, back-off, non-web contexts. */
class AgbaleAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  AgbaleAutoConfiguration.class,
                  io.github.juste120.agbale.spring.web.AgbaleWebAutoConfiguration.class));

  private final WebApplicationContextRunner webRunner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  AgbaleAutoConfiguration.class,
                  io.github.juste120.agbale.spring.web.AgbaleWebAutoConfiguration.class,
                  WebMvcAutoConfiguration.class,
                  HttpMessageConvertersAutoConfiguration.class,
                  JacksonAutoConfiguration.class));

  @Test
  void providesAgbaleWithZeroConfiguration() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(Agbale.class);
          assertThat(context.getBean(Agbale.class).availableTemplates()).contains("invoice");
          // Not a web application: no web beans, and the bean still works.
          assertThat(context).doesNotHaveBean(ReportReturnValueHandler.class);
          assertThat(
                  context
                      .getBean(Agbale.class)
                      .pdf(
                          "invoice",
                          Map.of("number", "1", "customer", Map.of("name", "A"), "total", 1)))
              .isNotEmpty();
        });
  }

  @Test
  void appliesProperties() {
    runner
        .withPropertyValues(
            "agbale.templates-path=other",
            "agbale.cache.enabled=false",
            "agbale.locale=en",
            "agbale.limits.max-pages=1",
            "agbale.limits.max-template-size=10B",
            "agbale.limits.max-render-time=5s",
            "agbale.resources.allowed-hosts=cdn.example.com",
            "agbale.resources.missing=warn",
            "agbale.fonts[0].family=Custom",
            "agbale.fonts[0].location=io/github/juste120/agbale/fonts/NotoSans-Bold.ttf")
        .run(
            context -> {
              AgbaleProperties properties = context.getBean(AgbaleProperties.class);
              assertThat(properties.templatesPath()).isEqualTo("other");
              assertThat(properties.resources().missing().name()).isEqualTo("WARN");
              Agbale agbale = context.getBean(Agbale.class);
              assertThat(agbale.availableTemplates()).containsExactly("hello");
              org.assertj.core.api.Assertions.assertThatThrownBy(
                      () -> agbale.pdf("hello", Map.of()))
                  .isInstanceOf(LimitExceededException.class);
            });
  }

  @Test
  void invalidValuesFailAtStartup() {
    runner
        .withPropertyValues("agbale.limits.max-pages=0")
        .run(
            context ->
                assertThat(context).hasFailed().getFailure().hasStackTraceContaining("maxPages"));
    runner
        .withPropertyValues("agbale.resources.missing=maybe")
        .run(context -> assertThat(context).hasFailed());
    runner
        .withPropertyValues("agbale.templates-path=../secret")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("templates path"));
    runner
        .withPropertyValues("agbale.fonts[0].family=X", "agbale.fonts[0].location=fonts/none.ttf")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("fonts/none.ttf"));
  }

  @Configuration(proxyBeanMethods = false)
  static class OwnAgbale {
    static final Agbale OWN = Agbale.builder().templatesPath("other").build();

    @Bean
    Agbale myAgbale() {
      return OWN;
    }
  }

  @Test
  void backsOffWhenTheApplicationDefinesItsOwnBean() {
    runner
        .withUserConfiguration(OwnAgbale.class)
        .run(context -> assertThat(context.getBean(Agbale.class)).isSameAs(OwnAgbale.OWN));
  }

  @Test
  void registersWebSupportInServletApplications() {
    webRunner.run(
        context -> {
          assertThat(context).hasSingleBean(ReportReturnValueHandler.class);
          assertThat(context).hasSingleBean(AgbaleProblemDetailsHandler.class);
        });
    webRunner
        .withPropertyValues("agbale.web.problem-details.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(AgbaleProblemDetailsHandler.class));
  }
}
