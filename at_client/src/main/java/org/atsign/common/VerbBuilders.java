package org.atsign.common;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.atsign.client.util.Preconditions.*;
import static org.atsign.client.util.StringUtil.isBlank;
import static org.atsign.common.Metadata.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.atsign.client.util.EnrollmentId;
import org.atsign.client.util.TypedString;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Metadata.MetadataBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Builder;

/**
 *
 * Contains builders for composing Atsign protocol command strings
 *
 */
public class VerbBuilders {

  private static final Metadata EMPTY_METADATA = Metadata.builder().build();

  /**
   * A builder to compose an Atsign protocol command with the <b>from</b> verb. The <b>from</b> verb
   * is used to tell the Atsign server whom you claim to be and initiates the authentication workflow.
   *
   * @param atSign The {@link AtSign} you claim to be
   * @return A correctly formed <b>from</b> verb command
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "fromCommandBuilder", builderClassName = "FromCommandBuilder")
  public static String from(AtSign atSign) {
    checkNotNull(atSign, "atSign not set");
    return "from:" + atSign;
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>cram</b> verb. The <b>cram</b> verb
   * is used to boostrap authenticate one's own self as an owner of the Atsign server. It is intended
   * to be used once until a set of PKAM keys are cut on the owner's mobile device and from then on we
   * use the pkam verb.
   *
   * @param digest the challenge sent as a result of the from verb encrypted with the CRAM key/secret
   * @return A correctly formed <b>cram</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set.
   */
  @Builder(builderMethodName = "cramCommandBuilder", builderClassName = "CramCommandBuilder")
  public static String cram(String digest) {
    checkNotNull(digest, "digest not set");
    return "cram:" + digest;
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>pol</b> verb. The <b>pol</b> verb
   * is part of the pkam process to authenticate oneself while connecting to someone else's atServer.
   * The term 'pol' means 'proof of life' as it provides a near realtime assurance that the requestor
   * is who it claims to be.
   *
   * @return A correctly formed <b>pol</b> verb command.
   */
  @Builder(builderMethodName = "polCommandBuilder", builderClassName = "PolCommandBuilder")
  public static String pol() {
    return "pol";
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>pkam</b> verb. The <b>pkam</b> verb
   * follows the <b>from</b> verb. As an owner of the atServer, you should be able to take the
   * challenge thrown by the <b>from</b> verb and encrypt using the private key of the RSA key pair
   * with what the server has been bound with. Upon receiving the cram verb along with the digest, the
   * server decrypts the digest using the public key and matches it with the challenge. If they are
   * the same then the atServer lets you connect to the atServer and changes the prompt to your
   * {@link AtSign}.
   *
   * @param digest The challenge sent as a result of the from verb signed with the {@link AtSign}'s
   *        private authentication key.
   * @param signingAlgo The signing algorithm used.
   * @param hashingAlgo The hashing algorithm used.
   * @param enrollmentId The specific enrollment id for the {@link AtSign} that matches a specific
   *        authentication key pair.
   * @return A correctly formed <b>pkam</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "pkamCommandBuilder", builderClassName = "PkamCommandBuilder")
  public static String pkam(String digest, String signingAlgo, String hashingAlgo, EnrollmentId enrollmentId) {
    checkNotNull(digest, "digest not set");
    if (enrollmentId != null) {
      checkNotBlank(signingAlgo, "signingAlgo not set");
      checkNotBlank(hashingAlgo, "hashingAlgo not set");
    }

    return new StringBuilder("pkam")
        .append(signingAlgo != null ? ":signingAlgo:" + signingAlgo : "")
        .append(hashingAlgo != null ? ":hashingAlgo:" + hashingAlgo : "")
        .append(enrollmentId != null ? ":enrollmentId:" + enrollmentId : "")
        .append(':').append(digest)
        .toString();
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>update</b> verb. The <b>update</b>
   * is used to insert key/value pairs into a Key Store. An update command can only be sent by the
   * {@link AtSign} that "owns" the key value and can only be sent to their own Atsign server.
   *
   * @param keyName The namespace qualified key name (without the sharedBy or sharedWith or public,
   *        hidden or cache qualifiers).
   * @param sharedBy The {@link AtSign} which is owns / is sharing this key value (this will qualify
   *        the keyName is the built command).
   * @param sharedWith The {@link AtSign} which is receiving this key value (this will qualify the
   *        keyName is the built command).
   * @param isHidden Denotes whether the key value is hidden (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param isPublic Denotes whether the key value is public (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param isCached Denotes whether the key value is cached (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param ttl Sets the time to live in the metadata (milliseconds). This overrides the metadata
   *        param if this is also set.
   * @param ttb Sets the time to birth in the metadata (milliseconds). This overrides the metadata
   *        param if this is also set.
   * @param ttr Sets the time to refresh (for a cached key) in the metadata (milliseconds). The value
   *        -1 denotes "cached forever". This overrides the metadata param if this is also set.
   * @param ccd Indicates if a cached key needs to be deleted when the atSign user who has originally
   *        shared it deletes it. This overrides the metadata param if this is also set.
   * @param isBinary Sets metadata field which indicates a binary value. This overrides the metadata
   *        param if this is also set.
   * @param isEncrypted Sets metadata field which indicates that value is encrypted. This overrides
   *        the metadata param if this is also set.
   * @param dataSignature sets metadata field that holds signature of the value. This overrides the
   *        metadata param if this is also set.
   * @param sharedKeyEnc Sets metadata field. This overrides the metadata param if this is also set.
   * @param pubKeyCS sets metadata field. This overrides the metadata param if this is also set.
   * @param encoding sets metadata field. This overrides the metadata param if this is also set.
   * @param ivNonce sets metadata field used to hold the encryption initialization vector when value
   *        is encrypted. This overrides the metadata param if this is also set.
   * @param value the value of the key / value. This overrides the metadata param if this is also set.
   * @param key a {@link AtKey} instance from which keyName, sharedBy, sharedWith and metadata will be
   *        taken from.
   * @param rawKey the Atsign protocol key with cached and public qualifications
   * @return A correctly formed <b>update</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "updateCommandBuilder", builderClassName = "UpdateCommandBuilder")
  public static String update(String keyName, AtSign sharedBy, AtSign sharedWith, Boolean isHidden, Boolean isPublic,
                              Boolean isCached, Long ttl, Long ttb, Long ttr, Boolean ccd, Boolean isBinary,
                              Boolean isEncrypted, String dataSignature, String sharedKeyEnc, String pubKeyCS,
                              String encoding, String ivNonce, Object value, AtKey key, String rawKey) {

    String metadataString;
    String keyString;

    if (key != null) {
      checkAllNull("both key and key fields set", keyName, sharedBy, sharedWith, isHidden, isPublic, isCached, ttl, ttb,
                   ttr, ccd, isBinary, isEncrypted, dataSignature, sharedKeyEnc, pubKeyCS, encoding, ivNonce);
      metadataString = key.metadata().toString();
      keyString = key.toString();
    } else {
      MetadataBuilder metadataBuilder = createBlankMetadataBuilder();
      if (rawKey == null) {
        setIsHiddenIfNotNull(metadataBuilder, isHidden);
        setIsPublicIfNotNull(metadataBuilder, isPublic);
        setIsCachedIfNotNull(metadataBuilder, isCached);
      } else {
        checkAllNull("both rawKeys and isHidden, isPublic isCached set", isHidden, isPublic, isCached);
        checkAllNull("both rawKeys and key fields set", keyName, sharedBy, sharedWith);
      }
      setTtlIfNotNull(metadataBuilder, ttl);
      setTtrIfNotNull(metadataBuilder, ttr);
      setTtbIfNotNull(metadataBuilder, ttb);
      setCcdIfNotNull(metadataBuilder, ccd);
      setIsBinaryIfNotNull(metadataBuilder, isBinary);
      setIsEncryptedIfNotNull(metadataBuilder, isEncrypted);
      setDataSignatureIfNotNull(metadataBuilder, dataSignature);
      setSharedKeyEncIfNotNull(metadataBuilder, sharedKeyEnc);
      setPubKeyCSIfNotNull(metadataBuilder, pubKeyCS);
      setEncodingIfNotNull(metadataBuilder, encoding);
      setIvNonceIfNotNull(metadataBuilder, ivNonce);

      checkNotBlank(keyName, "keyName not set");
      checkNotNull(sharedBy, "sharedBy not set");
      checkNotNull(value, "value not set");

      Metadata metadata = metadataBuilder.build();
      metadataString = metadata.toString();
      keyString = rawKey != null ? rawKey : toRawKey(keyName, sharedBy, sharedWith, metadata);
    }

    return String.format("update%s:%s %s", metadataString, keyString, value);
  }

  /**
   * Controls whether lookups return just the value, just the metadata or the value and metadata
   */
  public enum LookupOperation {
    none, meta, all
  };

  /**
   * A builder to compose an Atsign protocol command with the <b>llookup</b> verb. The <b>llookup</b>
   * verb is used to look up key values "owned" / shared by the {@link AtSign} that is sending the
   * command.
   *
   * @param keyName The namespace qualified key name (without the sharedBy or sharedWith or public,
   *        hidden or cache qualifiers).
   * @param sharedBy The {@link AtSign} which is owns / is sharing this key value (this will qualify
   *        the keyName is the built command).
   * @param sharedWith The {@link AtSign} which is receiving this key value (this will qualify the
   *        keyName is the built command).
   * @param isHidden Denotes whether the key value is hidden (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param isPublic Denotes whether the key value is public (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param isCached Denotes whether the key value is cached (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param operation Controls whether lookups return just the value, just the metadata or the value
   *        and metadata.
   * @param key a {@link AtKey} instance from which keyName, sharedBy, sharedWith and
   *        public/hidden/cached qualifiers will be taken from.
   * @param rawKey The "raw" protocol key string for the key. i.e. includes keyName, sharedBy,
   *        sharedWith and public/hidden/cached qualifiers (this will override those fields).
   * @return A correctly formed <b>llookup</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "llookupCommandBuilder", builderClassName = "LlookupCommandBuilder")
  public static String llookup(String keyName, AtSign sharedBy, AtSign sharedWith, Boolean isHidden, Boolean isPublic,
                               Boolean isCached, LookupOperation operation, AtKey key, String rawKey) {

    String operationString = toOperationString(operation);
    String keyString;
    if (rawKey != null) {
      checkAllNull("both rawKey and key fields are set", keyName, sharedBy, sharedWith, key);
      keyString = rawKey;
    } else if (key != null) {
      checkAllNull("both key and key fields are set", keyName, sharedBy, sharedWith, isHidden, isPublic, isCached);
      keyString = key.toString();
    } else {
      checkNotBlank(keyName, "keyName not set");
      checkNotNull(sharedBy, "sharedBy not set");
      MetadataBuilder metadataBuilder = createBlankMetadataBuilder();
      setIsHiddenIfNotNull(metadataBuilder, isHidden);
      setIsPublicIfNotNull(metadataBuilder, isPublic);
      setIsCachedIfNotNull(metadataBuilder, isCached);
      keyString = toRawKey(keyName, sharedBy, sharedWith, metadataBuilder.build());
    }
    return String.format("llookup:%s%s", operationString, keyString);
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>lookup</b> verb. The <b>lookup</b>
   * verb is used to look up key values shared by other {@link AtSign}s with the {@link AtSign} that
   * is sending the command.
   *
   * @param keyName The namespace qualified key name (without the sharedBy or sharedWith or public,
   *        hidden or cache qualifiers).
   * @param sharedBy The {@link AtSign} which is owns / is sharing this key value (this will qualify
   *        the keyName is the built command).
   * @param operation Controls whether lookups return just the value, just the metadata or the value
   *        and metadata.
   * @param key a {@link AtKey} instance from which keyName, sharedBy, sharedWith and
   *        public/hidden/cached qualifiers will be taken from.
   * @param rawKey The "raw" protocol key string for the key. i.e. includes keyName, sharedBy,
   *        sharedWith and public/hidden/cached qualifiers (this will override those fields).
   * @return A correctly formed <b>lookup</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "lookupCommandBuilder", builderClassName = "LookupCommandBuilder")
  public static String lookup(String keyName, AtSign sharedBy, LookupOperation operation, Keys.SharedKey key,
                              String rawKey) {
    String operationString = toOperationString(operation);
    String keyString;
    if (rawKey != null) {
      checkAllNull("both rawKey and key fields are set", keyName, sharedBy, key);
      keyString = rawKey;
    } else if (key != null) {
      checkAllNull("both key and key fields are set", keyName, sharedBy);
      keyString = toRawKey(key.name(), key.sharedBy());
    } else {
      checkNotBlank(keyName, "keyName not set");
      checkNotNull(sharedBy, "sharedBy not set");
      keyString = toRawKey(keyName, sharedBy);
    }
    return String.format("lookup:%s%s", operationString, keyString);
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>plookup</b> verb. The <b>plookup</b>
   * verb is used to look up a public key value shared by an {@link AtSign} other than the one that
   * is sending the command.
   *
   * @param keyName The namespace qualified key name (without the sharedBy or sharedWith or public,
   *        hidden or cache qualifiers).
   * @param sharedBy The {@link AtSign} which is owns / is sharing this key value (this will qualify
   *        the keyName is the built command).
   * @param bypassCache If true this forces the value to be fetch from the Atsign server that has
   *        shared the key value.
   * @param operation Controls whether lookups return just the value, just the metadata or the value
   *        and metadata.
   * @param key a {@link AtKey} instance from which keyName, sharedBy, sharedWith and
   *        public/hidden/cached qualifiers will be taken from.
   * @param rawKey The "raw" protocol key string for the key. i.e. includes keyName, sharedBy,
   *        sharedWith and public/hidden/cached qualifiers (this will override those fields).
   * @return A correctly formed <b>plookup</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "plookupCommandBuilder", builderClassName = "PlookupCommandBuilder")
  public static String plookup(String keyName, AtSign sharedBy, Boolean bypassCache, LookupOperation operation,
                               AtKey key, String rawKey) {

    String bypassCacheString = toBypassCacheString(bypassCache);
    String operationString = toOperationString(operation);
    String keyString;
    if (rawKey != null) {
      checkAllNull("both rawKey and key fields are set", keyName, sharedBy, key);
      keyString = rawKey;
    } else if (key != null) {
      checkAllNull("both key and key fields are set", keyName, sharedBy);
      keyString = toRawKey(key.name(), key.sharedBy());
    } else {
      checkNotBlank(keyName, "keyName not set");
      checkNotNull(sharedBy, "sharedBy not set");
      keyString = toRawKey(keyName, sharedBy);
    }

    return String.format("plookup:%s%s%s", bypassCacheString, operationString, keyString);
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>delete</b> verb. The <b>delete</b>
   * verb is used to remove key/value pairs into a Key Store. An <b>delete</b> command can only be
   * sent by the {@link AtSign} that "owns" the key value and can only be sent to their own Atsign
   * server.
   *
   * @param keyName The namespace qualified key name (without the sharedBy or sharedWith or public,
   *        hidden or cache qualifiers).
   * @param sharedBy The {@link AtSign} which is owns / is sharing this key value (this will qualify
   *        the keyName is the built command).
   * @param sharedWith The {@link AtSign} which is receiving this key value (this will qualify the
   *        keyName is the built command).
   * @param isHidden Denotes whether the key value is hidden (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param isPublic Denotes whether the key value is public (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param isCached Denotes whether the key value is cached (this will qualify the keyName in the
   *        built command and set the metadata).
   * @param key a {@link AtKey} instance from which keyName, sharedBy, sharedWith and metadata will be
   *        taken from.
   * @return A correctly formed <b>delete</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "deleteCommandBuilder", builderClassName = "DeleteCommandBuilder")
  public static String delete(String keyName, AtSign sharedBy, AtSign sharedWith, Boolean isHidden, Boolean isPublic,
                              Boolean isCached, AtKey key, String rawKey) {

    String keyString;
    if (rawKey != null) {
      checkAllNull("both rawKey and key fields are set", keyName, sharedBy, sharedWith, key);
      keyString = rawKey;
    } else if (key != null) {
      checkAllNull("both key and isHidden, isPublic, isCached are set",
                   keyName, sharedBy, sharedWith, isHidden, isPublic, isCached);
      keyString = key.toString();
    } else {
      MetadataBuilder metadataBuilder = createBlankMetadataBuilder();
      setIsHiddenIfNotNull(metadataBuilder, isHidden);
      setIsPublicIfNotNull(metadataBuilder, isPublic);
      setIsCachedIfNotNull(metadataBuilder, isCached);
      checkNotBlank(keyName, "keyName not set");
      checkNotNull(sharedBy, "sharedBy not set");
      keyString = toRawKey(keyName, sharedBy, sharedWith, metadataBuilder.build());
    }

    return String.format("delete:%s", keyString);
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>scan</b> verb. The <b>scan</b> verb
   * is used to list the keys in an {@link AtSign}'s Atsign server.
   *
   * @param regex If set only show keys that match this regular expression pattern.
   * @param fromAtSign If set only show keys that are created by the {@link AtSign}.
   * @param showHidden If true, will show hidden internal keys.
   * @return A correctly formed <b>scan</b> verb command.
   */
  @Builder(builderMethodName = "scanCommandBuilder", builderClassName = "ScanCommandBuilder")
  public static String scan(String regex, AtSign fromAtSign, Boolean showHidden) {

    StringBuilder builder = new StringBuilder("scan");
    if (isTrue(showHidden)) {
      builder.append(":showHidden:true");
    }
    if (fromAtSign != null) {
      builder.append(':').append(fromAtSign);
    }
    if (!isBlank(regex)) {
      builder.append(' ').append(regex);
    }
    return builder.toString();
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>notify:messageType:text</b> verb.
   * The <b>notify:messageType:text</b> verb is used to send an arbitrary message to another
   * {@link AtSign}.
   *
   * @param recipient The {@link AtSign} you wish to send the message to.
   * @param text The message.
   * @return A correctly formed <b>notify:messageType:text</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "notifyTextCommandBuilder", builderClassName = "NotifyTextCommandBuilder")
  public static String notifyText(AtSign recipient, String text) {
    checkNotNull(recipient, "recipient not set");
    checkNotBlank(text, "text not set");
    return String.format("notify:messageType:text:%s:%s", recipient, text);
  }

  /**
   * The type of key change operation
   */
  public enum NotifyOperation {
    update, delete
  }

  /**
   * A builder to compose an Atsign protocol command with the
   * <b>notify:(update|delete):messageType:key</b> verb.
   * The <b>notify:(update|delete):messageType:key</b> verb is used to send key change notifications
   * to other
   * {@link AtSign}s.
   *
   * @param operation Update or Delete.
   * @param recipient The {@link AtSign} to send the notification to.
   * @param sender The {@link AtSign} that is sending the notification.
   * @param key The namespace qualified key name.
   * @param value The updated value for the key.
   * @param ttr Sets the time to refresh (milliseconds).
   * @return A correctly formed <b>notify:messageType:key</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "notifyKeyChangeCommandBuilder", builderClassName = "NotifyKeyChangeCommandBuilder")
  public static String notifyKeyChange(NotifyOperation operation, AtSign recipient, AtSign sender, String key,
                                       String value,
                                       Long ttr) {

    checkNotBlank(key, "key not set");
    checkNotNull(operation, "operation not set");

    if (ttr != null) {
      checkTrue(ttr >= -1, "ttr < -1");
      checkNotBlank(value, "value not set (mandatory when ttr is set)");
    }

    return new StringBuilder("notify:")
        .append(operation)
        .append(":messageType:key:")
        .append(ttr != null ? "ttr:" + ttr + ":" : "")
        .append(recipient != null ? recipient + ":" : "")
        .append(key)
        .append(sender != null ? sender : "")
        .append(!isBlank(value) ? ":" + value : "")
        .toString();
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>notify:status</b> verb.
   * The <b>notify:status</b> verb is query the status of a previously sent notification.
   *
   * @param notificationId The unique id of a notification. This will have been the response for to a
   *        previously sent notify:(update:delete) command.
   * @return A correctly formed <b>notify:status</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "notifyStatusCommandBuilder", builderClassName = "NotifyStatusCommandBuilder")
  public static String notifyStatus(String notificationId) {

    checkNotBlank(notificationId, "notificationId not set");

    return "notify:status:" + notificationId;
  }

  /**
   * Types of enroll operations
   */
  public enum EnrollOperation {
    request, approve, deny, revoke, list, fetch, unrevoke, delete
  }

  /**
   * JSON member names used in enroll parameters
   */
  public static class EnrollParameters {
    public static final String ENROLLMENT_ID = "enrollmentId";
    public static final String ENCRYPTED_PRIVATE_KEY = "encryptedDefaultEncryptionPrivateKey";
    public static final String PRIVATE_KEY_IV = "encPrivateKeyIV";
    public static final String ENCRYPTED_SELF_ENCRYPTION_KEY = "encryptedDefaultSelfEncryptionKey";
    public static final String SELF_ENCRYPTION_KEY_IV = "selfEncKeyIV";
    public static final String ENROLLMENT_STATUS_FILTER = "enrollmentStatusFilter";
    public static final String APP_NAME = "appName";
    public static final String DEVICE_NAME = "deviceName";
    public static final String APKAM_PUBLIC_KEY = "apkamPublicKey";
    public static final String ENCRYPTED_APKAM_SYMMETRIC_KEY = "encryptedAPKAMSymmetricKey";
    public static final String OTP = "otp";
    public static final String NAMESPACES = "namespaces";
    public static final String APKAM_KEYS_EXPIRY_IN_MILLIS = "apkamKeysExpiryInMillis";
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>enroll</b> verb.
   * The <b>enroll</b> verb is used to submit an APKAM enrollment.
   *
   * @param operation The specific enroll operation to perform see {@link EnrollOperation}.
   * @param status Filters {@link EnrollOperation#list} operations.
   * @param enrollmentId The unique enrollment id to refer to in fetch, approve,deny,delete,revoke or
   *        unrevoke.
   * @param encryptPrivateKey The private encryption key which all enrollments share for an
   *        {@link AtSign}. This is mandatory for an approve and the value should be encrypted with
   *        the APKAM symmetric key that was provided by the {@link EnrollOperation#request}.
   * @param encryptPrivateKeyIv The initialization vector used for the private key encryption.
   * @param selfEncryptKey The self encryption key which all enrollments share for an
   *        {@link AtSign}. This is mandatory for an approve and the value should be encrypted with
   *        the APKAM symmetric key that was provided by the {@link EnrollOperation#request}.
   * @param selfEncryptKeyIv The initialization vector used for the self key encryption.
   * @param appName The application name qualifier for the appName deviceName combination we are
   *        enrolling.
   * @param deviceName The device name qualifier for the appName deviceName combination we are
   *        enrolling.
   * @param apkamPublicKey The public authentication key for the appName and device we are enrolling.
   * @param otp The one time password for {@link EnrollOperation#request}.
   * @param namespaces A map of namespace access control associations. e.g. ns1 {@code -->} rw, ns2
   *        {@code -->} r (r = read only, rw = read write).
   * @param apkamSymmetricKey A one-time symmetric key used for the duration of the enrollment
   *        workflow. This should be encrypted with the public encryption key of the {@link AtSign}.
   * @param ttl The time to live for the enrollment request.
   * @return A correctly formed <b>enroll</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "enrollCommandBuilder", builderClassName = "EnrollCommandBuilder")
  public static String enroll(EnrollOperation operation, String status, EnrollmentId enrollmentId,
                              String encryptPrivateKey, String encryptPrivateKeyIv, String selfEncryptKey,
                              String selfEncryptKeyIv, String appName, String deviceName, String apkamPublicKey,
                              String otp, Map<String, String> namespaces, String apkamSymmetricKey,
                              long ttl) {

    checkNotNull(operation, "operation not set");

    Object params = null;

    switch (operation) {
      case list:
        if (!isBlank(status)) {
          params = singletonMap(EnrollParameters.ENROLLMENT_STATUS_FILTER, singletonList(status));
        }
        break;
      case approve:
        checkNotNull(enrollmentId, "enrollmentId not set");
        checkNotNull(encryptPrivateKey, "encryptPrivateKey not set");
        checkNotNull(encryptPrivateKeyIv, "encryptPrivateKeyIv not set");
        checkNotNull(selfEncryptKey, "selfEncryptKey not set");
        checkNotNull(selfEncryptKeyIv, "selfEncryptKeyIv not set");
        params = toObjectMap(EnrollParameters.ENROLLMENT_ID, enrollmentId,
                             EnrollParameters.ENCRYPTED_PRIVATE_KEY, encryptPrivateKey,
                             EnrollParameters.PRIVATE_KEY_IV, encryptPrivateKeyIv,
                             EnrollParameters.ENCRYPTED_SELF_ENCRYPTION_KEY, selfEncryptKey,
                             EnrollParameters.SELF_ENCRYPTION_KEY_IV, selfEncryptKeyIv);
        break;
      case fetch:
      case deny:
      case revoke:
      case unrevoke:
      case delete:
        checkNotNull(enrollmentId, "enrollmentId not set");
        params = toObjectMap(EnrollParameters.ENROLLMENT_ID, enrollmentId);
        break;
      case request:
        checkNotBlank(appName, "appName not set");
        checkNotBlank(deviceName, "deviceName not set");
        checkNotBlank(apkamPublicKey, "apkamPublicKey not set");
        if (otp == null) {
          params = toObjectMap(EnrollParameters.APP_NAME, appName,
                               EnrollParameters.DEVICE_NAME, deviceName,
                               EnrollParameters.APKAM_PUBLIC_KEY, apkamPublicKey);
        } else {
          checkNotBlank(otp, "otp not set");
          checkNotNull(namespaces, "namespaces not set");
          params = toObjectMap(EnrollParameters.APP_NAME, appName,
                               EnrollParameters.DEVICE_NAME, deviceName,
                               EnrollParameters.APKAM_PUBLIC_KEY, apkamPublicKey,
                               EnrollParameters.ENCRYPTED_APKAM_SYMMETRIC_KEY, apkamSymmetricKey,
                               EnrollParameters.OTP, otp,
                               EnrollParameters.NAMESPACES, namespaces,
                               EnrollParameters.APKAM_KEYS_EXPIRY_IN_MILLIS, ttl);
        }
        break;
      default:
        throw new IllegalArgumentException("unsupported operation");
    }

    return new StringBuilder("enroll:")
        .append(operation)
        .append(params != null ? encodeAsJson(params) : "")
        .toString();
  }

  /**
   * Types of operation for keys verb
   */
  public enum KeysOperation {
    put, get, delete
  };

  /**
   * A builder to compose an Atsign protocol command with the <b>keys</b> verb.
   * The <b>keys</b> verb is specifically used to update security keys in the Atsign server.
   *
   * @param operation put, get or delete.
   * @param keyName The full qualified key name which includes sharedBy, sharedWith, namespace and
   *        visibility qualifiers.
   * @return A correctly formed <b>keys</b> verb command.
   * @throws IllegalArgumentException If mandatory fields are not set or if field values conflict.
   */
  @Builder(builderMethodName = "keysCommandBuilder", builderClassName = "KeysCommandBuilder")
  public static String keys(KeysOperation operation, String keyName) {
    checkNotNull(operation, "operation not set");
    if (operation == KeysOperation.get) {
      checkNotBlank(keyName, "keyName not set");
      return String.format("keys:get:keyName:%s", keyName);
    } else {
      throw new IllegalArgumentException(operation + " not supported");
    }
  }

  /**
   * A builder to compose an Atsign protocol command with the <b>otp</b> verb.
   * The <b>otp</b> verb is used to request a one time password for the enrollment workflow.
   *
   * @return A correctly formed <b>otp</b> verb command.
   */
  @Builder(builderMethodName = "otpCommandBuilder", builderClassName = "OtpCommandBuilder")
  public static String otp() {
    return "otp:get";
  }

  protected static Map<String, Object> toObjectMap(Object... nameValuePairs) {
    if ((nameValuePairs.length % 2) != 0) {
      throw new IllegalArgumentException("odd number of parameters");
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < nameValuePairs.length; i++) {
      String key = nameValuePairs[i].toString();
      Object value = nameValuePairs[++i];
      if (value instanceof TypedString) {
        map.put(key, value.toString());
      } else {
        map.put(key, value);
      }
    }
    return map;
  }

  protected static String encodeAsJson(Object o) {
    try {
      return Json.MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("json encoding exception", e);
    }
  }

  private static boolean isTrue(Boolean bool) {
    return bool != null && bool;
  }

  private static String toRawKey(String keyName, AtSign sharedBy) {
    return toRawKey(keyName, sharedBy, null, EMPTY_METADATA);
  }

  private static String toRawKey(String keyName, AtSign sharedBy, AtSign sharedWith, Metadata metadata) {
    StringBuilder builder = new StringBuilder();
    if (isTrue(metadata.isHidden())) {
      builder.append("_");
    }
    if (isTrue(metadata.isCached())) {
      builder.append("cached:");
    }
    if (isTrue(metadata.isPublic())) {
      builder.append("public:");
    }
    if (sharedWith != null) {
      builder.append(sharedWith).append(':');
    }
    builder.append(keyName);
    builder.append(sharedBy);
    return builder.toString();
  }

  private static String toOperationString(LookupOperation operation) {
    if (operation == null || operation == LookupOperation.none) {
      return "";
    }
    return operation + ":";
  }

  private static String toBypassCacheString(Boolean bypassCache) {
    return isTrue(bypassCache) ? "bypassCache:true:" : "";
  }

  private static MetadataBuilder createBlankMetadataBuilder() {
    return Metadata.builder()
        .isPublic(null)
        .isEncrypted(null)
        .isHidden(null)
        .namespaceAware(null)
        .isBinary(null)
        .isCached(null);
  }
}
