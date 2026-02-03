package org.atsign.client.api;

import org.atsign.client.util.EnrollmentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.atsign.client.util.EnrollmentId.createEnrollmentId;
import static org.junit.jupiter.api.Assertions.*;

class AtKeysTest {

    private AtKeys atKeys;
    private static final String TEST_ENROLLMENT_ID = "test-enrollment-123";
    private static final String TEST_SELF_ENCRYPT_KEY = "testSelfEncryptKey123";
    private static final String TEST_APKAM_SYMMETRIC_KEY = "testApkamSymmetricKey";

    @BeforeEach
    void setUp() {
        atKeys = new AtKeys();
    }

    // EnrollmentId Tests

    @Test
    void testHasEnrollmentIdReturnsFalseWhenNotSet() {
        assertFalse(atKeys.hasEnrollmentId());
    }

    @Test
    void testHasEnrollmentIdReturnsTrueWhenSet() {
        atKeys.setEnrollmentId(createEnrollmentId(TEST_ENROLLMENT_ID));
        assertTrue(atKeys.hasEnrollmentId());
    }

    @Test
    void testSetAndGetEnrollmentId() {
        EnrollmentId enrollmentId = createEnrollmentId(TEST_ENROLLMENT_ID);
        atKeys.setEnrollmentId(enrollmentId);
        assertEquals(enrollmentId, atKeys.getEnrollmentId());
    }

    @Test
    void testSetEnrollmentIdSupportsChaining() {
        EnrollmentId enrollmentId = createEnrollmentId(TEST_ENROLLMENT_ID);
        AtKeys result = atKeys.setEnrollmentId(enrollmentId);
        assertSame(atKeys, result);
    }

    @Test
    void testGetEnrollmentIdReturnsNullWhenNotSet() {
        assertNull(atKeys.getEnrollmentId());
    }

    // Self Encryption Key Tests

    @Test
    void testSetAndGetSelfEncryptionKey() {
        atKeys.setSelfEncryptKey(TEST_SELF_ENCRYPT_KEY);
        assertEquals(TEST_SELF_ENCRYPT_KEY, atKeys.getSelfEncryptKey());
    }

    @Test
    void testSetSelfEncryptKeySupportsChaining() {
        AtKeys result = atKeys.setSelfEncryptKey(TEST_SELF_ENCRYPT_KEY);
        assertSame(atKeys, result);
    }

    @Test
    void testGetSelfEncryptionKeyReturnsNullWhenNotSet() {
        assertNull(atKeys.getSelfEncryptKey());
    }

    // APKAM Public Key Tests

    @Test
    void testSetAndGetApkamPublicKeyAsString() {
        String testKey = "testPublicKey123";
        atKeys.setApkamPublicKey(testKey);
        assertEquals(testKey, atKeys.getApkamPublicKey());
    }

    @Test
    void testSetApkamPublicKeyFromPublicKeyObject() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        atKeys.setApkamPublicKey(keyPair.getPublic());

        assertNotNull(atKeys.getApkamPublicKey());
        String expected = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        assertEquals(expected, atKeys.getApkamPublicKey());
    }

    @Test
    void testSetApkamPublicKeyWithStringSupportsChaining() {
        AtKeys result = atKeys.setApkamPublicKey("testKey");
        assertSame(atKeys, result);
    }

    @Test
    void testSetApkamPublicKeyWithObjectSupportsChaining() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();
        AtKeys result = atKeys.setApkamPublicKey(keyPair.getPublic());
        assertSame(atKeys, result);
    }

    // APKAM Private Key Tests

    @Test
    void testSetAndGetApkamPrivateKeyAsString() {
        String testKey = "testPrivateKey123";
        atKeys.setApkamPrivateKey(testKey);
        assertEquals(testKey, atKeys.getApkamPrivateKey());
    }

    @Test
    void testSetApkamPrivateKeyFromPrivateKeyObject() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        atKeys.setApkamPrivateKey(keyPair.getPrivate());

        assertNotNull(atKeys.getApkamPrivateKey());
        String expected = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        assertEquals(expected, atKeys.getApkamPrivateKey());
    }

    @Test
    void testSetApkamPrivateKeyWithStringSupportsChaining() {
        AtKeys result = atKeys.setApkamPrivateKey("testKey");
        assertSame(atKeys, result);
    }

    @Test
    void testSetApkamPrivateKeyWithObjectSupportsChaining() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();
        AtKeys result = atKeys.setApkamPrivateKey(keyPair.getPrivate());
        assertSame(atKeys, result);
    }

    // APKAM Key Pair Tests

    @Test
    void testSetApkamKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        atKeys.setApkamKeyPair(keyPair);

        assertNotNull(atKeys.getApkamPublicKey());
        assertNotNull(atKeys.getApkamPrivateKey());

        String expectedPublic = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String expectedPrivate = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        assertEquals(expectedPublic, atKeys.getApkamPublicKey());
        assertEquals(expectedPrivate, atKeys.getApkamPrivateKey());
    }

    @Test
    void testSetApkamKeyPairSupportsChaining() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();
        AtKeys result = atKeys.setApkamKeyPair(keyPair);
        assertSame(atKeys, result);
    }

    // hasPkamKeys Tests

    @Test
    void testHasPkamKeysReturnsFalseWhenNotSet() {
        assertFalse(atKeys.hasPkamKeys());
    }

    @Test
    void testHasPkamKeysReturnsFalseWhenOnlyPublicKeySet() {
        atKeys.setApkamPublicKey("testPublicKey");
        assertFalse(atKeys.hasPkamKeys());
    }

    @Test
    void testHasPkamKeysReturnsFalseWhenOnlyPrivateKeySet() {
        atKeys.setApkamPrivateKey("testPrivateKey");
        assertFalse(atKeys.hasPkamKeys());
    }

    @Test
    void testHasPkamKeysReturnsTrueWhenBothSet() {
        atKeys.setApkamPublicKey("testPublicKey");
        atKeys.setApkamPrivateKey("testPrivateKey");
        assertTrue(atKeys.hasPkamKeys());
    }

    // Encrypt Public Key Tests

    @Test
    void testSetAndGetEncryptPublicKeyAsString() {
        String testKey = "testEncryptPublicKey";
        atKeys.setEncryptPublicKey(testKey);
        assertEquals(testKey, atKeys.getEncryptPublicKey());
    }

    @Test
    void testSetEncryptPublicKeyFromPublicKeyObject() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        atKeys.setEncryptPublicKey(keyPair.getPublic());

        assertNotNull(atKeys.getEncryptPublicKey());
        String expected = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        assertEquals(expected, atKeys.getEncryptPublicKey());
    }

    @Test
    void testSetEncryptPublicKeyWithStringSupportsChaining() {
        AtKeys result = atKeys.setEncryptPublicKey("testKey");
        assertSame(atKeys, result);
    }

    @Test
    void testSetEncryptPublicKeyWithObjectSupportsChaining() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();
        AtKeys result = atKeys.setEncryptPublicKey(keyPair.getPublic());
        assertSame(atKeys, result);
    }

    // Encrypt Private Key Tests

    @Test
    void testSetAndGetEncryptPrivateKeyAsString() {
        String testKey = "testEncryptPrivateKey";
        atKeys.setEncryptPrivateKey(testKey);
        assertEquals(testKey, atKeys.getEncryptPrivateKey());
    }

    @Test
    void testSetEncryptPrivateKeyFromPrivateKeyObject() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        atKeys.setEncryptPrivateKey(keyPair.getPrivate());

        assertNotNull(atKeys.getEncryptPrivateKey());
        String expected = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        assertEquals(expected, atKeys.getEncryptPrivateKey());
    }

    @Test
    void testSetEncryptPrivateKeyWithStringSupportsChaining() {
        AtKeys result = atKeys.setEncryptPrivateKey("testKey");
        assertSame(atKeys, result);
    }

    @Test
    void testSetEncryptPrivateKeyWithObjectSupportsChaining() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();
        AtKeys result = atKeys.setEncryptPrivateKey(keyPair.getPrivate());
        assertSame(atKeys, result);
    }

    // Encrypt Key Pair Tests

    @Test
    void testSetEncryptKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        atKeys.setEncryptKeyPair(keyPair);

        assertNotNull(atKeys.getEncryptPublicKey());
        assertNotNull(atKeys.getEncryptPrivateKey());

        String expectedPublic = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String expectedPrivate = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        assertEquals(expectedPublic, atKeys.getEncryptPublicKey());
        assertEquals(expectedPrivate, atKeys.getEncryptPrivateKey());
    }

    @Test
    void testSetEncryptKeyPairSupportsChaining() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyGen.generateKeyPair();
        AtKeys result = atKeys.setEncryptKeyPair(keyPair);
        assertSame(atKeys, result);
    }

    // APKAM Symmetric Key Tests

    @Test
    void testSetAndGetApkamSymmetricKey() {
        atKeys.setApkamSymmetricKey(TEST_APKAM_SYMMETRIC_KEY);
        assertEquals(TEST_APKAM_SYMMETRIC_KEY, atKeys.getApkamSymmetricKey());
    }

    @Test
    void testSetApkamSymmetricKeySupportsChaining() {
        AtKeys result = atKeys.setApkamSymmetricKey(TEST_APKAM_SYMMETRIC_KEY);
        assertSame(atKeys, result);
    }

    @Test
    void testGetApkamSymmetricKeyReturnsNullWhenNotSet() {
        assertNull(atKeys.getApkamSymmetricKey());
    }

    // Cache Tests

    @Test
    void testPutAndGetFromCache() {
        String key = "customKey";
        String value = "customValue";

        atKeys.put(key, value);
        assertEquals(value, atKeys.get(key));
    }

    @Test
    void testGetReturnsNullForNonExistentKey() {
        assertNull(atKeys.get("nonExistentKey"));
    }

    @Test
    void testPutOverwritesExistingKeyValue() {
        String key = "testKey";
        atKeys.put(key, "value1");
        atKeys.put(key, "value2");
        assertEquals("value2", atKeys.get(key));
    }

    @Test
    void testPutMultipleKeys() {
        atKeys.put("key1", "value1");
        atKeys.put("key2", "value2");
        atKeys.put("key3", "value3");

        assertEquals("value1", atKeys.get("key1"));
        assertEquals("value2", atKeys.get("key2"));
        assertEquals("value3", atKeys.get("key3"));
    }

    @Test
    void testGetCacheReturnsUnmodifiableMap() {
        atKeys.put("key1", "value1");
        atKeys.put("key2", "value2");

        Map<String, String> cache = atKeys.getCache();

        assertNotNull(cache);
        assertEquals(2, cache.size());
        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));

        assertThrows(UnsupportedOperationException.class, () -> {
            cache.put("key3", "value3");
        });
    }

    @Test
    void testGetCacheReturnsEmptyUnmodifiableMapWhenEmpty() {
        Map<String, String> cache = atKeys.getCache();

        assertNotNull(cache);
        assertTrue(cache.isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> {
            cache.put("key1", "value1");
        });
    }

    @Test
    void testGetCacheReflectsChanges() {
        atKeys.put("key1", "value1");
        Map<String, String> cache1 = atKeys.getCache();
        assertEquals(1, cache1.size());

        atKeys.put("key2", "value2");
        Map<String, String> cache2 = atKeys.getCache();
        assertEquals(2, cache2.size());
    }
}