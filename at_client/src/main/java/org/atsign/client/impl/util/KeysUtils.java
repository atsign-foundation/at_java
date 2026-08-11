package org.atsign.client.impl.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.atsign.client.impl.util.EncryptionUtils.aesDecryptFromBase64;
import static org.atsign.client.impl.util.EncryptionUtils.aesEncryptToBase64;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.CryptographicMaterial;
import org.atsign.client.api.CryptographicMaterial.Role;
import org.atsign.client.api.EnrollmentId;
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

  /**
   * A symmetric key that encrypts other keys rather than data, so that it is distinguishable from a
   * symmetric data encryption key. The apkam symmetric key is one: it wraps the self encryption key
   * and the private encryption key for the duration of an enrollment. NIST SP 800-57 separates a
   * symmetric key-wrapping key from a symmetric data encryption key, and JWK separates wrapKey from
   * encrypt, so the distinction is a standard one that the shared role vocabulary is missing.
   * <p>
   * Held here rather than alongside the other roles until the token is agreed across SDKs.
   */
  public static final Role SYMMETRIC_KEY_WRAPPING = Role.of("symmetricKeyWrapping");

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static final DateTimeFormatter ISO_MICROS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

  /**
   * Decoding reads into Object values rather than Strings so that a document carrying anything other
   * than a String — a typed 'keys' array, or an integer version — still binds. Values are taken as
   * their toString(), so an integer version reads the same as the String form.
   */
  private static final TypeReference<Map<String, Object>> ANY_MAP_TYPE = new TypeReference<>() {};

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
  private static final int VERSION_1 = 1;
  private static final String VERSION_1_TOKEN = String.valueOf(VERSION_1);
  private static final String ATSIGN_KEY = "atsign";
  private static final String KEYS_KEY = "keys";
  private static final String KEY_ID = "keyId";
  private static final String KEY_PARTS = "keyParts";
  private static final String KEY_PART_TYPE = "keyPartType";
  private static final String KEY_ALGORITHM_TYPE = "keyAlgorithmType";
  private static final String OPERATIONS = "operations";
  private static final String CREATED_AT = "createdAt";
  private static final String STATUS = "status";
  private static final String BYTES = "bytes";
  private static final String PKAM_PUBLIC_KEY = "aesPkamPublicKey";
  private static final String PKAM_PRIVATE_KEY = "aesPkamPrivateKey";
  private static final String ENCRYPT_PUBLIC_KEY = "aesEncryptPublicKey";
  private static final String ENCRYPT_PRIVATE_KEY = "aesEncryptPrivateKey";
  private static final String SELF_ENCRYPT_KEY = "selfEncryptionKey";
  private static final String APKAM_SYMMETRIC_KEY = "apkamSymmetricKey";
  private static final String ENROLLMENT_ID = "enrollmentId";

  /**
   * Persists {@link AtKeys} to the default file for the {@link AtSign}.
   *
   * @param atSign The {@link AtSign} which these keys relate to.
   * @param keys The {@link AtKeys} to persist.
   * @throws Exception If anything fails.
   */
  public static void saveKeys(AtSign atSign, AtKeys keys) throws Exception {
    saveKeys(keys, getKeysFile(atSign), atSign);
  }

  /**
   * Persists {@link AtKeys} to a file, without recording which {@link AtSign} they belong to.
   *
   * @param keys The {@link AtKeys} to persist.
   * @param file The file to write / overwrite.
   * @throws IOException If anything fails.
   */
  public static void saveKeys(AtKeys keys, File file) throws IOException {
    saveKeys(keys, file, null);
  }

  /**
   * Persists {@link AtKeys} to a file, recording the {@link AtSign} they belong to.
   *
   * @param keys The {@link AtKeys} to persist.
   * @param file The file to write / overwrite.
   * @param atSign The {@link AtSign} these keys belong to, or null when it is not known. Supplying it
   *        is what allows the typed document to be written, since that shape requires it.
   * @throws IOException If anything fails.
   * @throws IllegalArgumentException If {@code keys} already names a different {@link AtSign}.
   */
  public static void saveKeys(AtKeys keys, File file, AtSign atSign) throws IOException {
    if (file.getParentFile() != null && !file.getParentFile().exists()) {
      Files.createDirectories(file.getParentFile().toPath());
    }
    log.info("Saving keys to {}", file.getAbsolutePath());

    Files.write(file.toPath(), getAsJson(keys, atSign).getBytes(UTF_8));
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
   * Instantiates a {@link AtKeys} loaded with the contents of a keys file.
   *
   * @param file The file to read.
   * @return A populated {@link AtKeys} instance.
   * @throws AtClientConfigException If anything fails or the keys file does not exist.
   */
  public static AtKeys loadKeys(File file) throws AtClientConfigException {
    try {
      return createAtKeysFromJson(Files.readString(file.toPath()));
    } catch (IOException e) {
      throw new AtClientConfigException("failed to read " + file, e);
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

  private static String getAsJson(AtKeys keys, AtSign atSign) {
    AtSign owner = resolveOwner(keys, atSign);

    // Typed material can only be written inside a typed document, which needs an atsign. Writing the
    // legacy shape instead would silently drop it, so refuse rather than lose it. Mirrors the same
    // refusal in the canonical SDK.
    if (owner == null && !keys.getAllCryptographicMaterial().isEmpty()) {
      throw new IllegalArgumentException(
          "an atSign is required to write key material that is not one of the legacy fields");
    }
    try {
      Map<String, Object> map = new TreeMap<>();

      mapPut(map, SELF_ENCRYPT_KEY, keys.getSelfEncryptKey());
      mapPut(map, ENROLLMENT_ID, keys.getEnrollmentId());
      mapPut(map, APKAM_SYMMETRIC_KEY, keys.getApkamSymmetricKey());

      mapPutEncrypted(map, PKAM_PUBLIC_KEY, keys.getApkamPublicKey(), keys.getSelfEncryptKey());
      mapPutEncrypted(map, PKAM_PRIVATE_KEY, keys.getApkamPrivateKey(), keys.getSelfEncryptKey());
      mapPutEncrypted(map, ENCRYPT_PUBLIC_KEY, keys.getEncryptPublicKey(), keys.getSelfEncryptKey());
      mapPutEncrypted(map, ENCRYPT_PRIVATE_KEY, keys.getEncryptPrivateKey(), keys.getSelfEncryptKey());

      // version, atsign and keys are one set, not three options: other SDKs take the typed branch as
      // soon as they see a version and then require the other two. With no atSign to write we emit
      // the legacy flat shape instead, which they read through their legacy branch.
      if (owner != null) {
        map.put(VERSION_KEY, VERSION_1);
        map.put(ATSIGN_KEY, owner.toString());
        map.put(KEYS_KEY, encodeKeys(keys.getAllCryptographicMaterial()));
      }

      return JsonUtils.writeValueAsString(map, true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static AtKeys createAtKeysFromJson(String json) throws AtClientConfigException {
    try {
      Map<String, Object> map = JsonUtils.readValue(json, ANY_MAP_TYPE);
      String version = mapGet(map, VERSION_KEY);
      if (version == null || version.equals(VERSION_1_TOKEN)) {
        return createAtKeysVersion1(map);
      } else {
        throw new AtClientConfigException("unsupported version of AtKeys json : " + version);
      }
    } catch (AtDecryptionException e) {
      throw new AtClientConfigException("failed to create AtKeys from json", e);
    }
  }

  private static AtKeys createAtKeysVersion1(Map<String, Object> map) throws AtDecryptionException {
    Object typed = map.get(KEYS_KEY);
    if (typed instanceof Collection && !((Collection<?>) typed).isEmpty()) {
      log.warn("ignoring {} typed key material entries: decoding them is not implemented yet",
               ((Collection<?>) typed).size());
    }
    String selfEncryptKey = mapGet(map, SELF_ENCRYPT_KEY);
    AtKeys.AtKeysBuilder builder = AtKeys.builder()
        .selfEncryptKey(selfEncryptKey)
        .apkamSymmetricKey(mapGet(map, APKAM_SYMMETRIC_KEY))
        .apkamPublicKey(mapGetDecrypted(map, PKAM_PUBLIC_KEY, selfEncryptKey))
        .apkamPrivateKey(mapGetDecrypted(map, PKAM_PRIVATE_KEY, selfEncryptKey))
        .encryptPublicKey(mapGetDecrypted(map, ENCRYPT_PUBLIC_KEY, selfEncryptKey))
        .encryptPrivateKey(mapGetDecrypted(map, ENCRYPT_PRIVATE_KEY, selfEncryptKey));

    // Last: the enrollment id is stamped onto whatever material is already present, so every
    // other setter has to have run first.
    EnrollmentId enrollmentId = mapGetEnrollmentId(map, ENROLLMENT_ID);
    if (enrollmentId != null) {
      builder.enrollmentId(enrollmentId);
    }
    return builder.build();
  }

  private static void mapPut(Map<String, Object> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private static void mapPut(Map<String, Object> map, String key, TypedString value) {
    if (value != null) {
      map.put(key, value.toString());
    }
  }

  private static void mapPutEncrypted(Map<String, Object> map, String key, String value, String encryptKey)
      throws Exception {
    if (value != null) {
      map.put(key, aesEncryptToBase64(value, encryptKey, EMPTY_IV));
    }
  }

  /**
   * Decides which {@link AtSign} the written document should name. A caller-supplied one fills in for
   * keys that do not carry their own, but the two must agree — persisting one atSign's keys under
   * another's name is refused rather than silently resolved either way.
   */
  private static AtSign resolveOwner(AtKeys keys, AtSign atSign) {
    if (atSign == null) {
      return keys.getAtSign();
    }
    if (keys.getAtSign() != null && !keys.getAtSign().equals(atSign)) {
      throw new IllegalArgumentException(
          "AtKeys belong to " + keys.getAtSign() + " but are being persisted for " + atSign);
    }
    return atSign;
  }

  /**
   * Encodes material into the typed 'keys' array, grouping by key id so that the halves of one
   * keypair share an entry.
   */
  private static List<Map<String, Object>> encodeKeys(Collection<CryptographicMaterial> materials) {
    Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
    for (CryptographicMaterial material : materials) {
      Map<String, Object> group = groups.computeIfAbsent(material.getKeyId().toString(), keyId -> {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(KEY_ID, keyId);
        mapPut(entry, ENROLLMENT_ID, material.getEnrollmentId());
        entry.put(KEY_PARTS, new ArrayList<Map<String, Object>>());
        return entry;
      });
      keyParts(group).add(encodeKeyPart(material));
    }
    return new ArrayList<>(groups.values());
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> keyParts(Map<String, Object> group) {
    return (List<Map<String, Object>>) group.get(KEY_PARTS);
  }

  private static Map<String, Object> encodeKeyPart(CryptographicMaterial material) {
    Map<String, Object> part = new LinkedHashMap<>();
    part.put(KEY_PART_TYPE, material.getRole().toString());
    part.put(KEY_ALGORITHM_TYPE, material.getAlgorithm().toString());
    if (!material.getOperations().isEmpty()) {
      List<String> operations = new ArrayList<>();
      material.getOperations().forEach(operation -> operations.add(operation.toString()));
      part.put(OPERATIONS, operations);
    }
    part.put(CREATED_AT, formatCreatedAt(material.getCreatedAt()));
    part.put(STATUS, material.getStatus().name());
    part.put(BYTES, material.getBytes().base64());
    return part;
  }

  /**
   * Formats as other SDKs do: always UTC, three fractional digits, or six when the value carries
   * sub-millisecond precision. Java's own OffsetDateTime.toString() drops zero-valued fields, which
   * would not round-trip byte for byte.
   */
  private static String formatCreatedAt(OffsetDateTime createdAt) {
    OffsetDateTime utc = createdAt.withOffsetSameInstant(ZoneOffset.UTC);
    return utc.getNano() % 1_000_000 == 0 ? ISO_MILLIS.format(utc) : ISO_MICROS.format(utc);
  }

  private static EnrollmentId mapGetEnrollmentId(Map<String, Object> map, String key) {
    return EnrollmentId.of(mapGet(map, key));
  }

  private static String mapGet(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? null : value.toString();
  }

  private static String mapGetDecrypted(Map<String, Object> map, String key, String decryptKey)
      throws AtDecryptionException {
    String value = mapGet(map, key);
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
