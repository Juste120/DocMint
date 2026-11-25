package io.github.juste120.docmint.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.juste120.docmint.api.DocMint;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** V5 properties: docmint.pdf.* and docmint.signature.* (FR-403). */
class OfficialDocumentsPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DocMintAutoConfiguration.class));

  private static final Map<String, Object> DATA =
      Map.of("number", "1", "customer", Map.of("name", "A"), "total", 1);

  @Test
  void pdfAAndAccessibilityFromProperties() {
    runner
        .withPropertyValues("docmint.pdf.standard=pdf-a-3b", "docmint.pdf.accessible=true")
        .run(
            context -> {
              byte[] pdf = context.getBean(DocMint.class).pdf("invoice", DATA);
              try (PDDocument document = Loader.loadPDF(pdf)) {
                String xmp =
                    new String(
                        document
                            .getDocumentCatalog()
                            .getMetadata()
                            .exportXMPMetadata()
                            .readAllBytes(),
                        StandardCharsets.UTF_8);
                assertThat(xmp).contains("<pdfaid:part>3</pdfaid:part>");
                assertThat(document.getDocumentCatalog().getStructureTreeRoot()).isNotNull();
              }
            });
  }

  @Test
  void signatureFromAKeyStoreFile(@TempDir Path dir) throws Exception {
    Path keyStore = dir.resolve("sign.p12");
    createKeyStore(keyStore, "Props-Pwd-5".toCharArray());

    runner
        .withPropertyValues(
            "docmint.signature.key-store=file:" + keyStore,
            "docmint.signature.password=Props-Pwd-5",
            "docmint.signature.reason=Facture certifiée")
        .run(
            context -> {
              assertThat(context.getBean(DocMintProperties.class).signature().toString())
                  .doesNotContain("Props-Pwd-5");
              byte[] pdf = context.getBean(DocMint.class).pdf("invoice", DATA);
              try (PDDocument document = Loader.loadPDF(pdf)) {
                assertThat(document.getLastSignatureDictionary().getReason())
                    .isEqualTo("Facture certifiée");
              }
            });
    runner
        .withPropertyValues("docmint.signature.key-store=file:" + dir.resolve("missing.p12"))
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .hasStackTraceContaining("docmint.signature.key-store"));
  }

  private static void createKeyStore(Path path, char[] password) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    X500Name name = new X500Name("CN=DocMint Test");
    Date now = new Date();
    var certificate =
        new JcaX509CertificateConverter()
            .getCertificate(
                new JcaX509v3CertificateBuilder(
                        name,
                        BigInteger.TWO,
                        now,
                        new Date(now.getTime() + 86_400_000L),
                        name,
                        pair.getPublic())
                    .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
    KeyStore store = KeyStore.getInstance("PKCS12");
    store.load(null, null);
    store.setKeyEntry("key", pair.getPrivate(), password, new Certificate[] {certificate});
    try (OutputStream out = Files.newOutputStream(path)) {
      store.store(out, password);
    }
  }
}
