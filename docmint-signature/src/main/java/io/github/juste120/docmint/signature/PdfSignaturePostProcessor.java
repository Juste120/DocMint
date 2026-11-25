package io.github.juste120.docmint.signature;

import io.github.juste120.docmint.api.InvalidOptionsException;
import io.github.juste120.docmint.api.RenderException;
import io.github.juste120.docmint.domain.OutputFormat;
import io.github.juste120.docmint.domain.RenderedDocument;
import io.github.juste120.docmint.domain.SignatureSettings;
import io.github.juste120.docmint.port.PostProcessContext;
import io.github.juste120.docmint.port.PostProcessor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/**
 * Signs PDFs with a PKCS#12 key store: detached CMS signature ({@code adbe.pkcs7.detached}),
 * SHA-256, certificate chain included, incremental save. It is the last step applied to a PDF.
 *
 * <p>Key store passwords are never logged nor included in error messages, and their copies are
 * wiped after use.
 */
public final class PdfSignaturePostProcessor implements PostProcessor {

  private static final int SIGNATURE_SIZE = 32 * 1024;

  @Override
  public boolean provides(String capability) {
    return SIGNATURE.equals(capability);
  }

  @Override
  public boolean appliesTo(RenderedDocument document, PostProcessContext context) {
    return PostProcessor.is(document, OutputFormat.PDF)
        && context.requestedFormat() == OutputFormat.PDF
        && context.options().signature() != null;
  }

  @Override
  public RenderedDocument process(RenderedDocument document, PostProcessContext context) {
    SignatureSettings settings = context.options().signature();
    Signer signer = signer(settings);
    try (PDDocument pdf = Loader.loadPDF(document.content());
        SignatureOptions options = new SignatureOptions()) {
      PDSignature signature = new PDSignature();
      signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
      signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
      signature.setName(commonName(signer.certificate()));
      signature.setReason(settings.reason());
      signature.setLocation(settings.location());
      signature.setContactInfo(settings.contact());
      signature.setSignDate(Calendar.getInstance());
      options.setPreferredSignatureSize(SIGNATURE_SIZE);
      pdf.addSignature(signature, signer::sign, options);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      pdf.saveIncremental(out);
      return new RenderedDocument(out.toByteArray(), OutputFormat.PDF, document.pageCount());
    } catch (IOException e) {
      throw new RenderException(
          "The PDF could not be signed (" + e.getClass().getSimpleName() + ").", null);
    }
  }

  private static Signer signer(SignatureSettings settings) {
    char[] password = settings.passwordCopy();
    try {
      KeyStore store = KeyStore.getInstance("PKCS12");
      store.load(new ByteArrayInputStream(settings.keyStoreCopy()), password);
      String alias = settings.alias();
      if (alias == null) {
        alias =
            Collections.list(store.aliases()).stream()
                .filter(a -> isKey(store, a))
                .findFirst()
                .orElseThrow(() -> invalid("the key store contains no private key"));
      }
      if (!store.isKeyEntry(alias)) {
        throw invalid("the key store has no private key with the given alias");
      }
      PrivateKey key = (PrivateKey) store.getKey(alias, password);
      Certificate[] chain = store.getCertificateChain(alias);
      if (chain == null
          || chain.length == 0
          || !(chain[0] instanceof X509Certificate certificate)) {
        throw invalid("the key has no X.509 certificate");
      }
      return new Signer(key, certificate, Arrays.asList(chain));
    } catch (IOException | GeneralSecurityException e) {
      throw invalid("cannot open the PKCS#12 key store (wrong password or format)");
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  private static boolean isKey(KeyStore store, String alias) {
    try {
      return store.isKeyEntry(alias);
    } catch (GeneralSecurityException e) {
      return false;
    }
  }

  private static InvalidOptionsException invalid(String detail) {
    return new InvalidOptionsException(
        InvalidOptionsException.SIGNATURE_INVALID, "Cannot sign the PDF: " + detail + ".");
  }

  private static String commonName(X509Certificate certificate) {
    X500Name name = new X500Name(certificate.getSubjectX500Principal().getName());
    RDN[] cn = name.getRDNs(BCStyle.CN);
    return cn.length == 0 ? name.toString() : IETFUtils.valueToString(cn[0].getFirst().getValue());
  }

  /** Creates the CMS signature of the signed byte ranges. */
  private record Signer(PrivateKey key, X509Certificate certificate, List<Certificate> chain) {
    byte[] sign(InputStream content) throws IOException {
      try {
        String algorithm =
            "EC".equals(key.getAlgorithm()) ? "SHA256withECDSA" : "SHA256with" + key.getAlgorithm();
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        generator.addSignerInfoGenerator(
            new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                .build(new JcaContentSignerBuilder(algorithm).build(key), certificate));
        generator.addCertificates(new JcaCertStore(chain));
        return generator
            .generate(new CMSProcessableByteArray(content.readAllBytes()), false)
            .getEncoded();
      } catch (OperatorCreationException | CMSException | GeneralSecurityException e) {
        throw new IOException("signature failed: " + e.getClass().getSimpleName(), e);
      }
    }
  }
}
