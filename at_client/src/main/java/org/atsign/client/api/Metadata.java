package org.atsign.client.api;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import org.atsign.client.impl.util.JsonUtils;

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
  String encoding;
  String ivNonce;

  /**
   * A builder for instantiating {@link Metadata} instances. Note: Metadata is immutable so if you
   * want to create a modified instance then use the toBuilder() method, override the fields and
   * invoke
   * build().
   */
  public static class MetadataBuilder {
    // required for javadoc
  };

  public static Metadata fromJson(String json) throws JsonProcessingException {
    return JsonUtils.MAPPER.readValue(json, Metadata.class);
  }

  /**
   *
   * @return the encoded metadata fields as recognized by an at server in an update command.
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
        .append(sharedKeyEnc != null ? ":sharedKeyEnc:" + sharedKeyEnc : "")
        .append(pubKeyCS != null ? ":pubKeyCS:" + pubKeyCS : "")
        .append(isBinary != null ? ":isBinary:" + isBinary : "")
        .append(isEncrypted != null ? ":isEncrypted:" + isEncrypted : "")
        .append(encoding != null ? ":encoding:" + encoding : "")
        .append(ivNonce != null ? ":ivNonce:" + ivNonce : "")
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
    if (!setEncodingIfNotNull(builder, md1.encoding)) {
      setEncodingIfNotNull(builder, md2.encoding);
    }
    if (!setIvNonceIfNotNull(builder, md1.ivNonce)) {
      setIvNonceIfNotNull(builder, md2.ivNonce);
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

  public static boolean setEncodingIfNotNull(MetadataBuilder builder, String value) {
    if (value != null) {
      builder.encoding(value);
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
}
