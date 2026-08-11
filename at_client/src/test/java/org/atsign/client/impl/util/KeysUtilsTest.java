package org.atsign.client.impl.util;

import static java.util.Arrays.asList;
import static org.atsign.client.impl.util.EncryptionUtils.aesDecryptFromBase64;
import static org.atsign.client.impl.util.EncryptionUtils.generateAESKeyBase64;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.CryptographicMaterial;
import org.atsign.client.api.CryptographicMaterial.Algorithm;
import org.atsign.client.api.CryptographicMaterial.Role;
import org.atsign.client.api.EnrollmentId;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;

@SuppressWarnings("deprecation")
public class KeysUtilsTest {

  /**
   * The at-rest field names. Declared here rather than reused from KeysUtils so that these tests
   * pin the wire format independently of the code that produces it.
   */
  private static final String VERSION = "version";
  private static final String PKAM_PUBLIC_KEY = "aesPkamPublicKey";
  private static final String PKAM_PRIVATE_KEY = "aesPkamPrivateKey";
  private static final String ENCRYPT_PUBLIC_KEY = "aesEncryptPublicKey";
  private static final String ENCRYPT_PRIVATE_KEY = "aesEncryptPrivateKey";
  private static final String SELF_ENCRYPT_KEY = "selfEncryptionKey";
  private static final String APKAM_SYMMETRIC_KEY = "apkamSymmetricKey";
  private static final String ENROLLMENT_ID = "enrollmentId";

  /** The four self-encrypted fields use an all-zero 16 byte IV; both SDKs depend on that. */
  private static final String EMPTY_IV = Base64.getEncoder().encodeToString(new byte[16]);

  private static final TypeReference<Map<String, Object>> ANY_MAP = new TypeReference<>() {};

  AtSign testAtSign = AtSign.of("@testSaveKeysFile");

  @TempDir
  Path tempDir;

  @AfterEach
  public void tearDown() throws IOException {
    Files.deleteIfExists(KeysUtils.getKeysFile(testAtSign, KeysUtils.expectedKeysFilesLocation).toPath());
    Files.deleteIfExists(KeysUtils.getKeysFile(testAtSign, KeysUtils.legacyKeysFilesLocation).toPath());
  }

  @Test
  public void testSaveKeysFile() throws Exception {
    File expected = KeysUtils.getKeysFile(testAtSign, KeysUtils.expectedKeysFilesLocation);
    assertThat(expected, not(anExistingFile()));

    // Given a Map of keys (like Onboard creates)
    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .apkamKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();

    // When we call KeysUtil.saveKeys
    KeysUtils.saveKeys(testAtSign, keys);

    // Then we end up with a file with the expected name in the expected (canonical) location
    assertThat(expected, anExistingFile());
  }

  @Test
  public void testLoadKeysFile() throws Exception {
    // Given a correctly formatted keys file in the canonical location
    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .apkamKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();
    KeysUtils.saveKeys(testAtSign, keys);

    // When we call KeysUtil.loadKeys
    AtKeys loadedKeys = KeysUtils.loadKeys(testAtSign);

    // Then the keys are loaded successfully
    assertContentsMatch(keys, loadedKeys);
  }

  @Test
  public void testLoadKeysFileLegacy() throws Exception {
    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .apkamKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();

    KeysUtils.saveKeys(testAtSign, keys);
    File expected = KeysUtils.getKeysFile(testAtSign, KeysUtils.expectedKeysFilesLocation);
    assertThat(expected, anExistingFile());

    // Given a correctly formatted keys file in the legacy location
    // And there is NOT a keys file in the canonical location

    // So, in order to set up the "Given" pre-conditions above, we'll need to
    // 1) move the generated file to the legacy location
    File file = new File(KeysUtils.legacyKeysFilesLocation);
    file.deleteOnExit();
    Files.createDirectories(file.toPath());
    Files.move(expected.toPath(), KeysUtils.getKeysFile(testAtSign, KeysUtils.legacyKeysFilesLocation).toPath());

    // 2) delete the file we just generated in the expected location
    Path expectedCanonicalFilePath = KeysUtils.getKeysFile(testAtSign, KeysUtils.expectedKeysFilesLocation).toPath();
    Files.deleteIfExists(expectedCanonicalFilePath);

    // Ensure the file does not exist in the expected place
    assertThat(expected, not(anExistingFile()));

    // When we call KeysUtil.loadKeys
    // Then the keys are loaded successfully from the legacy location
    AtKeys loadedKeys = KeysUtils.loadKeys(testAtSign);
    assertContentsMatch(keys, loadedKeys);
  }

  @Test
  public void testEncodeEmitsTheLegacyFlatShape() throws Exception {
    Map<String, Object> encoded = encode(fullKeys());

    List<String> expected = asList(
                                   PKAM_PUBLIC_KEY, PKAM_PRIVATE_KEY,
                                   ENCRYPT_PUBLIC_KEY,
                                   ENCRYPT_PRIVATE_KEY, SELF_ENCRYPT_KEY,
                                   APKAM_SYMMETRIC_KEY, ENROLLMENT_ID);
    assertThat(encoded.keySet(), hasItems(expected.toArray(new String[0])));
    assertThat(encoded, not(hasKey(VERSION)));
  }

  /**
   * Without an atSign there is no complete typed document to write, so no version is written either
   * — a version with no atsign and no keys alongside it is what other SDKs reject.
   */
  @Test
  public void testEncodeOmitsVersionWithoutAnAtSign() throws Exception {
    assertThat(encode(fullKeys()), not(hasKey(VERSION)));
  }

  /**
   * With an atSign the typed document is written: an integer version, the atsign, and a keys array.
   */
  @Test
  public void testEncodeWritesATypedDocumentWithAnAtSign() throws Exception {
    Map<String, Object> encoded = encode(fullKeys().toBuilder().atSign("@alice").build());

    assertThat(encoded.get(VERSION), equalTo(1));
    assertThat(encoded.get("atsign"), equalTo("@alice"));
    assertThat(encoded.get("keys"), equalTo(List.of()));
  }

  /**
   * A caller-supplied atSign fills in for keys that do not carry one, which is how a legacy keyfile
   * becomes a typed document.
   */
  @Test
  public void testEncodeTakesTheAtSignFromTheCaller() throws Exception {
    File file = tempDir.resolve("supplied.atKeys").toFile();
    KeysUtils.saveKeys(fullKeys(), file, AtSign.of("@alice"));

    assertThat(readJson(file).get("atsign"), equalTo("@alice"));
  }

  /**
   * Mirrors at_auth's refusal to persist one atSign's keys under another's name; neither side wins.
   */
  @Test
  public void testEncodeRefusesAnAtSignThatDisagreesWithTheKeys() throws Exception {
    AtKeys keys = fullKeys().toBuilder().atSign("@alice").build();
    File file = tempDir.resolve("conflict.atKeys").toFile();

    assertThrows(IllegalArgumentException.class,
                 () -> KeysUtils.saveKeys(keys, file, AtSign.of("@bob")));
  }

  /**
   * Typed material cannot be written into the legacy shape, so writing it without an atSign is
   * refused rather than losing it.
   */
  @Test
  public void testEncodeRefusesTypedMaterialWithoutAnAtSign() throws Exception {
    AtKeys keys = fullKeys().withKey(CryptographicMaterial.builder()
        .keyId("rotated")
        .role(Role.symmetricEncryption)
        .algorithm(Algorithm.aes256)
        .bytes("YXBwZW5kZWQ=")
        .createdAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
        .build());
    File file = tempDir.resolve("orphan.atKeys").toFile();

    assertThrows(IllegalArgumentException.class, () -> KeysUtils.saveKeys(keys, file));
  }

  /**
   * Typed material is grouped by key id under 'keys', and createdAt is written with the three
   * fractional digits other SDKs use rather than Java's shorter default rendering.
   */
  @Test
  public void testEncodeWritesTypedMaterialIntoTheKeysArray() throws Exception {
    AtKeys keys = fullKeys().toBuilder().atSign("@alice").build()
        .withKey(CryptographicMaterial.builder()
            .keyId("rotated")
            .role(Role.symmetricEncryption)
            .algorithm(Algorithm.aes256)
            .bytes("YXBwZW5kZWQ=")
            .createdAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
            .build());

    Map<String, Object> entry = firstMap(encode(keys).get("keys"));
    Map<String, Object> part = firstMap(entry.get("keyParts"));

    assertThat(entry.get("keyId"), equalTo("rotated"));
    assertThat(part.get("keyPartType"), equalTo("symmetricEncryption"));
    assertThat(part.get("keyAlgorithmType"), equalTo("aes256"));
    assertThat(part.get("createdAt"), equalTo("2024-01-01T00:00:00.000Z"));
    assertThat(part.get("status"), equalTo("active"));
    assertThat(part.get("bytes"), equalTo("YXBwZW5kZWQ="));
    assertThat(part, not(hasKey("operations")));
  }

  /**
   * Exactly four fields are self-encrypted, with an all-zero IV; the rest are plaintext.
   */
  @Test
  public void testEncodeSelfEncryptsExactlyFourFields() throws Exception {
    AtKeys keys = fullKeys();
    Map<String, Object> encoded = encode(keys);
    String selfKey = keys.getSelfEncryptKey();

    assertEncrypted(encoded, PKAM_PUBLIC_KEY, keys.getApkamPublicKey(), selfKey);
    assertEncrypted(encoded, PKAM_PRIVATE_KEY, keys.getApkamPrivateKey(), selfKey);
    assertEncrypted(encoded, ENCRYPT_PUBLIC_KEY, keys.getEncryptPublicKey(), selfKey);
    assertEncrypted(encoded, ENCRYPT_PRIVATE_KEY, keys.getEncryptPrivateKey(), selfKey);

    assertThat(encoded.get(SELF_ENCRYPT_KEY), equalTo(selfKey));
    assertThat(encoded.get(APKAM_SYMMETRIC_KEY), equalTo(keys.getApkamSymmetricKey()));
    assertThat(encoded.get(ENROLLMENT_ID), equalTo(keys.getEnrollmentId().toString()));
  }

  /**
   * at_auth's 'toJson -> legacy fields are nullable' emits every field with a null value. Java omits
   * absent fields instead, so an empty AtKeys yields nothing at all.
   */
  @Test
  public void testEncodeOmitsAbsentFields() throws Exception {
    assertThat(encode(AtKeys.builder().build()), aMapWithSize(0));
  }

  /**
   * Mirrors 'FileAtKeysIo round-trips legacy AtKeys'.
   */
  @Test
  public void testEncodeDecodeRoundTripPreservesEveryField() throws Exception {
    AtKeys keys = fullKeys();
    File file = tempDir.resolve("roundtrip.atKeys").toFile();

    KeysUtils.saveKeys(keys, file);

    assertContentsMatch(keys, KeysUtils.loadKeys(file));
  }

  /**
   * Mirrors 'fromJson falls back to legacy for json without a version field'.
   */
  @Test
  public void testDecodeAcceptsJsonWithNoVersionField() throws Exception {
    AtKeys keys = fullKeys();
    Map<String, Object> encoded = encode(keys);
    encoded.remove(VERSION);

    assertContentsMatch(keys, KeysUtils.loadKeys(writeJson("noversion.atKeys", encoded)));
  }

  /**
   * at_auth writes the version as an integer. Jackson coerces it to a String on the way in, so a
   * Dart-written version alone does not stop Java reading the file.
   */
  @Test
  public void testDecodeAcceptsAnIntegerVersion() throws Exception {
    AtKeys keys = fullKeys();
    Map<String, Object> encoded = encode(keys);
    encoded.put(VERSION, 1);

    assertContentsMatch(keys, KeysUtils.loadKeys(writeJson("intversion.atKeys", encoded)));
  }

  /**
   * Mirrors 'fromJson throws on an unsupported version', for both the string and integer forms.
   */
  @Test
  public void testDecodeRejectsAnUnsupportedVersion() throws Exception {
    Map<String, Object> asString = encode(fullKeys());
    asString.put(VERSION, "2");
    File stringFile = writeJson("v2string.atKeys", asString);
    assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(stringFile));

    Map<String, Object> asInt = encode(fullKeys());
    asInt.put(VERSION, 2);
    File intFile = writeJson("v2int.atKeys", asInt);
    assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(intFile));
  }

  /**
   * at_auth keeps unrecognised top-level keys in AtKeys.metadata and writes them back
   * ('toJson -> passes legacy metadata through'). We deliberately do not, so they are discarded on
   * write — an accepted difference, not a gap to close.
   * <p>
   * It is safe for the one such key that occurs in practice: the historic '@atsign' trailer written
   * by older clients holds a byte-for-byte copy of selfEncryptionKey, so nothing is lost with it.
   * Note the other SDK cannot detect the loss either way, since its never-lose check only requires
   * that what is already in the file survives, and by then the key has gone.
   */
  @Test
  public void testUnknownTopLevelKeysAreDiscarded() throws Exception {
    AtKeys keys = fullKeys();
    Map<String, Object> encoded = encode(keys);
    encoded.put("@alice", keys.getSelfEncryptKey());

    File file = writeJson("trailer.atKeys", encoded);
    AtKeys loaded = KeysUtils.loadKeys(file);
    KeysUtils.saveKeys(loaded, file);

    assertThat(readJson(file), not(hasKey("@alice")));
  }

  /**
   * A typed at_auth document carries an 'atsign' string and a 'keys' array alongside the flat
   * fields. Neither is modelled here yet, but both must bind, so that the legacy payload of a
   * Dart-written keyfile still loads.
   */
  @Test
  public void testDecodeAcceptsATypedDocumentAndReadsItsLegacyFields() throws Exception {
    AtKeys keys = fullKeys();
    Map<String, Object> encoded = encode(keys);
    encoded.put(VERSION, 1);
    encoded.put("atsign", "@alice");
    Map<String, Object> keyPart = Map.of(
                                         "keyPartType", "symmetricEncryption",
                                         "keyAlgorithmType", "aes256",
                                         "createdAt", "2024-01-01T00:00:00.000Z",
                                         "status", "active",
                                         "bytes", "YXBwZW5kZWQ=");
    encoded.put("keys", asList(Map.of("keyId", "rotated", "keyParts", asList(keyPart))));

    assertContentsMatch(keys, KeysUtils.loadKeys(writeJson("typed.atKeys", encoded)));
  }

  private static AtKeys fullKeys() throws Exception {
    return AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .apkamKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .apkamSymmetricKey(generateAESKeyBase64())
        .enrollmentId(EnrollmentId.of("352b78c8-4b6f-4d07-a9cf-5466512ffa44"))
        .build();
  }

  private Map<String, Object> encode(AtKeys keys) throws Exception {
    File file = tempDir.resolve("encoded-" + System.nanoTime() + ".atKeys").toFile();
    KeysUtils.saveKeys(keys, file);
    return readJson(file);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> firstMap(Object jsonArray) {
    return (Map<String, Object>) ((List<?>) jsonArray).get(0);
  }

  private static Map<String, Object> readJson(File file) throws IOException {
    return JsonUtils.readValue(Files.readString(file.toPath()), ANY_MAP);
  }

  private File writeJson(String name, Map<String, Object> json) throws IOException {
    File file = tempDir.resolve(name).toFile();
    Files.write(file.toPath(), JsonUtils.writeValueAsString(json).getBytes(StandardCharsets.UTF_8));
    return file;
  }

  private static void assertEncrypted(Map<String, Object> encoded, String field, String plaintext,
                                      String selfKey)
      throws Exception {
    String atRest = (String) encoded.get(field);
    assertThat(field + " should be stored encrypted", atRest, not(equalTo(plaintext)));
    assertThat(aesDecryptFromBase64(atRest, selfKey, EMPTY_IV), equalTo(plaintext));
  }

  private static void assertContentsMatch(AtKeys expected, AtKeys actual) {
    assertThat(actual.getEnrollmentId(), equalTo(expected.getEnrollmentId()));
    assertThat(actual.getApkamPublicKey(), equalTo(expected.getApkamPublicKey()));
    assertThat(actual.getApkamPrivateKey(), equalTo(expected.getApkamPrivateKey()));
    assertThat(actual.getEncryptPublicKey(), equalTo(expected.getEncryptPublicKey()));
    assertThat(actual.getEncryptPrivateKey(), equalTo(expected.getEncryptPrivateKey()));
    assertThat(actual.getSelfEncryptKey(), equalTo(expected.getSelfEncryptKey()));
    assertThat(actual.getApkamSymmetricKey(), equalTo(expected.getApkamSymmetricKey()));
  }
}
