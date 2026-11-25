package io.github.juste120.docmint.demo.config;

import io.github.juste120.docmint.domain.SignatureSettings;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DEMO ONLY: creates a self-signed certificate at startup so that signed documents can be shown
 * without any setup. In a real application, use a certificate from a certification authority and
 * the properties {@code docmint.signature.key-store} and {@code docmint.signature.password}.
 */
@Configuration
public class DemoSigningConfig {

  @Bean
  SignatureSettings demoSignature() throws Exception {
    char[] password = new char[24];
    SecureRandom random = new SecureRandom();
    String chars = Base64.getEncoder().encodeToString(random.generateSeed(24));
    chars.getChars(0, 24, password, 0);
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    X500Name name = new X500Name("CN=Quincaillerie du Golfe (démo), O=DocMint demo, C=TG");
    Date now = new Date();
    Certificate certificate =
        new JcaX509CertificateConverter()
            .getCertificate(
                new JcaX509v3CertificateBuilder(
                        name,
                        BigInteger.valueOf(now.getTime()),
                        now,
                        new Date(now.getTime() + 365L * 86_400_000L),
                        name,
                        pair.getPublic())
                    .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
    KeyStore store = KeyStore.getInstance("PKCS12");
    store.load(null, null);
    store.setKeyEntry("demo", pair.getPrivate(), password, new Certificate[] {certificate});
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    store.store(out, password);
    return SignatureSettings.pkcs12(out.toByteArray(), password)
        .withReason("Attestation officielle (démonstration)")
        .withLocation("Lomé, Togo");
  }
}
