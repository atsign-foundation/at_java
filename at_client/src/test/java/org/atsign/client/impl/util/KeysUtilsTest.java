package org.atsign.client.impl.util;

import static org.atsign.client.impl.util.EncryptionUtils.generateAESKeyBase64;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class KeysUtilsTest {

  AtSign testAtSign = new AtSign("@testSaveKeysFile");

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
