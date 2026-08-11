package org.atsign.client.api;

import lombok.*;
import org.atsign.client.api.CryptographicMaterial.Algorithm;
import org.atsign.client.api.CryptographicMaterial.BytesAsBase64;
import org.atsign.client.api.CryptographicMaterial.Role;
import org.atsign.client.api.CryptographicMaterial.Status;

import java.security.KeyPair;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.atsign.client.impl.util.EncryptionUtils.toStringBase64;
import static org.atsign.client.impl.util.KeysUtils.SYMMETRIC_KEY_WRAPPING;

/**
 * An immutable class used to hold an {@link org.atsign.client.api.AtClient}s keys.
 * These are use for authentication and encryption.
 */
@Value
@Builder
public class AtKeys {

  /**
   * Reserved key id for the material that a keyfile carried as flat fields before key material
   * became self-describing. They are internal to this class: the flat fields are still read and
   * written under their own names, so these ids are never serialised.
   */
  private static final KeyId LEGACY = KeyId.of("_legacy");


  /**
   * The flat fields record no creation time, so their material carries this rather than a clock
   * reading, which would make two otherwise equal {@link AtKeys} unequal.
   */
  private static final OffsetDateTime UNKNOWN_CREATED_AT = OffsetDateTime.parse("1970-01-01T00:00:00Z");

  /**
   * The atSign these keys belong to. Nullable for the moment: a keyfile written before the atSign
   * was recorded does not carry one.
   */
  AtSign atSign;

  /**
   * Key material by key id, then by role. Not exposed: callers reach it through
   * {@link #getCryptographicMaterial} and
   * {@link #getAllCryptographicMaterial}, so the shape stays free to change.
   */
  @Getter(AccessLevel.NONE)
  Map<KeyId, Map<Role, CryptographicMaterial>> materials;

  /**
   * Transient cache of other keys. Not part of the identity of these keys, so it takes no part in
   * equality and is kept out of toString().
   */
  @Builder.Default
  @Getter(AccessLevel.NONE)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  Map<String, String> cache = new ConcurrentHashMap<>();

  /**
   * Written rather than generated so that the material and the cache are copied on the way out.
   * Sharing them would let a derived builder write into this instance, which for a value type would
   * be a surprise. <b>Every field has to appear here</b>: one left out is silently dropped by every
   * copy, which is what {@code testToBuilderCarriesEveryField} exists to catch.
   *
   * @return a builder holding a copy of this instance's state
   */
  public AtKeysBuilder toBuilder() {
    Map<KeyId, Map<Role, CryptographicMaterial>> materialsCopy = new LinkedHashMap<>();
    materials.forEach((keyId, byRole) -> materialsCopy.put(keyId, new LinkedHashMap<>(byRole)));
    ConcurrentHashMap<String, String> cacheCopy = new ConcurrentHashMap<>(cache);
    return new AtKeysBuilder()
        .atSign(atSign)
        .materials(materialsCopy)
        .cache(cacheCopy);
  }

  /**
   * @return true if these {link @AtKeys} have enrollment id
   */
  public boolean hasEnrollmentId() {
    return getEnrollmentId() != null;
  }

  /**
   * @return true if these {link @AtKeys} have APKAM private key set
   */
  public boolean hasPkamKey() {
    return getApkamPrivateKey() != null;
  }

  /**
   * @param keyId identifies the key
   * @param role the part that key plays
   * @return the material, or null if these keys hold no such material
   */
  public CryptographicMaterial getCryptographicMaterial(KeyId keyId, Role role) {
    return getCryptographicMaterial(m -> m.getKeyId().equals(keyId) && m.getRole().equals(role));
  }

  /**
   * @return every material these keys hold (legacy material is excluded)
   */
  public Collection<CryptographicMaterial> getAllCryptographicMaterial() {
    return getAllCryptographicMaterial(m -> !m.getKeyId().equals(LEGACY));
  }

  /**
   * @param keyId identifies the key
   * @return every material sharing that key id, for example both halves of a keypair
   */
  public Collection<CryptographicMaterial> getAllCryptographicMaterial(KeyId keyId) {
    return getAllCryptographicMaterial(m -> m.getKeyId().equals(keyId));
  }

  /**
   * @param enrollmentId identifies the enrollment
   * @return every material belonging to that enrollment
   */
  public Collection<CryptographicMaterial> getAllCryptographicMaterial(EnrollmentId enrollmentId) {
    // enrollmentId first: material that belongs to no enrollment has none to compare.
    return getAllCryptographicMaterial(m -> enrollmentId.equals(m.getEnrollmentId()));
  }

  /**
   * @param predicate filter
   * @return every material that matches the filter
   */
  public Collection<CryptographicMaterial> getAllCryptographicMaterial(Predicate<CryptographicMaterial> predicate) {
    ArrayList<CryptographicMaterial> result = new ArrayList<>();
    for (Map<Role, CryptographicMaterial> map : materials.values()) {
      for (CryptographicMaterial material : map.values()) {
        if (predicate.test(material)) {
          result.add(material);
        }
      }
    }
    return result;
  }

  public CryptographicMaterial getCryptographicMaterial(Predicate<CryptographicMaterial> predicate) {
    Collection<CryptographicMaterial> result = getAllCryptographicMaterial(predicate);
    if (result.size() == 1) {
      return result.iterator().next();
    } else if (result.isEmpty()) {
      return null;
    } else {
      throw new IllegalArgumentException("predicate matches " + result.size() + " materials");
    }
  }

  /**
   * @param material the material to add
   * @return a copy of these keys holding {@code material} as well
   * @throws IllegalArgumentException if the material duplicates a role already held under the same
   *         key id, disagrees with the enrollment id of that key id, or gives an enrollment a second
   *         material of the same role
   */
  public AtKeys withKey(CryptographicMaterial material) {
    validateNewKey(material);
    return toBuilder().material(material).build();
  }

  /**
   * @param keyId identifies the key to retire
   * @return a copy of these keys with every material of {@code keyId} marked
   *         {@link Status#retired}
   * @throws IllegalArgumentException if these keys hold no such key id
   */
  public AtKeys withRetiredKey(KeyId keyId) {
    return withRetiredKey(keyId, Status.retired);
  }

  /**
   * Material is never removed: retired and dead material is still needed to decrypt what it
   * protected, so this is the delete operation. Status only moves forward, and a call that would
   * leave it where it is does nothing.
   *
   * @param keyId identifies the key to retire
   * @param to the status to move to
   * @return a copy of these keys with every material of {@code keyId} marked {@code to}
   * @throws IllegalArgumentException if {@code to} is {@link Status#active}, if these keys hold no
   *         such key id, or if any material of that key id is already past {@code to}
   */
  public AtKeys withRetiredKey(KeyId keyId, Status to) {
    return toBuilder().retire(keyId, to).build();
  }

  private static CryptographicMaterial transitionStatus(CryptographicMaterial material, Status status) {
    if (material.getStatus().ordinal() > status.ordinal()) {
      String warning = String.format("cannot move keyId %s backward from %s to %s",
                                     material.getKeyId(), material.getStatus(), status);
      throw new IllegalArgumentException(warning);
    }
    return material.withStatus(status);
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
   * @return the base64 APKAM public key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public String getApkamPublicKey() {
    return lookupBytes(LEGACY, Role.publicVerification);
  }

  /**
   * @return the base64 APKAM private key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public EnrollmentId getEnrollmentId() {
    // The public half rather than the private one: an authMode of sim or another secure element
    // leaves the private key out of the keyfile entirely.
    CryptographicMaterial material = getCryptographicMaterial(LEGACY, Role.publicVerification);
    return material == null ? null : material.getEnrollmentId();
  }

  /**
   * @return the base64 APKAM private key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public String getApkamPrivateKey() {
    return lookupBytes(LEGACY, Role.privateSigning);
  }

  /**
   * @return the base64 APKAM symmetric key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public String getApkamSymmetricKey() {
    return lookupBytes(LEGACY, SYMMETRIC_KEY_WRAPPING);
  }

  /**
   * @return the base64 self encryption key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public String getSelfEncryptKey() {
    return lookupBytes(LEGACY, Role.symmetricEncryption);
  }

  /**
   * @return the base64 encryption public key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public String getEncryptPublicKey() {
    return lookupBytes(LEGACY, Role.publicEncryption);
  }

  /**
   * @return the base64 encryption private key, or null
   * @deprecated read the material through {@link #getCryptographicMaterial} instead
   */
  @Deprecated
  public String getEncryptPrivateKey() {
    return lookupBytes(LEGACY, Role.privateDecryption);
  }

  private String lookupBytes(KeyId keyId, Role role) {
    CryptographicMaterial material = getCryptographicMaterial(keyId, role);
    return material == null ? null : material.getBytes().base64();
  }

  private void validateNewKey(CryptographicMaterial candidate) {
    for (CryptographicMaterial held : getAllCryptographicMaterial()) {
      if (held.getKeyId().equals(candidate.getKeyId())) {
        if (held.getRole().equals(candidate.getRole())) {
          throw new IllegalArgumentException(
              "AtKeys already holds a " + candidate.getRole() + " material for " + candidate.getKeyId());
        }
        if (!java.util.Objects.equals(held.getEnrollmentId(), candidate.getEnrollmentId())) {
          throw new IllegalArgumentException("enrollmentId " + candidate.getEnrollmentId()
              + " does not match " + held.getEnrollmentId() + " already on " + candidate.getKeyId());
        }
      } else if (candidate.getEnrollmentId() != null
          && candidate.getEnrollmentId().equals(held.getEnrollmentId())
          && held.getRole().equals(candidate.getRole())) {
        throw new IllegalArgumentException("enrollment " + candidate.getEnrollmentId()
            + " already has a " + candidate.getRole() + " material");
      }
    }
  }

  /**
   * A builder for instantiating {@link AtKeys}.
   *
   * <pre>
   *
   * AtKeys keys = AtKeys.builder()
   *     .selfEncryptKey(EncryptionUtil.generateAESKeyBase64())
   *     .apkamKeyPair(EncryptionUtil.generateRSAKeyPair())
   *     .apkamSymmetricKey(EncryptionUtil.generateAESKeyBase64())
   *     .build();
   * </pre>
   *
   * <b>NOTE</b> {@link AtKeys} are immutable, so use the toBuilder() method
   * to create a modified instance.
   *
   * <pre>
   *
   * AtKeys newKeys = keys.toBuilder().enrollmentId(enrollmentId).build();
   * </pre>
   */
  public static class AtKeysBuilder {

    /**
     * Declared here rather than left to Lombok so that it starts empty and is never null. Every
     * build() therefore hands AtKeys a real map, and nothing has to test for one.
     */
    private Map<KeyId, Map<Role, CryptographicMaterial>> materials = new LinkedHashMap<>();

    public AtKeysBuilder atSign(AtSign atSign) {
      this.atSign = atSign;
      return this;
    }

    /**
     * @param atSign the atSign these keys belong to
     * @return this builder
     */
    public AtKeysBuilder atSign(String atSign) {
      return atSign(AtSign.of(atSign));
    }

    @Deprecated
    public AtKeysBuilder enrollmentId(EnrollmentId enrollmentId) {
      Map<Role, CryptographicMaterial> legacy = materials.get(LEGACY);
      if (legacy == null || legacy.isEmpty()) {
        throw new IllegalArgumentException("attempt to set enrollment id but no legacy material");
      }
      legacy.replaceAll((role, material) -> material.toBuilder().enrollmentId(enrollmentId).build());
      return this;
    }

    /**
     * Moves every material of {@code keyId} to {@code to}. Material is never removed, so this is the
     * delete operation; status only ever moves forward.
     *
     * @param keyId identifies the key to retire
     * @param to the status to move to
     * @return this builder
     * @throws IllegalArgumentException if {@code to} is {@link Status#active}, this builder holds no
     *         such key id, or any material of that key id is already past {@code to}
     */
    public AtKeysBuilder retire(KeyId keyId, Status to) {
      if (to == Status.active) {
        throw new IllegalArgumentException("cannot retire a key to active: " + keyId);
      }
      Map<Role, CryptographicMaterial> byRole = materials.get(keyId);
      if (byRole == null || byRole.isEmpty()) {
        throw new IllegalArgumentException("unrecognised keyId: " + keyId);
      }
      byRole.replaceAll((role, material) -> transitionStatus(material, to));
      return this;
    }

    /**
     * @param enrollmentId the enrollment the legacy keys were issued for
     * @return this builder
     */
    public AtKeysBuilder enrollmentId(String enrollmentId) {
      return enrollmentId(EnrollmentId.of(enrollmentId));
    }

    /**
     * Adds key material, replacing whatever this builder held for the same key id and role.
     *
     * @param material the material to hold
     * @return this builder
     */
    public AtKeysBuilder material(CryptographicMaterial material) {
      materials.computeIfAbsent(material.getKeyId(), keyId -> new LinkedHashMap<>())
          .put(material.getRole(), material);
      return this;
    }

    /**
     * Hidden so that the shape of the material map stays an implementation detail; it exists only so
     * that {@link AtKeys#toBuilder()} has somewhere to put its copy.
     */
    private AtKeysBuilder materials(Map<KeyId, Map<Role, CryptographicMaterial>> materials) {
      this.materials = materials;
      return this;
    }

    /**
     * @param keyPair the encryption keypair
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder encryptKeyPair(KeyPair keyPair) {
      return this.encryptPublicKey(toStringBase64(keyPair.getPublic()))
          .encryptPrivateKey(toStringBase64(keyPair.getPrivate()));
    }

    /**
     * @param keyPair the APKAM keypair
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder apkamKeyPair(KeyPair keyPair) {
      return this.apkamPublicKey(toStringBase64(keyPair.getPublic()))
          .apkamPrivateKey(toStringBase64(keyPair.getPrivate()));
    }

    /**
     * @param base64 the base64 APKAM public key, or null to hold none
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder apkamPublicKey(String base64) {
      return legacyMaterial(LEGACY, Role.publicVerification, Algorithm.rsa2048, base64);
    }

    /**
     * @param base64 the base64 APKAM private key, or null to hold none
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder apkamPrivateKey(String base64) {
      return legacyMaterial(LEGACY, Role.privateSigning, Algorithm.rsa2048, base64);
    }

    /**
     * @param base64 the base64 APKAM symmetric key, or null to hold none
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder apkamSymmetricKey(String base64) {
      return legacyMaterial(LEGACY, SYMMETRIC_KEY_WRAPPING, Algorithm.aes256, base64);
    }

    /**
     * @param base64 the base64 self encryption key, or null to hold none
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder selfEncryptKey(String base64) {
      return legacyMaterial(LEGACY, Role.symmetricEncryption, Algorithm.aes256, base64);
    }

    /**
     * @param base64 the base64 encryption public key, or null to hold none
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder encryptPublicKey(String base64) {
      return legacyMaterial(LEGACY, Role.publicEncryption, Algorithm.rsa2048, base64);
    }

    /**
     * @param base64 the base64 encryption private key, or null to hold none
     * @return this builder
     * @deprecated add the material with {@link #material} instead
     */
    @Deprecated
    public AtKeysBuilder encryptPrivateKey(String base64) {
      return legacyMaterial(LEGACY, Role.privateDecryption, Algorithm.rsa2048, base64);
    }

    private AtKeysBuilder legacyMaterial(KeyId keyId, Role role, Algorithm algorithm, String base64) {
      BytesAsBase64 bytes = BytesAsBase64.of(base64);
      if (bytes == null) {
        return this;
      }
      return material(CryptographicMaterial.builder()
          .keyId(keyId)
          .role(role)
          .algorithm(algorithm)
          .bytes(bytes)
          .createdAt(UNKNOWN_CREATED_AT)
          .build());
    }
  }

}
