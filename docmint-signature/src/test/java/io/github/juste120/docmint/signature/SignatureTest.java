package io.github.juste120.docmint.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.juste120.docmint.api.DocMint;
import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.RenderOptions;
import io.github.juste120.docmint.api.RenderRequest;
import io.github.juste120.docmint.domain.PdfStandard;
import io.github.juste120.docmint.domain.SignatureSettings;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** V5 user story 1: electronic signature (FR-401, FR-404, FR-405). */
class SignatureTest {

  private static final char[] PASSWORD = "Keystore-Pwd-77".toCharArray();
  private static byte[] keyStore;

  @BeforeAll
  static void createCertificate() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    X500Name subject = new X500Name("CN=Mairie de Lomé, O=Test, C=TG");
    Date now = new Date();
    X509CertificateHolder holder =
        new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.ONE,
                now,
                new Date(now.getTime() + 86_400_000L),
                subject,
                pair.getPublic())
            .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate()));
    X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(holder);
    KeyStore store = KeyStore.getInstance("PKCS12");
    store.load(null, null);
    store.setKeyEntry("mairie", pair.getPrivate(), PASSWORD, new Certificate[] {certificate});
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    store.store(out, PASSWORD);
    keyStore = out.toByteArray();
  }

  private static SignatureSettings settings() {
    return SignatureSettings.pkcs12(keyStore, PASSWORD)
        .withReason("Attestation officielle")
        .withLocation("Lomé, Togo");
  }

  /** The signature value is zero-padded to the reserved size: read the first ASN.1 object only. */
  private static CMSSignedData cms(PDSignature signature, byte[] pdf) throws Exception {
    try (ASN1InputStream in = new ASN1InputStream(signature.getContents(pdf))) {
      return new CMSSignedData(
          new CMSProcessableByteArray(signature.getSignedContent(pdf)),
          ContentInfo.getInstance(in.readObject()));
    }
  }

  private static void assertValidSignature(byte[] pdf) throws Exception {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDSignature signature = document.getLastSignatureDictionary();
      assertThat(signature).isNotNull();
      assertThat(signature.getName()).isEqualTo("Mairie de Lomé");
      assertThat(signature.getReason()).isEqualTo("Attestation officielle");
      assertThat(signature.getLocation()).isEqualTo("Lomé, Togo");
      int[] range = signature.getByteRange();
      assertThat(range[0]).isZero();
      assertThat(range[2] + range[3]).isEqualTo(pdf.length); // the whole file is covered
      CMSSignedData cms = cms(signature, pdf);
      SignerInformation signer = cms.getSignerInfos().getSigners().iterator().next();
      X509CertificateHolder certificate =
          (X509CertificateHolder)
              cms.getCertificates().getMatches(signer.getSID()).iterator().next();
      assertThat(signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate)))
          .isTrue();
    }
  }

  @Test
  void signedPdfVerifies() throws Exception {
    byte[] pdf =
        DocMint.createDefault()
            .render(
                RenderRequest.of("simple", Map.of("name", "Ama"))
                    .withOptions(RenderOptions.defaults().withSignature(settings())))
            .content();

    assertValidSignature(pdf);
  }

  @Test
  void modifiedPdfNoLongerVerifies() throws Exception {
    byte[] pdf =
        DocMint.builder().signature(settings()).build().pdf("simple", Map.of("name", "Ama"));
    String text = new String(pdf, StandardCharsets.ISO_8859_1);
    int index = text.indexOf("/Producer");
    pdf[index + 2] = (byte) (pdf[index + 2] ^ 1); // tamper with the signed bytes

    try (PDDocument document = Loader.loadPDF(pdf)) {
      PDSignature signature = document.getLastSignatureDictionary();
      CMSSignedData cms = cms(signature, pdf);
      SignerInformation signer = cms.getSignerInfos().getSigners().iterator().next();
      X509CertificateHolder certificate =
          (X509CertificateHolder)
              cms.getCertificates().getMatches(signer.getSID()).iterator().next();
      assertThatThrownBy(
              () -> signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate)))
          .isInstanceOf(Exception.class);
    }
  }

  @Test
  void signedPdfAArchive() throws Exception {
    byte[] pdf =
        DocMint.builder()
            .pdfStandard(PdfStandard.PDF_A_3B)
            .signature(settings())
            .build()
            .pdf("simple", Map.of("name", "Ama"));

    assertValidSignature(pdf);
    try (PDDocument document = Loader.loadPDF(pdf)) {
      assertThat(document.getDocumentCatalog().getOutputIntents()).isNotEmpty();
    }
  }

  @Test
  void wrongPasswordIsAClearErrorWithoutSecrets() {
    SignatureSettings wrong = SignatureSettings.pkcs12(keyStore, "Wrong-Secret-1".toCharArray());

    assertThatThrownBy(
            () ->
                DocMint.createDefault()
                    .render(
                        RenderRequest.of("simple", Map.of("name", "Ama"))
                            .withOptions(RenderOptions.defaults().withSignature(wrong))))
        .isInstanceOfSatisfying(
            InvalidOptionsException.class,
            e -> assertThat(e.code()).isEqualTo("DOCMINT_SIGNATURE_INVALID"))
        .hasMessageContaining("wrong password")
        .hasMessageNotContaining("Wrong-Secret-1");
    assertThatThrownBy(
            () ->
                DocMint.createDefault()
                    .render(
                        RenderRequest.of("simple", Map.of("name", "Ama"))
                            .withOptions(
                                RenderOptions.defaults()
                                    .withSignature(settings().withAlias("nobody")))))
        .hasMessageContaining("alias");
  }
}
