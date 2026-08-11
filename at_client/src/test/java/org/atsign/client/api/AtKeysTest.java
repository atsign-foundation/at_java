package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AtKeysTest {

  @Test
  void testHasEnrollmentIdReturnsFalseWhenNotSet() {
    AtKeys keys = AtKeys.builder().build();
    assertFalse(keys.hasEnrollmentId());
  }

  @Test
  void testHasEnrollmentIdReturnsTrueWhenSet() {
    AtKeys keys = AtKeys.builder().enrollmentId(EnrollmentId.of("123")).build();
    assertTrue(keys.hasEnrollmentId());
  }

  @Test
  void testApkamKeyPairBuilderExtension() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair keyPair = keyGen.generateKeyPair();

    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(keyPair)
        .build();

    assertThat(keys.getApkamPublicKey(), equalTo(toBase64(keyPair.getPublic())));
    assertThat(keys.getApkamPrivateKey(), equalTo(toBase64(keyPair.getPrivate())));
  }

  @Test
  void testEncryptKeyPairBuilderExtension() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair keyPair = keyGen.generateKeyPair();

    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(keyPair)
        .build();

    assertThat(keys.getEncryptPublicKey(), equalTo(toBase64(keyPair.getPublic())));
    assertThat(keys.getEncryptPrivateKey(), equalTo(toBase64(keyPair.getPrivate())));
  }

  @Test
  void testHasPkamKeyReturnsFalseWhenNotSet() {
    AtKeys keys = AtKeys.builder().build();
    assertFalse(keys.hasPkamKey());
  }

  @Test
  void testHasPkamKeyReturnsFalseWhenOnlyPublicKeySet() {
    AtKeys keys = AtKeys.builder().apkamPublicKey("xxxx").build();
    assertFalse(keys.hasPkamKey());
  }

  @Test
  void testHasPkamKeyReturnsTrueWhenPrivateKeySet() {
    AtKeys keys = AtKeys.builder().apkamPrivateKey("xxxx").build();
    assertTrue(keys.hasPkamKey());
  }

  @Test
  void testGetReturnsNullForKeyBeforePut() {
    AtKeys keys = AtKeys.builder().build();
    assertThat(keys.get("key1"), nullValue());
  }

  @Test
  void testGetReturnsExpectedValueForKeyAfterPut() {
    AtKeys keys = AtKeys.builder().build();
    keys.put("key1", "value1");
    assertThat(keys.get("key1"), equalTo("value1"));
    assertThat(keys.get("key2"), nullValue());
  }

  @Test
  void testGetCacheReturnsUnmodifiableMap() {
    AtKeys keys = AtKeys.builder().build();
    keys.put("key1", "value1");
    keys.put("key2", "value2");

    Map<String, String> cache = keys.getCache();

    assertNotNull(cache);
    assertEquals(2, cache.size());
    assertEquals("value1", cache.get("key1"));
    assertEquals("value2", cache.get("key2"));

    assertThrows(UnsupportedOperationException.class, () -> {
      cache.put("key3", "value3");
    });
  }

  private static String toBase64(Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

}
