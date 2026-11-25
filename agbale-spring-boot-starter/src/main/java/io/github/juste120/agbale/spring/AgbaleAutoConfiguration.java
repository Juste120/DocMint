package io.github.juste120.agbale.spring;

import io.github.juste120.agbale.api.Agbale;
import io.github.juste120.agbale.api.AgbaleBuilder;
import io.github.juste120.agbale.domain.FontDefinition;
import io.github.juste120.agbale.domain.ResourceLimits;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Auto-configures a shared {@link Agbale} bean from {@link AgbaleProperties}. Backs off when the
 * application defines its own {@code Agbale} bean.
 */
@AutoConfiguration
@EnableConfigurationProperties(AgbaleProperties.class)
public class AgbaleAutoConfiguration {

  /**
   * Creates the Agbale bean.
   *
   * @param properties the {@code agbale.*} properties
   * @param resourceLoader gives the application class loader (also inside executable jars)
   * @return the configured instance
   */
  @Bean
  @ConditionalOnMissingBean
  public Agbale agbale(AgbaleProperties properties, ResourceLoader resourceLoader) {
    AgbaleBuilder builder =
        Agbale.builder()
            .templatesPath(properties.templatesPath())
            .cache(properties.cache().enabled())
            .locale(properties.locale())
            .limits(
                new ResourceLimits(
                    properties.limits().maxTemplateSize().toBytes(),
                    properties.limits().maxPages(),
                    properties.limits().maxRenderTime()))
            .missingResources(properties.resources().missing());
    ClassLoader classLoader = resourceLoader.getClassLoader();
    if (classLoader != null) {
      builder.classLoader(classLoader);
    }
    for (String host : properties.resources().allowedHosts()) {
      builder.allowRemoteHost(host);
    }
    for (AgbaleProperties.Font font : properties.fonts()) {
      builder.font(
          new FontDefinition(font.family(), font.location(), font.weight(), font.italic()));
    }
    return builder.build();
  }
}
