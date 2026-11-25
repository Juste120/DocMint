package io.github.juste120.docmint.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the Angular demo (http://localhost:4200). {@code Content-Disposition} must be exposed,
 * otherwise the browser hides it from JavaScript and Angular cannot read the file name.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:4200")
        .allowedMethods("GET")
        .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION);
  }
}
