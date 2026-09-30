package org.atsign.client.impl.util;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.util.EncryptionUtils.generateAESKeyBase64;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    File file = copyFixture("at_java_string_version.atKeys", dir);

    AtKeys keys = KeysUtils.loadKeys(file);

    assertEquals(readJson(file).get("selfEncryptionKey"), keys.getSelfEncryptKey());
    assertNotNull(keys.getApkamPrivateKey());
    assertNotNull(keys.getEncryptPrivateKey());
  }

  @Test
  public void loadsTheFlatFieldsOfATypedDocumentThatAuthenticatesThroughThem(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_auth_4_0_0_rc2_typed_atsign_keys.atKeys", dir);

    AtKeys keys = KeysUtils.loadKeys(file);

    assertEquals("flat-enrollment-1", keys.getEnrollmentId().toString());
    assertEquals(readJson(file).get("selfEncryptionKey"), keys.getSelfEncryptKey());
    assertNotNull(keys.getApkamPrivateKey());
  }

  @Test
  public void refusesATypedDocumentThatAuthenticatesThroughTypedMaterial(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_auth_4_0_0_rc2_typed_authentication.atKeys", dir);

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("typed-enrollment-2"), e.getMessage());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void loadsATypedDocumentWhoseTypedAuthenticationIsNotActive(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_auth_4_0_0_rc2_typed_authentication.atKeys", dir);
    Map<String, Object> json = readJson(file);
    for (Object enrollment : (List<?>) json.get("enrollments")) {
      for (Object key : (List<?>) ((Map<?, ?>) enrollment).get("keys")) {
        for (Object material : (List<?>) ((Map<?, ?>) key).get("material")) {
          ((Map<String, Object>) material).put("status", "pending");
        }
      }
    }
    writeJson(file, json);

    AtKeys keys = KeysUtils.loadKeys(file);

    assertEquals("flat-enrollment-1", keys.getEnrollmentId().toString());
  }

  @Test
  public void refusesAnUnsupportedVersion(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_java_string_version.atKeys", dir);
    Map<String, Object> json = readJson(file);
    json.put("version", 2);
    writeJson(file, json);

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("unsupported version"), e.getMessage());
  }

  @Test
  public void refusesAFlatFieldThatIsNotAString(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_java_string_version.atKeys", dir);
    Map<String, Object> json = readJson(file);
    json.put("selfEncryptionKey", List.of("not", "a", "string"));
    writeJson(file, json);

    AtClientConfigException e = assertThrows(AtClientConfigException.class, () -> KeysUtils.loadKeys(file));

    assertTrue(e.getMessage().contains("selfEncryptionKey"), e.getMessage());
  }

  @Test
  public void refusesATypedDocumentWhoseEnrollmentsAreNotAList(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_auth_4_0_0_rc2_typed_authentication.atKeys", dir);
    Map<String, Object> json = readJson(file);
    json.put("enrollments", Map.of("enrollmentId", "typed-enrollment-2"));
    writeJson(file, json);

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
    File file = copyFixture("at_auth_4_0_0_rc2_typed_atsign_keys.atKeys", dir);
    byte[] before = Files.readAllBytes(file.toPath());

    IOException e = assertThrows(IOException.class, () -> KeysUtils.saveKeys(newKeys(), file));

    assertTrue(e.getMessage().contains("typed keys document"), e.getMessage());
    assertArrayEquals(before, Files.readAllBytes(file.toPath()));
  }

  @Test
  public void saveKeysOverwritesALegacyFile(@TempDir Path dir) throws Exception {
    File file = copyFixture("at_java_string_version.atKeys", dir);
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

  private static File copyFixture(String name, Path dir) throws Exception {
    Path target = dir.resolve(name);
    try (InputStream in = KeysUtilsTest.class.getResourceAsStream(name)) {
      assertNotNull(in, "missing fixture " + name);
      Files.copy(in, target);
    }
    return target.toFile();
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
