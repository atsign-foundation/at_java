package org.atsign.common;

import org.atsign.client.util.KeysUtil;
import org.atsign.client.util.OnboardingUtil;
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class KeysUtilTest {
    AtSign testAtSign = new AtSign("@testSaveKeysFile");

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(KeysUtil.getKeysFile(testAtSign, KeysUtil.expectedKeysFilesLocation).toPath());
        Files.deleteIfExists(KeysUtil.getKeysFile(testAtSign, KeysUtil.legacyKeysFilesLocation).toPath());
    }

    @Test
    public void testSaveKeysFile() throws Exception {
        File expected = KeysUtil.getKeysFile(testAtSign, KeysUtil.expectedKeysFilesLocation);
        assertFalse(expected.exists());

        // Given a Map of keys (like Onboard creates)
        Map<String, String> keys = new HashMap<>();
        OnboardingUtil onboardingUtil = new OnboardingUtil();
        onboardingUtil.generateEncryptionKeypair(keys);
        onboardingUtil.generatePkamKeypair(keys);
        onboardingUtil.generateSelfEncryptionKey(keys);

        // When we call KeysUtil.saveKeys
        KeysUtil.saveKeys(testAtSign, keys);

        // Then we end up with a file with the expected name in the expected (canonical) location
        assertTrue(expected.exists());
    }

    @Test
    public void testLoadKeysFile() throws Exception {
        // Given a correctly formatted keys file in the canonical location
        Map<String, String> keys = new HashMap<>();
        OnboardingUtil onboardingUtil = new OnboardingUtil();
        onboardingUtil.generateEncryptionKeypair(keys);
        onboardingUtil.generatePkamKeypair(keys);
        onboardingUtil.generateSelfEncryptionKey(keys);
        KeysUtil.saveKeys(testAtSign, keys);

        // When we call KeysUtil.loadKeys
        Map<String, String> loadedKeys = KeysUtil.loadKeys(testAtSign);

        // Then the keys are loaded successfully
        assertEquals(keys, loadedKeys);
    }

    @Test
    public void testLoadKeysFileLegacy() throws Exception {
        Map<String, String> keys = new HashMap<>();
        OnboardingUtil onboardingUtil = new OnboardingUtil();
        onboardingUtil.generateEncryptionKeypair(keys);
        onboardingUtil.generatePkamKeypair(keys);
        onboardingUtil.generateSelfEncryptionKey(keys);

        KeysUtil.saveKeys(testAtSign, keys);

        // Given a correctly formatted keys file in the legacy location
        //   And there is NOT a keys file in the canonical location
        // So = we'll need to copy the generated file to the legacy location
        // And delete the file we just generated

        // When we call KeysUtil.loadKeys
        // Then the keys are loaded successfully from the legacy location
    }
}
