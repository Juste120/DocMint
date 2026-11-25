package io.github.juste120.docmint.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.LimitExceededException;
import io.github.juste120.docmint.spring.web.DocMintProblemDetailsHandler;
import io.github.juste120.docmint.spring.web.ReportReturnValueHandler;
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
class DocMintAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  DocMintAutoConfiguration.class,
                  io.github.juste120.docmint.spring.web.DocMintWebAutoConfiguration.class));

  private final WebApplicationContextRunner webRunner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  DocMintAutoConfiguration.class,
                  io.github.juste120.docmint.spring.web.DocMintWebAutoConfiguration.class,
                  WebMvcAutoConfiguration.class,
                  HttpMessageConvertersAutoConfiguration.class,
                  JacksonAutoConfiguration.class));

  @Test
  void providesDocMintWithZeroConfiguration() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(DocMint.class);
          assertThat(context.getBean(DocMint.class).availableTemplates()).contains("invoice");
          // Not a web application: no web beans, and the bean still works.
          assertThat(context).doesNotHaveBean(ReportReturnValueHandler.class);
          assertThat(
                  context
                      .getBean(DocMint.class)
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
            "docmint.templates-path=other",
            "docmint.cache.enabled=false",
            "docmint.locale=en",
            "docmint.limits.max-pages=1",
            "docmint.limits.max-template-size=10B",
            "docmint.limits.max-render-time=5s",
            "docmint.resources.allowed-hosts=cdn.example.com",
            "docmint.resources.missing=warn",
            "docmint.fonts[0].family=Custom",
            "docmint.fonts[0].location=io/github/juste120/docmint/fonts/NotoSans-Bold.ttf")
        .run(
            context -> {
              DocMintProperties properties = context.getBean(DocMintProperties.class);
              assertThat(properties.templatesPath()).isEqualTo("other");
              assertThat(properties.resources().missing().name()).isEqualTo("WARN");
              DocMint docMint = context.getBean(DocMint.class);
              assertThat(docMint.availableTemplates()).containsExactly("hello");
              org.assertj.core.api.Assertions.assertThatThrownBy(
                      () -> docMint.pdf("hello", Map.of()))
                  .isInstanceOf(LimitExceededException.class);
            });
  }

  @Test
  void invalidValuesFailAtStartup() {
    runner
        .withPropertyValues("docmint.limits.max-pages=0")
        .run(
            context ->
                assertThat(context).hasFailed().getFailure().hasStackTraceContaining("maxPages"));
    runner
        .withPropertyValues("docmint.resources.missing=maybe")
        .run(context -> assertThat(context).hasFailed());
    runner
        .withPropertyValues("docmint.templates-path=../secret")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("templates path"));
    runner
        .withPropertyValues("docmint.fonts[0].family=X", "docmint.fonts[0].location=fonts/none.ttf")
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("fonts/none.ttf"));
  }

  @Configuration(proxyBeanMethods = false)
  static class OwnDocMint {
    static final DocMint OWN = DocMint.builder().templatesPath("other").build();

    @Bean
    DocMint myDocMint() {
      return OWN;
    }
  }

  @Test
  void backsOffWhenTheApplicationDefinesItsOwnBean() {
    runner
        .withUserConfiguration(OwnDocMint.class)
        .run(context -> assertThat(context.getBean(DocMint.class)).isSameAs(OwnDocMint.OWN));
  }

  @Test
  void registersWebSupportInServletApplications() {
    webRunner.run(
        context -> {
          assertThat(context).hasSingleBean(ReportReturnValueHandler.class);
          assertThat(context).hasSingleBean(DocMintProblemDetailsHandler.class);
        });
    webRunner
        .withPropertyValues("docmint.web.problem-details.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(DocMintProblemDetailsHandler.class));
  }
}
