package org.atsign.client.impl.util;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.impl.util.EncryptionUtils.generateAESKeyBase64;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.atsign.client.impl.util.EncryptionUtils.toStringBase64;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;

public class KeysUtilsTest {

  AtSign testAtSign = createAtSign("@testSaveKeysFile");

  @AfterEach
  public void tearDown() throws IOException {
    Files.deleteIfExists(KeysUtils.getKeysFile(testAtSign, KeysUtils.expectedKeysFilesLocation).toPath());
    Files.deleteIfExists(KeysUtils.getKeysFile(testAtSign, KeysUtils.legacyKeysFilesLocation).toPath());
  }

  @Test
  public void testSaveKeysFile() throws Exception {
    File expected = KeysUtils.getKeysFile(testAtSign, KeysUtils.expectedKeysFilesLocation);
    assertFalse(expected.exists());

    // Given a Map of keys (like Onboard creates)
    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .apkamKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();

    // When we call KeysUtil.saveKeys
    KeysUtils.saveKeys(testAtSign, keys);

    // Then we end up with a file with the expected name in the expected (canonical) location
    assertTrue(expected.exists());
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
    assertTrue(expected.exists());

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
    assertFalse(expected.exists());

    // When we call KeysUtil.loadKeys
    // Then the keys are loaded successfully from the legacy location
    AtKeys loadedKeys = KeysUtils.loadKeys(testAtSign);
    assertContentsMatch(keys, loadedKeys);
  }

  @Test
  public void savedKeysFileIsTheLegacyFlatShape(@TempDir Path dir) throws Exception {
    File file = dir.resolve("keys.atKeys").toFile();

    KeysUtils.saveKeys(newKeys(), file);

    // NOTE the legacy .atKeys shape at_auth reads. A "version" field makes at_auth 3.3.0 and
    // later read the file as its typed keys document and refuse it.
    assertEquals(
                 Set.of("aesEncryptPrivateKey", "aesEncryptPublicKey", "aesPkamPrivateKey", "aesPkamPublicKey",
                        "selfEncryptionKey"),
                 readJson(file).keySet());
  }

  @Test
  public void loadsAFileWrittenWithAStringVersion(@TempDir Path dir) throws Exception {
    AtKeys keys = newKeys();
    File file = writeKeysFile(keys, dir, Map.of("version", "1"));

    assertContentsMatch(keys, KeysUtils.loadKeys(file));
  }

  @Test
  public void loadsTheFlatFieldsOfATypedDocumentThatAuthenticatesThroughThem(@TempDir Path dir) throws Exception {
    AtKeys keys = newEnrolledKeys();
    File file = writeKeysFile(keys, dir, typedAtSignKeysDocument());

    assertContentsMatch(keys, KeysUtils.loadKeys(file));
  }

  @Test
  public void refusesATypedDocumentThatAuthenticatesThroughTypedMaterial(@TempDir Path dir) throws Exception {
    File file = writeKeysFile(newEnrolledKeys(), dir, typedAuthenticationDocument("active"));

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("typed-enrollment-2"), e.getMessage());
  }

  @Test
  public void loadsATypedDocumentWhoseTypedAuthenticationIsNotActive(@TempDir Path dir) throws Exception {
    AtKeys keys = newEnrolledKeys();
    File file = writeKeysFile(keys, dir, typedAuthenticationDocument("pending"));

    assertContentsMatch(keys, KeysUtils.loadKeys(file));
  }

  @Test
  public void refusesAnUnsupportedVersion(@TempDir Path dir) throws Exception {
    File file = writeKeysFile(newKeys(), dir, Map.of("version", 2));

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("unsupported version"), e.getMessage());
  }

  @Test
  public void refusesAFlatFieldThatIsNotAString(@TempDir Path dir) throws Exception {
    File file = writeKeysFile(newKeys(), dir, Map.of("selfEncryptionKey", List.of("not", "a", "string")));

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("selfEncryptionKey"), e.getMessage());
  }

  @Test
  public void refusesATypedDocumentWhoseEnrollmentsAreNotAList(@TempDir Path dir) throws Exception {
    File file = writeKeysFile(newEnrolledKeys(), dir,
                              typedDocument("enrollments", Map.of("enrollmentId", "typed-enrollment-2")));

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("enrollments"), e.getMessage());
  }

  @Test
  public void refusesAFileThatIsNotAJsonObject(@TempDir Path dir) throws Exception {
    File file = dir.resolve("keys.atKeys").toFile();
    Files.writeString(file.toPath(), "[]");

    assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));
  }

  @Test
  public void saveKeysRefusesToOverwriteATypedDocument(@TempDir Path dir) throws Exception {
    File file = writeKeysFile(newEnrolledKeys(), dir, typedAtSignKeysDocument());
    byte[] before = Files.readAllBytes(file.toPath());

    IOException e = assertThrows(IOException.class, () -> KeysUtils.saveKeys(newKeys(), file));

    assertTrue(e.getMessage().contains("typed keys document"), e.getMessage());
    assertArrayEquals(before, Files.readAllBytes(file.toPath()));
  }

  @Test
  public void saveKeysOverwritesALegacyFile(@TempDir Path dir) throws Exception {
    File file = writeKeysFile(newKeys(), dir, Map.of("version", "1"));
    AtKeys keys = newKeys();

    KeysUtils.saveKeys(keys, file);

    assertContentsMatch(keys, KeysUtils.loadKeys(file));
    assertFalse(readJson(file).containsKey("version"));
  }

  private static AtKeys newKeys() throws Exception {
    return AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .apkamKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();
  }

  private static AtKeys newEnrolledKeys() throws Exception {
    return newKeys().toBuilder()
        .enrollmentId(createEnrollmentId("flat-enrollment-1"))
        .apkamSymmetricKey(generateAESKeyBase64())
        .build();
  }

  private static File writeKeysFile(AtKeys keys, Path dir, Map<String, Object> fields) throws Exception {
    File file = dir.resolve("keys.atKeys").toFile();
    KeysUtils.saveKeys(keys, file);
    Map<String, Object> json = readJson(file);
    json.putAll(fields);
    writeJson(file, json);
    return file;
  }

  // NOTE the typed keys document at_auth 4.0.0-rc2 writes beside the flat fields. These spellings are
  // at_auth's, so they stay raw literals rather than references to KeysUtils.
  private static Map<String, Object> typedDocument(String container, Object entries) {
    return Map.of("version", 1, "atsign", "@alice", "keys", List.of(), container, entries);
  }

  private static Map<String, Object> typedAtSignKeysDocument() throws Exception {
    return typedDocument("atsignKeys",
                         List.of(typedKey("root:rsa2048:1", "privateSigning", "publicVerification", "active")));
  }

  private static Map<String, Object> typedAuthenticationDocument(String status) throws Exception {
    return typedDocument("enrollments",
                         List.of(Map.of("enrollmentId", "typed-enrollment-2",
                                        "keys", List.of(typedKey("auth:rsa2048:1", "privateAuthentication",
                                                                 "publicAuthentication", status)))));
  }

  private static Map<String, Object> typedKey(String keyId, String privateRole, String publicRole, String status)
      throws Exception {
    KeyPair keyPair = generateRSAKeyPair();
    return Map.of("keyId", keyId,
                  "material", List.of(typedMaterial(privateRole, keyPair.getPrivate(), status),
                                      typedMaterial(publicRole, keyPair.getPublic(), status)));
  }

  private static Map<String, Object> typedMaterial(String role, Key key, String status) {
    return Map.of("role", role, "algorithm", "rsa2048", "createdAt", Instant.now().toString(), "status", status,
                  "bytes", toStringBase64(key));
  }

  private static Map<String, Object> readJson(File file) throws IOException {
    return JsonUtils.readValue(Files.readString(file.toPath()), new TypeReference<Map<String, Object>>() {});
  }

  private static void writeJson(File file, Map<String, Object> json) throws IOException {
    Files.writeString(file.toPath(), JsonUtils.writeValueAsString(json));
  }

  private static void assertContentsMatch(AtKeys keys1, AtKeys keys2) {
    assertEquals(keys1.getEnrollmentId(), keys2.getEnrollmentId());
    assertEquals(keys1.getApkamPublicKey(), keys2.getApkamPublicKey());
    assertEquals(keys1.getApkamPrivateKey(), keys2.getApkamPrivateKey());
    assertEquals(keys1.getEncryptPublicKey(), keys2.getEncryptPublicKey());
    assertEquals(keys1.getEncryptPrivateKey(), keys2.getEncryptPrivateKey());
    assertEquals(keys1.getSelfEncryptKey(), keys2.getSelfEncryptKey());
    assertEquals(keys1.getApkamSymmetricKey(), keys2.getApkamSymmetricKey());
  }
}
