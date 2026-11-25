package io.github.juste120.docmint.spring;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.DocMintBuilder;
import io.github.juste120.docmint.domain.FontDefinition;
import io.github.juste120.docmint.domain.ResourceLimits;
import io.github.juste120.docmint.domain.SignatureSettings;
import io.github.juste120.docmint.domain.Watermark;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
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
                    properties.limits().maxImagePixels(),
                    properties.limits().maxExportRows()))
            .missingResources(properties.resources().missing());
    DocMintProperties.WatermarkDefaults watermark = properties.watermark();
    if (watermark.text() != null && !watermark.text().isBlank()) {
      builder.defaultWatermark(
          Watermark.text(watermark.text())
              .withAngle(watermark.angle())
              .withOpacity(watermark.opacity())
              .withColor(watermark.color()));
    }
    builder.pdfStandard(properties.pdf().standard()).accessible(properties.pdf().accessible());
    SignatureSettings signature = signature(properties.signature(), resourceLoader);
    if (signature != null) {
      builder.signature(signature);
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

  private static SignatureSettings signature(
      DocMintProperties.Signature properties, ResourceLoader resourceLoader) {
    if (properties.keyStore() == null || properties.keyStore().isBlank()) {
      return null;
    }
    Resource resource = resourceLoader.getResource(properties.keyStore());
    byte[] keyStore;
    try (InputStream in = resource.getInputStream()) {
      keyStore = in.readAllBytes();
    } catch (IOException e) {
      throw new IllegalStateException(
          "docmint.signature.key-store: cannot read '" + properties.keyStore() + "'", e);
    }
    String password = properties.password() == null ? "" : properties.password();
    SignatureSettings settings = SignatureSettings.pkcs12(keyStore, password.toCharArray());
    if (properties.alias() != null && !properties.alias().isBlank()) {
      settings = settings.withAlias(properties.alias());
    }
    return settings
        .withReason(properties.reason())
        .withLocation(properties.location())
        .withContact(properties.contact());
  }
}
