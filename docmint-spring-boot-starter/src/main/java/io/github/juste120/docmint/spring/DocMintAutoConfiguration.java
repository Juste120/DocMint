package io.github.juste120.docmint.spring;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.DocMintBuilder;
import io.github.juste120.docmint.domain.FontDefinition;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.Watermark;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Auto-configures a shared {@link DocMint} bean from {@link DocMintProperties}. Backs off when the
 * application defines its own {@code DocMint} bean.
 */
@AutoConfiguration
@EnableConfigurationProperties(DocMintProperties.class)
public class DocMintAutoConfiguration {

  /**
   * Creates the DocMint bean.
   *
   * @param properties the {@code docmint.*} properties
   * @param resourceLoader gives the application class loader (also inside executable jars)
   * @return the configured instance
   */
  @Bean
  @ConditionalOnMissingBean
  public DocMint docMint(DocMintProperties properties, ResourceLoader resourceLoader) {
    DocMintBuilder builder =
        DocMint.builder()
            .templatesPath(properties.templatesPath())
            .cache(properties.cache().enabled())
            .locale(properties.locale())
            .limits(
                new ResourceLimits(
                    properties.limits().maxTemplateSize().toBytes(),
                    properties.limits().maxPages(),
                    properties.limits().maxRenderTime(),
                    properties.limits().maxImagePixels()))
            .missingResources(properties.resources().missing());
    DocMintProperties.WatermarkDefaults watermark = properties.watermark();
    if (watermark.text() != null && !watermark.text().isBlank()) {
      builder.defaultWatermark(
          Watermark.text(watermark.text())
              .withAngle(watermark.angle())
              .withOpacity(watermark.opacity())
              .withColor(watermark.color()));
    }
    ClassLoader classLoader = resourceLoader.getClassLoader();
    if (classLoader != null) {
      builder.classLoader(classLoader);
    }
    for (String host : properties.resources().allowedHosts()) {
      builder.allowRemoteHost(host);
    }
    for (DocMintProperties.Font font : properties.fonts()) {
      builder.font(
          new FontDefinition(font.family(), font.location(), font.weight(), font.italic()));
    }
    return builder.build();
  }
}
