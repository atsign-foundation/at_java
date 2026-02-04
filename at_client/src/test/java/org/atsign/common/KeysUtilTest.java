package org.atsign.common;

import static org.atsign.client.util.EncryptionUtil.generateAESKeyBase64;
import static org.atsign.client.util.EncryptionUtil.generateRSAKeyPair;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.atsign.client.api.AtKeys;
import org.atsign.client.util.KeysUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class KeysUtilTest {

  AtSign testAtSign = new AtSign("@testSaveKeysFile");

  @AfterEach
  public void tearDown() throws IOException {
    Files.deleteIfExists(KeysUtil.getKeysFile(testAtSign, KeysUtil.expectedKeysFilesLocation).toPath());
    Files.deleteIfExists(KeysUtil.getKeysFile(testAtSign, KeysUtil.legacyKeysFilesLocation).toPath());
  }

  @Test
  public void testSaveKeysFile() throws Exception {
    File expected = KeysUtil.getKeysFile(testAtSign, KeysUtil.expectedKeysFilesLocation);
    assertFalse(expected.exists());

    // Given a Map of keys (like Onboard creates)
    AtKeys keys = new AtKeys()
        .setEncryptKeyPair(generateRSAKeyPair())
        .setApkamKeyPair(generateRSAKeyPair())
        .setSelfEncryptKey(generateAESKeyBase64());

    // When we call KeysUtil.saveKeys
    KeysUtil.saveKeys(testAtSign, keys);

    // Then we end up with a file with the expected name in the expected (canonical) location
    assertTrue(expected.exists());
  }

  @Test
  public void testLoadKeysFile() throws Exception {
    // Given a correctly formatted keys file in the canonical location
    AtKeys keys = new AtKeys()
        .setEncryptKeyPair(generateRSAKeyPair())
        .setApkamKeyPair(generateRSAKeyPair())
        .setSelfEncryptKey(generateAESKeyBase64());
    KeysUtil.saveKeys(testAtSign, keys);

    // When we call KeysUtil.loadKeys
    AtKeys loadedKeys = KeysUtil.loadKeys(testAtSign);

    // Then the keys are loaded successfully
    assertContentsMatch(keys, loadedKeys);
  }

  @Test
  public void testLoadKeysFileLegacy() throws Exception {
    AtKeys keys = new AtKeys()
        .setEncryptKeyPair(generateRSAKeyPair())
        .setApkamKeyPair(generateRSAKeyPair())
        .setSelfEncryptKey(generateAESKeyBase64());

    KeysUtil.saveKeys(testAtSign, keys);
    File expected = KeysUtil.getKeysFile(testAtSign, KeysUtil.expectedKeysFilesLocation);
    assertTrue(expected.exists());

    // Given a correctly formatted keys file in the legacy location
    // And there is NOT a keys file in the canonical location

    // So, in order to set up the "Given" pre-conditions above, we'll need to
    // 1) move the generated file to the legacy location
    File file = new File(KeysUtil.legacyKeysFilesLocation);
    file.deleteOnExit();
    Files.createDirectories(file.toPath());
    Files.move(expected.toPath(), KeysUtil.getKeysFile(testAtSign, KeysUtil.legacyKeysFilesLocation).toPath());

    // 2) delete the file we just generated in the expected location
    Path expectedCanonicalFilePath = KeysUtil.getKeysFile(testAtSign, KeysUtil.expectedKeysFilesLocation).toPath();
    Files.deleteIfExists(expectedCanonicalFilePath);

    // Ensure the file does not exist in the expected place
    assertFalse(expected.exists());

    // When we call KeysUtil.loadKeys
    // Then the keys are loaded successfully from the legacy location
    AtKeys loadedKeys = KeysUtil.loadKeys(testAtSign);
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
