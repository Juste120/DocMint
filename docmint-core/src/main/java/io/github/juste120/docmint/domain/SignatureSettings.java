package io.github.juste120.docmint.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * How to sign PDFs electronically (module {@code docmint-signature}): a PKCS#12 key store ({@code
 * .p12} / {@code .pfx}) holding the private key and certificate chain.
 *
 * <pre>{@code
 * SignatureSettings.pkcs12(Files.readAllBytes(path), password)
 *     .withReason("Attestation officielle")
 *     .withLocation("Lomé, Togo");
 * }</pre>
 *
 * <p>The password is kept as a {@code char[]} copy, never shown by {@link #toString()}, and can be
 * wiped with {@link #clear()}.
 */
public final class SignatureSettings {

  private final byte[] keyStore;
  private final char[] password;
  private final String alias;
  private final String reason;
  private final String location;
  private final String contact;

  private SignatureSettings(
      byte[] keyStore,
      char[] password,
      String alias,
      String reason,
      String location,
      String contact) {
    this.keyStore = Objects.requireNonNull(keyStore, "keyStore").clone();
    this.password = Objects.requireNonNull(password, "password").clone();
    this.alias = alias;
    this.reason = reason;
    this.location = location;
    this.contact = contact;
  }

  /**
   * Creates settings from a PKCS#12 key store.
   *
   * @param keyStore the content of the {@code .p12} file
   * @param password its password
   * @return the settings (first key entry, no reason or location)
   * @throws IllegalArgumentException if the key store is empty
   */
  public static SignatureSettings pkcs12(byte[] keyStore, char[] password) {
    if (keyStore == null || keyStore.length == 0) {
      throw new IllegalArgumentException("the signature key store is empty");
    }
    return new SignatureSettings(keyStore, password, null, null, null, null);
  }

  /**
   * Returns a copy using a specific key entry.
   *
   * @param keyAlias the alias in the key store
   * @return the new settings
   */
  public SignatureSettings withAlias(String keyAlias) {
    return new SignatureSettings(keyStore, password, keyAlias, reason, location, contact);
  }

  /**
   * Returns a copy with the reason shown by PDF readers.
   *
   * @param text e.g. {@code Attestation officielle}
   * @return the new settings
   */
  public SignatureSettings withReason(String text) {
    return new SignatureSettings(keyStore, password, alias, text, location, contact);
  }

  /**
   * Returns a copy with the signing location.
   *
   * @param text e.g. {@code Lomé, Togo}
   * @return the new settings
   */
  public SignatureSettings withLocation(String text) {
    return new SignatureSettings(keyStore, password, alias, reason, text, contact);
  }

  /**
   * Returns a copy with the signer's contact information.
   *
   * @param text e.g. an email address
   * @return the new settings
   */
  public SignatureSettings withContact(String text) {
    return new SignatureSettings(keyStore, password, alias, reason, location, text);
  }

  /**
   * Returns a copy of the key store content.
   *
   * @return the bytes
   */
  public byte[] keyStoreCopy() {
    return keyStore.clone();
  }

  /**
   * Returns a copy of the password.
   *
   * @return the password
   */
  public char[] passwordCopy() {
    return password.clone();
  }

  /**
   * Returns the key alias, or {@code null} for the first key entry.
   *
   * @return the alias
   */
  public String alias() {
    return alias;
  }

  /**
   * Returns the reason, or {@code null}.
   *
   * @return the reason
   */
  public String reason() {
    return reason;
  }

  /**
   * Returns the location, or {@code null}.
   *
   * @return the location
   */
  public String location() {
    return location;
  }

  /**
   * Returns the contact information, or {@code null}.
   *
   * @return the contact
   */
  public String contact() {
    return contact;
  }

  /** Overwrites the password and the key store held by this object. */
  public void clear() {
    Arrays.fill(password, '\0');
    Arrays.fill(keyStore, (byte) 0);
  }

  @Override
  public String toString() {
    return "SignatureSettings[keyStore=***, password=***, alias=" + alias + "]";
  }
}
