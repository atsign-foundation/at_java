package org.atsign.client.impl.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.impl.util.EncryptionUtils.aesDecryptFromBase64;
import static org.atsign.client.impl.util.EncryptionUtils.aesEncryptToBase64;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.common.EnrollmentId;
import org.atsign.client.impl.common.TypedString;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.atsign.client.impl.exceptions.AtDecryptionException;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for loading a saving {@link AtKeys} from the file system.
 */
@Slf4j
public class KeysUtils {

  static private final String EMPTY_IV = Base64.getEncoder().encodeToString(new byte[16]);

  private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {};

  public static final String ATSIGN_KEYS_DIR = "ATSIGN_KEYS_DIR";
  public static final String ATSIGN_KEYS_SUFFIX = "ATSIGN_KEYS_SUFFIX";

  public static String expectedKeysFilesLocation = getFirstNonEmpty(
                                                                    System.getProperty(ATSIGN_KEYS_DIR),
                                                                    System.getenv(ATSIGN_KEYS_DIR),
                                                                    System.getProperty("user.home") + "/.atsign/keys/");

  public static final String legacyKeysFilesLocation = System.getProperty("user.dir") + "/keys/";

  public static String keysFileSuffix = getFirstNonEmpty(
                                                         System.getProperty(ATSIGN_KEYS_SUFFIX),
                                                         System.getenv(ATSIGN_KEYS_SUFFIX),
                                                         "_key.atKeys");

  /**
   * NB: These values are used in the JSON representation (atKeys file contents) and MUST match those
   * used in other SDK impls
   */
  private static final String VERSION_KEY = "version";
  private static final String VERSION_1 = "1";
  private static final String PKAM_PUBLIC_KEY = "aesPkamPublicKey";
  private static final String PKAM_PRIVATE_KEY = "aesPkamPrivateKey";
  private static final String ENCRYPT_PUBLIC_KEY = "aesEncryptPublicKey";
  private static final String ENCRYPT_PRIVATE_KEY = "aesEncryptPrivateKey";
  private static final String SELF_ENCRYPT_KEY = "selfEncryptionKey";
  private static final String APKAM_SYMMETRIC_KEY = "apkamSymmetricKey";
  private static final String ENROLLMENT_ID = "enrollmentId";

  private static final List<String> FLAT_FIELDS = List.of(SELF_ENCRYPT_KEY, ENROLLMENT_ID, APKAM_SYMMETRIC_KEY,
                                                          PKAM_PUBLIC_KEY, PKAM_PRIVATE_KEY, ENCRYPT_PUBLIC_KEY,
                                                          ENCRYPT_PRIVATE_KEY);

  /**
   * NB: These values are the typed keys document at_auth writes, and MUST match its field names
   */
  private static final String TYPED_ENROLLMENTS = "enrollments";
  private static final String TYPED_KEYS = "keys";
  private static final String TYPED_MATERIAL = "material";
  private static final String TYPED_ROLE = "role";
  private static final String TYPED_STATUS = "status";
  private static final String ROLE_PRIVATE_AUTHENTICATION = "privateAuthentication";
  private static final String STATUS_ACTIVE = "active";

  /**
   * Persists {@link AtKeys} to the default file for the {@link AtSign}.
   *
   * @param atSign The {@link AtSign} which these keys relate to.
   * @param keys The {@link AtKeys} to persist.
   * @throws Exception If anything fails.
   */
  public static void saveKeys(AtSign atSign, AtKeys keys) throws Exception {
    saveKeys(keys, getKeysFile(atSign));
  }

  /**
   * Persists {@link AtKeys} to a file, in the legacy flat shape.
   *
   * @param keys The {@link AtKeys} to persist.
   * @param file The file to write / overwrite.
   * @throws IOException If anything fails, or the file is a typed keys document: this class cannot
   *         carry its typed key material, so rewriting it would lose that material.
   */
  public static void saveKeys(AtKeys keys, File file) throws IOException {
    refuseToOverwriteTypedDocument(file);
    if (file.getParentFile() != null && !file.getParentFile().exists()) {
      Files.createDirectories(file.getParentFile().toPath());
    }
    log.info("Saving keys to {}", file.getAbsolutePath());

    Files.write(file.toPath(), getAsJson(keys).getBytes(UTF_8));
  }

  /**
   * Instantiates a {@link AtKeys} loaded with the contents of the default
   * keys file for the {@link AtSign}.
   *
   * @param atSign The {@link AtSign} which these keys relate to.
   * @return A populated {@link AtKeys} instance.
   * @throws AtClientConfigException If anything fails or the keys file does not exist.
   */
  public static AtKeys loadKeys(AtSign atSign) throws AtClientConfigException {
    return loadKeys(getKeysFileFallbackToLegacyLocation(atSign));
  }

  /**
   * Instantiates a {@link AtKeys} loaded with the contents of a keys file: the legacy flat shape, or
   * the typed keys document at_auth writes, whose flat fields are read and typed containers ignored.
   *
   * @param file The file to read.
   * @return A populated {@link AtKeys} instance.
   * @throws AtClientConfigException If anything fails, the keys file does not exist, or it is a
   *         typed document that authenticates through typed key material, which this class cannot
   *         read.
   */
  public static AtKeys loadKeys(File file) throws AtClientConfigException {
    try {
      return createAtKeysFromJson(Files.readString(file.toPath()));
    } catch (IOException e) {
      throw new AtClientConfigException("failed to read " + file, e);
    }
  }

  private static void refuseToOverwriteTypedDocument(File file) throws IOException {
    if (!file.exists()) {
      return;
    }
    Object version;
    try {
      version = JsonUtils.readValue(Files.readString(file.toPath()), JSON_OBJECT_TYPE).get(VERSION_KEY);
    } catch (RuntimeException e) {
      return;
    }
    if (version instanceof Number) {
      throw new IOException("refusing to overwrite " + file + ": it is a typed keys document, and rewriting it"
          + " would lose the typed key material it holds");
    }
  }

  private static File getKeysFileFallbackToLegacyLocation(AtSign atSign) throws AtClientConfigException {
    // check first if file exists at canonical location ~/.atsign/keys/$atSign_key.atKeys
    File file = getKeysFile(atSign, expectedKeysFilesLocation);

    if (!file.exists()) {
      // if keys do not exist in root, check in keys sub-directory under current working directory
      File legacyFile = getKeysFile(atSign, legacyKeysFilesLocation);
      // if file does not exist under current working directory, we're done - can't find the keys file
      if (!legacyFile.exists()) {
        throw new AtClientConfigException("loadKeys: No file found at " + file + " or " + legacyFile);
      }
      file = legacyFile;
    }
    return file;
  }

  /**
   * The default file for an {@link AtSign}. This will default the file location (directory)
   * and filename.
   *
   * @param atSign The {@link AtSign} which these keys relate to.
   * @return The file.
   */
  public static File getKeysFile(AtSign atSign) {
    return getKeysFile(atSign, expectedKeysFilesLocation);
  }

  /**
   * The default file for an {@link AtSign}. This will default the filename.
   *
   * @param atSign The {@link AtSign} which these keys relate to.
   * @param dir The directory for the file.
   * @return The file.
   */
  public static File getKeysFile(AtSign atSign, String dir) {
    return new File(dir, atSign + keysFileSuffix);
  }

  private static String getFirstNonEmpty(String... candidates) {
    for (String candidate : candidates) {
      if (candidate != null && !candidate.trim().isEmpty()) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("all candidates are null");
  }

  private static String getAsJson(AtKeys keys) {
    try {
      Map<String, String> map = new TreeMap<>();

      mapPut(map, SELF_ENCRYPT_KEY, keys.getSelfEncryptKey());
      mapPut(map, ENROLLMENT_ID, keys.getEnrollmentId());
      mapPut(map, APKAM_SYMMETRIC_KEY, keys.getApkamSymmetricKey());

      mapPutEncrypted(map, PKAM_PUBLIC_KEY, keys.getApkamPublicKey(), keys.getSelfEncryptKey());
      mapPutEncrypted(map, PKAM_PRIVATE_KEY, keys.getApkamPrivateKey(), keys.getSelfEncryptKey());
      mapPutEncrypted(map, ENCRYPT_PUBLIC_KEY, keys.getEncryptPublicKey(), keys.getSelfEncryptKey());
      mapPutEncrypted(map, ENCRYPT_PRIVATE_KEY, keys.getEncryptPrivateKey(), keys.getSelfEncryptKey());

      return JsonUtils.writeValueAsString(map, true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static AtKeys createAtKeysFromJson(String json) throws AtClientConfigException {
    Map<String, Object> document = readDocument(json);
    if (isTypedDocument(document)) {
      refuseTypedAuthentication(document);
    }
    try {
      return createAtKeysVersion1(flatFields(document));
    } catch (AtDecryptionException e) {
      throw new AtClientConfigException("failed to create AtKeys from json", e);
    }
  }

  private static Map<String, Object> readDocument(String json) throws AtClientConfigException {
    try {
      return JsonUtils.readValue(json, JSON_OBJECT_TYPE);
    } catch (RuntimeException e) {
      throw new AtClientConfigException("AtKeys json is not a JSON object", e);
    }
  }

  /**
   * An integer version 1 marks the typed keys document at_auth writes. No version, or the string
   * "1" that earlier releases of this class wrote, marks the legacy flat shape.
   */
  private static boolean isTypedDocument(Map<String, Object> document) throws AtClientConfigException {
    if (!document.containsKey(VERSION_KEY)) {
      return false;
    }
    Object version = document.get(VERSION_KEY);
    if (VERSION_1.equals(version)) {
      return false;
    }
    if (Integer.valueOf(1).equals(version)) {
      return true;
    }
    throw new AtClientConfigException("unsupported version of AtKeys json : " + version);
  }

  /**
   * A typed document authenticates as the enrollment holding active typed authentication material,
   * and its flat fields then belong to a different enrollment, so reading them would authenticate
   * as the wrong principal.
   */
  private static void refuseTypedAuthentication(Map<String, Object> document) throws AtClientConfigException {
    Set<String> enrollmentIds = new TreeSet<>();
    for (Object enrollment : listOf(document.get(TYPED_ENROLLMENTS), TYPED_ENROLLMENTS)) {
      Map<?, ?> enrollmentEntry = mapOf(enrollment, TYPED_ENROLLMENTS);
      for (Object key : listOf(enrollmentEntry.get(TYPED_KEYS), TYPED_ENROLLMENTS + "[].keys")) {
        Map<?, ?> keyEntry = mapOf(key, TYPED_ENROLLMENTS + "[].keys");
        for (Object material : listOf(keyEntry.get(TYPED_MATERIAL), TYPED_ENROLLMENTS + "[].keys[].material")) {
          Map<?, ?> materialEntry = mapOf(material, TYPED_ENROLLMENTS + "[].keys[].material");
          Object status = materialEntry.get(TYPED_STATUS);
          if (ROLE_PRIVATE_AUTHENTICATION.equals(materialEntry.get(TYPED_ROLE))
              && (status == null || STATUS_ACTIVE.equals(status))) {
            enrollmentIds.add(String.valueOf(enrollmentEntry.get(ENROLLMENT_ID)));
          }
        }
      }
    }
    if (!enrollmentIds.isEmpty()) {
      throw new AtClientConfigException("AtKeys json authenticates as enrollment " + String.join(", ", enrollmentIds)
          + " through typed key material, which this client cannot read");
    }
  }

  private static List<?> listOf(Object value, String field) throws AtClientConfigException {
    if (value == null) {
      return List.of();
    }
    if (value instanceof List) {
      return (List<?>) value;
    }
    throw new AtClientConfigException("AtKeys json field " + field + " is not a list");
  }

  private static Map<?, ?> mapOf(Object value, String field) throws AtClientConfigException {
    if (value instanceof Map) {
      return (Map<?, ?>) value;
    }
    throw new AtClientConfigException("AtKeys json field " + field + " holds an entry that is not an object");
  }

  private static Map<String, String> flatFields(Map<String, Object> document) throws AtClientConfigException {
    Map<String, String> map = new TreeMap<>();
    for (String field : FLAT_FIELDS) {
      Object value = document.get(field);
      if (value instanceof String) {
        map.put(field, (String) value);
      } else if (value != null) {
        throw new AtClientConfigException("AtKeys json field " + field + " is not a string");
      }
    }
    return map;
  }

  private static AtKeys createAtKeysVersion1(Map<String, String> map) throws AtDecryptionException {
    String selfEncryptKey = mapGet(map, SELF_ENCRYPT_KEY);
    return AtKeys.builder()
        .selfEncryptKey(selfEncryptKey)
        .enrollmentId(mapGetEnrollmentId(map, ENROLLMENT_ID))
        .apkamSymmetricKey(mapGet(map, APKAM_SYMMETRIC_KEY))
        .apkamPublicKey(mapGetDecrypted(map, PKAM_PUBLIC_KEY, selfEncryptKey))
        .apkamPrivateKey(mapGetDecrypted(map, PKAM_PRIVATE_KEY, selfEncryptKey))
        .encryptPublicKey(mapGetDecrypted(map, ENCRYPT_PUBLIC_KEY, selfEncryptKey))
        .encryptPrivateKey(mapGetDecrypted(map, ENCRYPT_PRIVATE_KEY, selfEncryptKey))
        .build();
  }

  private static void mapPut(Map<String, String> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private static void mapPut(Map<String, String> map, String key, TypedString value) {
    if (value != null) {
      map.put(key, value.toString());
    }
  }

  private static void mapPutEncrypted(Map<String, String> map, String key, String value, String encryptKey)
      throws Exception {
    if (value != null) {
      map.put(key, aesEncryptToBase64(value, encryptKey, EMPTY_IV));
    }
  }

  private static EnrollmentId mapGetEnrollmentId(Map<String, String> map, String key) {
    return createEnrollmentId(map.get(key));
  }

  private static String mapGet(Map<String, String> map, String key) {
    return map.get(key);
  }

  private static String mapGetDecrypted(Map<String, String> map, String key, String decryptKey)
      throws AtDecryptionException {
    String value = map.get(key);
    return value != null ? aesDecryptFromBase64(value, decryptKey, EMPTY_IV) : null;
  }

  public static String dump(AtKeys keys) {
    StringBuilder builder = new StringBuilder();
    builderAppend(builder, ENROLLMENT_ID, keys.getEnrollmentId());
    builderAppend(builder, PKAM_PUBLIC_KEY, keys.getApkamPublicKey());
    builderAppend(builder, PKAM_PRIVATE_KEY, keys.getApkamPrivateKey());
    builderAppend(builder, ENCRYPT_PUBLIC_KEY, keys.getEncryptPublicKey());
    builderAppend(builder, ENCRYPT_PRIVATE_KEY, keys.getEncryptPrivateKey());
    builderAppend(builder, APKAM_SYMMETRIC_KEY, keys.getApkamSymmetricKey());
    builderAppend(builder, SELF_ENCRYPT_KEY, keys.getSelfEncryptKey());
    for (Map.Entry<String, String> entry : keys.getCache().entrySet()) {
      builderAppend(builder, entry.getKey(), entry.getValue());
    }
    return builder.toString();
  }

  private static void builderAppend(StringBuilder builder, String key, Object value) {
    if (value != null) {
      builder.append("\tkey: ").append(key).append("\n\t\tvalue: ").append(value).append("\n");
    }
  }
}
