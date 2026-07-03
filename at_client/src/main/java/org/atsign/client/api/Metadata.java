package org.atsign.client.api;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.atsign.client.impl.util.JsonUtils;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * Value class which models key metadata in the Atsign Platform
 */
@Value
@Builder(toBuilder = true)
@Accessors(fluent = true)
@Jacksonized
public class Metadata {

  Long ttl;
  Long ttb;
  Long ttr;
  Boolean ccd;
  String createdBy;
  String updatedBy;
  OffsetDateTime availableAt;
  OffsetDateTime expiresAt;
  OffsetDateTime refreshAt;
  OffsetDateTime createdAt;
  OffsetDateTime updatedAt;
  String status;
  Integer version;
  String dataSignature;
  String sharedKeyStatus;
  Boolean isPublic;
  Boolean isEncrypted;
  Boolean isHidden;
  Boolean namespaceAware;
  Boolean isBinary;
  Boolean isCached;
  String sharedKeyEnc;
  String pubKeyCS;
  PublicKeyHash pubKeyHash;
  String encoding;
  String encKeyName;
  String encAlgo;
  String ivNonce;
  String skeEncKeyName;
  String skeEncAlgo;
  Boolean immutable;
  /**
   * Provider-owned crypto metadata for the pluggable encryption/decryption model. Mirrors the
   * canonical at_commons {@code AppMetadata}: an SDK-owned {@code providerId} that routes the
   * value to the crypto provider able to decrypt it, plus opaque {@code additional} entries the
   * SDK preserves but does not interpret. Serialised LAST in the metadata fragment as
   * {@code :appMetadata:<base64(JSON)>} and as a flat JSON object in metadata maps.
   */
  AppMetadata appMetadata;

  /**
   * A builder for instantiating {@link Metadata} instances. Note: Metadata is immutable so if you
   * want to create a modified instance then use the toBuilder() method, override the fields and
   * invoke build().
   */
  public static class MetadataBuilder {
    // required for javadoc
  };

  public static Metadata fromJson(String json) {
    return JsonUtils.readValue(json, Metadata.class);
  }

  /**
   * Ordering is crucial see at_commons\lib\src\verb\syntax.dart
   *
   * @return the encoded metadata fields as recognized by an At Server in an update command.
   */
  @Override
  public String toString() {
    return new StringBuilder()
        .append(ttl != null ? ":ttl:" + ttl : "")
        .append(ttb != null ? ":ttb:" + ttb : "")
        .append(ttr != null ? ":ttr:" + ttr : "")
        .append(ccd != null ? ":ccd:" + ccd : "")
        .append(dataSignature != null ? ":dataSignature:" + dataSignature : "")
        .append(sharedKeyStatus != null ? ":sharedKeyStatus:" + sharedKeyStatus : "")
        .append(isBinary != null ? ":isBinary:" + isBinary : "")
        .append(isEncrypted != null ? ":isEncrypted:" + isEncrypted : "")
        .append(sharedKeyEnc != null ? ":sharedKeyEnc:" + sharedKeyEnc : "")
        .append(pubKeyCS != null ? ":pubKeyCS:" + pubKeyCS : "")
        .append(pubKeyHash != null ? ":pubKeyHash:" + pubKeyHash.hash + ":hashingAlgo:" + pubKeyHash.hashingAlgo : "")
        .append(encoding != null ? ":encoding:" + encoding : "")
        .append(encKeyName != null ? ":encKeyName:" + encKeyName : "")
        .append(encAlgo != null ? ":encAlgo:" + encAlgo : "")
        .append(ivNonce != null ? ":ivNonce:" + ivNonce : "")
        .append(skeEncKeyName != null ? ":skeEncKeyName:" + skeEncKeyName : "")
        .append(skeEncAlgo != null ? ":skeEncAlgo:" + skeEncAlgo : "")
        .append(immutable != null ? ":immutable:" + immutable : "")
        .append(appMetadata != null ? ":appMetadata:" + appMetadata.encode() : "")
        .toString();
  }

  /**
   * Combines two metadata instances into a new metadata instance
   *
   * @param md1 has priority
   * @param md2 use fields from here if not in md1
   * @return A new merged metadata instance
   */
  public static Metadata merge(Metadata md1, Metadata md2) {
    return toMergedBuilder(md1, md2).build();
  }

  /**
   * Creates a builder for {@link Metadata} based on the combined fields of two
   * Metadata instances.
   *
   * @param md1 has priority
   * @param md2 use fields from here if not in md1
   * @return a builder with the squashed fields
   */
  public static MetadataBuilder toMergedBuilder(Metadata md1, Metadata md2) {
    MetadataBuilder builder = new MetadataBuilder();

    if (!setIsPublicIfNotNull(builder, md1.isPublic)) {
      setIsPublicIfNotNull(builder, md2.isPublic);
    }
    if (!setIsHiddenIfNotNull(builder, md1.isHidden)) {
      setIsHiddenIfNotNull(builder, md2.isHidden);
    }
    if (!setIsCachedIfNotNull(builder, md1.isCached)) {
      setIsCachedIfNotNull(builder, md2.isCached);
    }
    if (!setTtlIfNotNull(builder, md1.ttl)) {
      setTtlIfNotNull(builder, md2.ttl);
    }
    if (!setTtbIfNotNull(builder, md1.ttb)) {
      setTtbIfNotNull(builder, md2.ttb);
    }
    if (!setTtrIfNotNull(builder, md1.ttr)) {
      setTtrIfNotNull(builder, md2.ttr);
    }
    if (!setCcdIfNotNull(builder, md1.ccd)) {
      setCcdIfNotNull(builder, md2.ccd);
    }
    if (!setAvailableAtIfNotNull(builder, md1.availableAt)) {
      setAvailableAtIfNotNull(builder, md2.availableAt);
    }
    if (!setExpiresAtIfNotNull(builder, md1.expiresAt)) {
      setExpiresAtIfNotNull(builder, md2.expiresAt);
    }
    if (!setRefreshAtIfNotNull(builder, md1.refreshAt)) {
      setRefreshAtIfNotNull(builder, md2.refreshAt);
    }
    if (!setCreatedAtIfNotNull(builder, md1.createdAt)) {
      setCreatedAtIfNotNull(builder, md2.createdAt);
    }
    if (!setUpdatedAtIfNotNull(builder, md1.updatedAt)) {
      setUpdatedAtIfNotNull(builder, md2.updatedAt);
    }
    if (!setDataSignatureIfNotNull(builder, md1.dataSignature)) {
      setDataSignatureIfNotNull(builder, md2.dataSignature);
    }
    if (!setSharedKeyStatusIfNotNull(builder, md1.sharedKeyStatus)) {
      setSharedKeyStatusIfNotNull(builder, md2.sharedKeyStatus);
    }
    if (!setSharedKeyEncIfNotNull(builder, md1.sharedKeyEnc)) {
      setSharedKeyEncIfNotNull(builder, md2.sharedKeyEnc);
    }
    if (!setIsEncryptedIfNotNull(builder, md1.isEncrypted)) {
      setIsEncryptedIfNotNull(builder, md2.isEncrypted);
    }
    if (!setNamespaceAwareIfNotNull(builder, md1.namespaceAware)) {
      setNamespaceAwareIfNotNull(builder, md2.namespaceAware);
    }
    if (!setIsBinaryIfNotNull(builder, md1.isBinary)) {
      setIsBinaryIfNotNull(builder, md2.isBinary);
    }
    if (!setPubKeyCSIfNotNull(builder, md1.pubKeyCS)) {
      setPubKeyCSIfNotNull(builder, md2.pubKeyCS);
    }
    if (!setPubKeyHashIfNotNull(builder, md1.pubKeyHash)) {
      setPubKeyHashIfNotNull(builder, md2.pubKeyHash);
    }
    if (!setEncodingIfNotNull(builder, md1.encoding)) {
      setEncodingIfNotNull(builder, md2.encoding);
    }
    if (!setEncKeyNameIfNotNull(builder, md1.encKeyName)) {
      setEncKeyNameIfNotNull(builder, md2.encKeyName);
    }
    if (!setEncAlgoIfNotNull(builder, md1.encAlgo)) {
      setEncAlgoIfNotNull(builder, md2.encAlgo);
    }
    if (!setIvNonceIfNotNull(builder, md1.ivNonce)) {
      setIvNonceIfNotNull(builder, md2.ivNonce);
    }
    if (!setSkeEncKeyNameIfNotNull(builder, md1.skeEncKeyName)) {
      setSkeEncKeyNameIfNotNull(builder, md2.skeEncKeyName);
    }
    if (!setSkeEncAlgoIfNotNull(builder, md1.skeEncAlgo)) {
      setSkeEncAlgoIfNotNull(builder, md2.skeEncAlgo);
    }
    if (!setImmutableIfNotNull(builder, md1.immutable)) {
      setImmutableIfNotNull(builder, md2.immutable);
    }

    return builder;
  }

  public static boolean setIsHiddenIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.isHidden(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setIsPublicIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.isPublic(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setIsCachedIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.isCached(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setTtlIfNotNull(MetadataBuilder builder, Long value) {
    if (value != null) {
      builder.ttl(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setTtbIfNotNull(MetadataBuilder builder, Long value) {
    if (value != null) {
      builder.ttb(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setTtrIfNotNull(MetadataBuilder builder, Long value) {
    if (value != null) {
      builder.ttr(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setCcdIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.ccd(value);
      return true;
    } else {
      return false;
    }
  }

  private static boolean setAvailableAtIfNotNull(MetadataBuilder builder, OffsetDateTime value) {
    if (value != null) {
      builder.availableAt(value);
      return true;
    } else {
      return false;
    }
  }

  private static boolean setExpiresAtIfNotNull(MetadataBuilder builder, OffsetDateTime value) {
    if (value != null) {
      builder.expiresAt(value);
      return true;
    } else {
      return false;
    }
  }

  private static boolean setRefreshAtIfNotNull(MetadataBuilder builder, OffsetDateTime value) {
    if (value != null) {
      builder.refreshAt(value);
      return true;
    } else {
      return false;
    }
  }

  private static boolean setCreatedAtIfNotNull(MetadataBuilder builder, OffsetDateTime value) {
    if (value != null) {
      builder.createdAt(value);
      return true;
    } else {
      return false;
    }
  }

  private static boolean setUpdatedAtIfNotNull(MetadataBuilder builder, OffsetDateTime value) {
    if (value != null) {
      builder.updatedAt(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setIsBinaryIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.isBinary(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setIsEncryptedIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.isEncrypted(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setDataSignatureIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.dataSignature(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setNamespaceAwareIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.namespaceAware(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setSharedKeyStatusIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.sharedKeyStatus(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setSharedKeyEncIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.sharedKeyEnc(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setPubKeyCSIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.pubKeyCS(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setPubKeyHashIfNotNull(MetadataBuilder builder, PublicKeyHash value) {
    if (value != null) {
      builder.pubKeyHash(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setEncodingIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.encoding(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setEncKeyNameIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.encKeyName(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setEncAlgoIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.encAlgo(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setIvNonceIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.ivNonce(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setSkeEncKeyNameIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.skeEncKeyName(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setSkeEncAlgoIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.skeEncAlgo(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean setImmutableIfNotNull(MetadataBuilder builder, Boolean value) {
    if (value != null) {
      builder.immutable(value);
      return true;
    } else {
      return false;
    }
  }

  public static boolean isBinary(Metadata metadata) {
    return metadata.isBinary() != null && metadata.isBinary();
  }

  /**
   * Model a public key hash tuple, the hash value and the algorithm used to generate the digest.
   */
  @Value
  @Jacksonized
  @Builder
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class PublicKeyHash {
    String hash;
    String hashingAlgo;
  }

  /**
   * Encode {@link AppMetadata} to its base64(JSON) wire form. Mirrors canonical
   * {@code Metadata.encodeAppMetadata} (at_commons at_key.dart).
   *
   * @param appMetadata value to encode
   * @return base64(JSON) string
   */
  public static String encodeAppMetadata(AppMetadata appMetadata) {
    return appMetadata.encode();
  }

  /**
   * Decode an {@code appMetadata} value that may arrive as a base64(JSON) String (the wire form),
   * a flat JSON object ({@code Map}, the metadata-map form), or an already-parsed
   * {@link AppMetadata}. Mirrors canonical {@code Metadata.decodeAppMetadata}.
   *
   * @param value wire value, or null
   * @return the parsed value, or null when absent
   */
  public static AppMetadata decodeAppMetadata(Object value) {
    return AppMetadata.decode(value);
  }

  /**
   * Provider-owned crypto metadata for the pluggable encryption/decryption model, mirroring the
   * canonical Dart {@code AppMetadata} (at_commons). The SDK owns {@code providerId} — the
   * routing key that selects the crypto provider able to decrypt the value — and preserves any
   * {@code additional} provider-owned entries opaquely. On the wire it is base64(JSON)-encoded
   * (see {@link #encode()}); in metadata maps it is the flat JSON object
   * {@code {"providerId":…, …additional}} (see {@link #toJson()}).
   */
  @Value
  @Builder
  public static class AppMetadata {
    String providerId;
    Map<String, Object> additional;

    /**
     * @return the flat JSON object form — {@code providerId} plus any {@code additional} entries,
     *         serialised flat (not nested under an {@code additional} key), matching canonical.
     */
    @JsonValue
    public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("providerId", providerId);
      if (additional != null) {
        map.putAll(additional);
      }
      return map;
    }

    /**
     * Build from the flat JSON object form; every key other than {@code providerId} is collected
     * into {@code additional}. Used by Jackson when deserialising a metadata map.
     *
     * @param json flat map with a String {@code providerId} and opaque extras
     * @return the parsed value
     * @throws IllegalArgumentException if {@code providerId} is missing or blank
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AppMetadata fromJson(Map<String, Object> json) {
      Object providerId = json.get("providerId");
      if (!(providerId instanceof String) || ((String) providerId).trim().isEmpty()) {
        throw new IllegalArgumentException("Invalid appMetadata.providerId: " + providerId);
      }
      Map<String, Object> additional = new LinkedHashMap<>();
      for (Map.Entry<String, Object> entry : json.entrySet()) {
        if (!"providerId".equals(entry.getKey())) {
          additional.put(entry.getKey(), entry.getValue());
        }
      }
      return AppMetadata.builder()
          .providerId((String) providerId)
          .additional(additional.isEmpty() ? null : additional)
          .build();
    }

    /**
     * @return the base64(JSON) wire encoding of {@link #toJson()}.
     */
    public String encode() {
      return Base64.getEncoder()
          .encodeToString(JsonUtils.writeValueAsString(toJson()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode an {@code appMetadata} value arriving as a base64(JSON) String, a flat JSON
     * {@code Map}, or an already-parsed {@link AppMetadata}. Absent (null or the literal
     * {@code "null"}) yields null.
     *
     * @param value wire value
     * @return the parsed value, or null when absent
     * @throws IllegalArgumentException if the value is a non-decodable shape
     */
    @SuppressWarnings("unchecked")
    public static AppMetadata decode(Object value) {
      if (value == null || "null".equals(value)) {
        return null;
      }
      if (value instanceof AppMetadata) {
        return (AppMetadata) value;
      }
      if (value instanceof Map) {
        return fromJson((Map<String, Object>) value);
      }
      if (value instanceof String && !((String) value).isEmpty()) {
        byte[] decoded = Base64.getDecoder().decode((String) value);
        Object json = JsonUtils.readValue(new String(decoded, StandardCharsets.UTF_8), Object.class);
        if (json instanceof Map) {
          return fromJson((Map<String, Object>) json);
        }
      }
      throw new IllegalArgumentException("Invalid appMetadata: " + value);
    }
  }
}
