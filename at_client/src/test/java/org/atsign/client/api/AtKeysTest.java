package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import org.atsign.client.api.CryptographicMaterial.Algorithm;
import org.atsign.client.api.CryptographicMaterial.Role;
import org.atsign.client.api.CryptographicMaterial.Status;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class AtKeysTest {
  private static final KeyId TEST_KEY = KeyId.of("TEST_KEY");
  private static final KeyId TEST_KEY_2 = KeyId.of("TEST_KEY_2");

  @Test
  void testHasEnrollmentIdReturnsFalseWhenNotSet() {
    AtKeys keys = AtKeys.builder().build();
    assertFalse(keys.hasEnrollmentId());
  }

  @Test
  void testHasEnrollmentIdReturnsTrueWhenSet() {
    // The enrollment id is stamped onto the legacy material, so there has to be some.
    AtKeys keys = AtKeys.builder().apkamPublicKey("xxxx").enrollmentId("123").build();
    assertTrue(keys.hasEnrollmentId());
  }

  @Test
  void testEnrollmentIdRequiresLegacyMaterialToStampItOnto() {
    assertThrows(IllegalArgumentException.class, () -> AtKeys.builder().enrollmentId("123").build());
  }

  @Test
  void testApkamKeyPairBuilderExtension() throws Exception {
    KeyPair keyPair = generateKeyPair();

    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(keyPair)
        .build();

    assertThat(keys.getApkamPublicKey(), equalTo(toBase64(keyPair.getPublic())));
    assertThat(keys.getApkamPrivateKey(), equalTo(toBase64(keyPair.getPrivate())));
  }

  @Test
  void testEncryptKeyPairBuilderExtension() throws Exception {
    KeyPair keyPair = generateKeyPair();

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
  void testDeprecatedGettersReturnNullWhenTheMaterialIsAbsent() {
    AtKeys keys = AtKeys.builder().build();
    assertThat(keys.getSelfEncryptKey(), nullValue());
    assertThat(keys.getApkamSymmetricKey(), nullValue());
    assertThat(keys.getEncryptPublicKey(), nullValue());
    assertThat(keys.getAllCryptographicMaterial(), empty());
  }

  @Test
  void testDeprecatedSettersAndGettersAgreeThroughMaterials() {
    AtKeys keys = AtKeys.builder()
        .selfEncryptKey("c2VsZg==")
        .apkamSymmetricKey("c3ltbQ==")
        .build();

    assertThat(keys.getSelfEncryptKey(), equalTo("c2VsZg=="));
    assertThat(keys.getApkamSymmetricKey(), equalTo("c3ltbQ=="));
    assertThat(keys.getAllCryptographicMaterial(), is(empty()));
  }

  @Test
  void testDeprecatedSetterReplacesRatherThanAccumulates() {
    AtKeys keys = AtKeys.builder().selfEncryptKey("c2VsZg==").selfEncryptKey("b3RoZXI=").build();

    assertThat(keys.getSelfEncryptKey(), equalTo("b3RoZXI="));
  }

  @Test
  void testGetAllCryptographicMaterialByKeyIdAndEnrollmentId() {
    EnrollmentId enrollmentId = EnrollmentId.of("e1");
    CryptographicMaterial publicHalf = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .enrollmentId(enrollmentId)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();
    CryptographicMaterial privateHalf = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.privateDecryption)
        .enrollmentId(enrollmentId)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();
    CryptographicMaterial unenrolled = CryptographicMaterial.builder()
        .keyId(TEST_KEY_2)
        .role(Role.symmetricEncryption)
        .algorithm(Algorithm.aes256)
        .bytes("c2VjcmV0")
        .build();

    AtKeys keys = AtKeys.builder()
        .material(publicHalf)
        .material(privateHalf)
        .material(unenrolled)
        .build();

    assertThat(keys.getAllCryptographicMaterial(TEST_KEY), containsInAnyOrder(publicHalf, privateHalf));
    assertThat(keys.getAllCryptographicMaterial(TEST_KEY_2), contains(unenrolled));
    assertThat(keys.getAllCryptographicMaterial(enrollmentId), containsInAnyOrder(publicHalf, privateHalf));
    assertThat(keys.getAllCryptographicMaterial(KeyId.of("absent")), empty());
    assertThat(keys.getCryptographicMaterial(TEST_KEY, Role.publicEncryption), equalTo(publicHalf));
    assertThat(keys.getCryptographicMaterial(KeyId.of("absent"), Role.publicEncryption), nullValue());
    assertThat(keys.getCryptographicMaterial(TEST_KEY, Role.privateSigning), nullValue());
  }

  @Test
  void testWithKeyReturnsCopyAndLeavesOriginalUntouched() {
    CryptographicMaterial added = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();

    AtKeys original = AtKeys.builder().build();
    AtKeys updated = original.withKey(added);

    assertThat(updated.getAllCryptographicMaterial(), contains(added));
    assertThat(original.getAllCryptographicMaterial(), empty());
    assertThat(updated, not(sameInstance(original)));
  }

  @Test
  void testWithKeyRejectsDuplicateRoleForOneKeyId() {
    CryptographicMaterial held = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();
    CryptographicMaterial duplicate = held.toBuilder().bytes("b3RoZXI=").build();

    AtKeys keys = AtKeys.builder().material(held).build();

    assertThrows(IllegalArgumentException.class, () -> keys.withKey(duplicate));
  }

  @Test
  void testWithKeyRejectsDisagreeingEnrollmentIdOnOneKeyId() {
    CryptographicMaterial held = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .enrollmentId("e1")
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();
    CryptographicMaterial otherEnrollment = held.toBuilder()
        .role(Role.privateDecryption)
        .enrollmentId("e2")
        .build();

    AtKeys keys = AtKeys.builder().material(held).build();

    assertThrows(IllegalArgumentException.class, () -> keys.withKey(otherEnrollment));
  }

  @Test
  void testWithKeyRejectsASecondMaterialOfOneRoleForAnEnrollment() {
    CryptographicMaterial held = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .enrollmentId("e1")
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();
    CryptographicMaterial sameRoleOtherKeyId = held.toBuilder().keyId(TEST_KEY_2).build();

    AtKeys keys = AtKeys.builder().material(held).build();

    assertThrows(IllegalArgumentException.class, () -> keys.withKey(sameRoleOtherKeyId));
  }

  @Test
  void testWithRetiredKeyMarksEveryMaterialOfTheKeyId() {
    CryptographicMaterial publicHalf = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();
    CryptographicMaterial privateHalf = publicHalf.toBuilder().role(Role.privateDecryption).build();
    CryptographicMaterial untouched = publicHalf.toBuilder()
        .keyId(TEST_KEY_2)
        .role(Role.symmetricEncryption)
        .algorithm(Algorithm.aes256)
        .build();

    AtKeys keys = AtKeys.builder()
        .material(publicHalf)
        .material(privateHalf)
        .material(untouched)
        .build();

    AtKeys retired = keys.withRetiredKey(TEST_KEY);

    assertThat(retired.getCryptographicMaterial(TEST_KEY, Role.publicEncryption).getStatus(), equalTo(Status.retired));
    assertThat(retired.getCryptographicMaterial(TEST_KEY, Role.privateDecryption).getStatus(), equalTo(Status.retired));
    assertThat(retired.getCryptographicMaterial(TEST_KEY_2, Role.symmetricEncryption).getStatus(),
               equalTo(Status.active));
    assertThat(keys.getCryptographicMaterial(TEST_KEY, Role.publicEncryption).getStatus(), equalTo(Status.active));
  }

  @Test
  void testWithRetiredKeyMovesForwardAndIsIdempotent() {
    CryptographicMaterial material = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();

    AtKeys keys = AtKeys.builder().material(material).build();

    AtKeys dead = keys.withRetiredKey(TEST_KEY).withRetiredKey(TEST_KEY, Status.dead);
    assertThat(dead.getCryptographicMaterial(TEST_KEY, Role.publicEncryption).getStatus(), equalTo(Status.dead));

    AtKeys again = dead.withRetiredKey(TEST_KEY, Status.dead);
    assertThat(again.getCryptographicMaterial(TEST_KEY, Role.publicEncryption).getStatus(), equalTo(Status.dead));
  }

  @Test
  void testWithRetiredKeyRejectsBackwardUnknownAndReactivation() {
    CryptographicMaterial material = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();

    AtKeys keys = AtKeys.builder().material(material).build();
    AtKeys dead = keys.withRetiredKey(TEST_KEY, Status.dead);

    assertThrows(IllegalArgumentException.class, () -> dead.withRetiredKey(TEST_KEY));
    assertThrows(IllegalArgumentException.class, () -> keys.withRetiredKey(KeyId.of("absent")));
    assertThrows(IllegalArgumentException.class, () -> keys.withRetiredKey(TEST_KEY, Status.active));
  }

  @Test
  void testAtSignIsCarriedAndMayBeAbsent() {
    assertThat(AtKeys.builder().build().getAtSign(), nullValue());
    assertThat(AtKeys.builder().atSign("@alice").build().getAtSign(), equalTo(AtSign.of("@alice")));
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

  /**
   * toBuilder() is hand-written, so a field added to AtKeys and not to it would be silently dropped
   * by every copy. Equality covers everything except the cache, which the next test pins.
   */
  @Test
  void testToBuilderCarriesEveryField() {
    CryptographicMaterial material = CryptographicMaterial.builder()
        .keyId(TEST_KEY)
        .role(Role.publicEncryption)
        .enrollmentId("e1")
        .algorithm(Algorithm.rsa2048)
        .bytes("c2VjcmV0")
        .build();

    AtKeys keys = AtKeys.builder()
        .atSign("@alice")
        .material(material)
        .selfEncryptKey("c2VsZg==")
        .build();

    assertThat(keys.toBuilder().build(), equalTo(keys));
  }

  @Test
  void testToBuilderCopiesTheCacheRatherThanSharingIt() {
    AtKeys keys = AtKeys.builder().apkamPublicKey("xxxx").build();
    keys.put("key1", "value1");

    AtKeys copy = keys.toBuilder().build();
    copy.put("key2", "value2");

    assertThat(copy.get("key1"), equalTo("value1"));
    assertThat(keys.get("key2"), nullValue());
  }

  @Test
  void testToBuilderCarriesTheCache() {
    AtKeys keys = AtKeys.builder().apkamPublicKey("xxxx").build();
    keys.put("key1", "value1");

    AtKeys copy = keys.toBuilder().enrollmentId("123").build();

    assertThat(copy.get("key1"), equalTo("value1"));
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

  private static KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    return keyGen.generateKeyPair();
  }

  private static String toBase64(Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

}
