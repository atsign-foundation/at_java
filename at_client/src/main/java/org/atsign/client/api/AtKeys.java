package org.atsign.client.api;

import java.security.Key;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Builder;
import lombok.Value;
import org.atsign.client.impl.common.EnrollmentId;

/**
 * An immutable class used to hold an {@link org.atsign.client.api.AtClient}s keys.
 * <p>
 * Examples:
 *
 * <pre>
 * AtKeys keys = AtKeys.builder()
 *     .selfEncryptKey(generateAESKeyBase64())
 *     .apkamKeyPair(generateRSAKeyPair())
 *     .apkamSymmetricKey(generateAESKeyBase64())
 *     .build();
 *
 * keys = keys.toBuilder().enrollmentId(enrollmentId).build();
 * </pre>
 */
@Value
@Builder(toBuilder = true)
public class AtKeys {

  /**
   * unique id which is assigned by the at_server at enrollment time, this is associated with a
   * specific application and device and therefore a specific the apkam key pair
   */
  EnrollmentId enrollmentId;

  /**
   * Public Key used for Authentication Management, once this is stored in the at_server then an
   * {@link org.atsign.client.api.AtClient} is able to authenticate using the corresponding private
   * key.
   */
  String apkamPublicKey;

  /**
   * Private Key used for Authentication Management, this is used to sign the challenge during
   * authentication with the at_server.
   */
  String apkamPrivateKey;

  /**
   * Encryption Key used during enrollment. This key is sent as part of the enrollment request
   * (encrypted with the atsigns public encryption key). The process that approves the enrollment
   * request uses this key to encrypt the {@link #selfEncryptKey} and {@link #encryptPrivateKey}
   * in the response that it sends.
   * The process which requested the enrollment can then decrypt and store those keys.
   * This ensures that all {@link AtKeys} got and {@link AtSign} share the same
   * {@link #selfEncryptKey} and {@link #encryptPrivateKey}
   */
  String apkamSymmetricKey;

  /**
   * Encryption Key used to encrypt {@link Keys.SelfKey}s and the pkam and
   * encryption key pairs
   * when they are externalised as JSON
   */
  String selfEncryptKey;

  /**
   * This is used to encrypt the symmetric keys that are used to encrypt
   * {@link Keys.SharedKey}s
   * where the shared with {@link AtSign} is this {@link AtSign}
   */

  String encryptPublicKey;

  /**
   * This is used to decrypt the symmetric keys that are used to encrypt
   * {@link Keys.SharedKey}s
   * where the shared with {@link AtSign} is this {@link AtSign}
   */
  String encryptPrivateKey;

  /**
   * Transient cache of other keys
   */
  Map<String, String> cache = new ConcurrentHashMap<>();

  /**
   * @return true if these {link @AtKeys} have enrollment id
   */
  public boolean hasEnrollmentId() {
    return enrollmentId != null;
  }

  /**
   * @return true if these {link @AtKeys} have APKAM private key set
   */
  public boolean hasPkamKey() {
    return this.apkamPrivateKey != null;
  }

  /**
   * Allows the retrieval of previously put key values from the transient cache.
   *
   * @param key for the cached value
   * @return cached value or null if no entry
   */
  public String get(String key) {
    return cache.get(key);
  }

  /**
   * Allows the storage key values in the transient cache.
   *
   * @param key for the cached value
   * @param value the cached value
   */
  public void put(String key, String value) {
    cache.put(key, value);
  }

  /**
   * @return an immutable map of all the transient cached values
   */
  public Map<String, String> getCache() {
    return Collections.unmodifiableMap(cache);
  }

  /**
   * Builder utility.
   */
  public static class AtKeysBuilder {

    public AtKeysBuilder encryptKeyPair(KeyPair keyPair) {
      return this.encryptPublicKey(createStringBase64(keyPair.getPublic()))
          .encryptPrivateKey(createStringBase64(keyPair.getPrivate()));
    }

    public AtKeysBuilder apkamKeyPair(KeyPair keyPair) {
      return this.apkamPublicKey(createStringBase64(keyPair.getPublic()))
          .apkamPrivateKey(createStringBase64(keyPair.getPrivate()));
    }
  }

  private static String createStringBase64(Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

}
