package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KeysTest {

  @Test
  void testPublicKeyBuilderThrowsExpectedExceptionWhenSharedByIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.publicKeyBuilder().build());
    assertThat(ex.getMessage(), containsString("sharedBy is not set"));
  }

  @Test
  void testPublicKeyBuilderThrowsExpectedExceptionWhenKeyNameIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.publicKeyBuilder().sharedBy(AtSign.of("fred")).build());
    assertThat(ex.getMessage(), containsString("name is not set"));
  }

  @Test
  void testPublicKeyBuilderWithNoNamespaceCreatesExpectedKey() {
    Keys.PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("public:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.FALSE)
        .build()));
  }

  @Test
  void testPublicKeyBuilderWithNamespaceCreatesExpectedKey() {
    Keys.PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .namespace("ns")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
    assertThat(key.toString(), equalTo("public:key1.ns@fred"));
  }

  @Test
  void testPublicKeyBuilderWithAdditionalMetadataFieldsCreatesExpectedKey() {
    Keys.PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .ttl(1000L)
        .ttb(2000L)
        .ttr(3000L)
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.FALSE)
        .ttl(1000L)
        .ttb(2000L)
        .ttr(3000L)
        .build()));
  }

  @Test
  void testPublicKeyBuilderWithMetadataFieldsOverridesCreatesExpectedKey() {
    Keys.PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .isCached(true)
        .isBinary(true)
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.TRUE)
        .isEncrypted(Boolean.FALSE)
        .isBinary(Boolean.TRUE)
        .build()));
    assertThat(key.toString(), equalTo("cached:public:key1@fred"));
  }

  @Test
  void testKeyBuilderCreatesExpectedKeyForPublicKeys() {
    Keys.AtKey key = Keys.keyBuilder().rawKey("public:key1@fred").build();

    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("public:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.FALSE)
        .build()));

    key = Keys.keyBuilder().rawKey("cached:public:key1@fred").build();

    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("cached:public:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.TRUE)
        .isEncrypted(Boolean.FALSE)
        .build()));

    key = Keys.keyBuilder().rawKey("cached:public:_key1@fred").build();

    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("_key1"));
    assertThat(key.toString(), equalTo("cached:public:_key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.TRUE)
        .isCached(Boolean.TRUE)
        .isEncrypted(Boolean.FALSE)
        .build()));

    key = Keys.keyBuilder().rawKey("public:key1@fred").metadata(Metadata.builder().isEncrypted(true).build()).build();

    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("public:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.TRUE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));
  }

  @Test
  void testSelfKeyBuilderThrowsExpectedExceptionWhenSharedByIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.selfKeyBuilder().build());
    assertThat(ex.getMessage(), containsString("sharedBy is not set"));
  }

  @Test
  void testSelfKeyBuilderThrowsExpectedExceptionWhenKeyNameIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.selfKeyBuilder().sharedBy(AtSign.of("fred")).build());
    assertThat(ex.getMessage(), containsString("name is not set"));
  }

  @Test
  void testSelfKeyBuilderWithNoNamespaceCreatesExpectedKey() {
    Keys.SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));
  }

  @Test
  void testSelfKeyBuilderWithNamespaceCreatesExpectedKey() {
    Keys.SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .namespace("ns")
        .build();

    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.toString(), equalTo("key1.ns@fred"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
  }

  @Test
  void testSelfKeyBuilderWithSharedWithCreatesExpectedKey() {
    Keys.SelfKey key = Keys.selfKeyBuilder()
        .sharedWith(AtSign.of("fred"))
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .namespace("ns")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("fred")));
    assertThat(key.toString(), equalTo("@fred:key1.ns@fred"));
    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
  }

  @Test
  void testSelfKeyBuilderWithAdditionalMetadataFieldsCreatesExpectedKey() {
    Keys.SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .ttl(1000L)
        .ttb(2000L)
        .ttr(3000L)
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .ttl(1000L)
        .ttb(2000L)
        .ttr(3000L)
        .build()));
  }

  @Test
  void testSelfKeyBuilderWithMetadataFieldsOverridesCreatesExpectedKey() {
    Keys.SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .isBinary(true)
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .isBinary(Boolean.TRUE)
        .build()));
  }

  @Test
  void testKeyBuilderCreatesExpectedSelfKey() {
    Keys.AtKey key = Keys.keyBuilder()
        .rawKey("key1@fred")
        .build();

    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.toString(), equalTo("key1@fred"));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.namespace(), nullValue());
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));

    key = Keys.keyBuilder()
        .rawKey("key1.ns@fred")
        .build();

    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.toString(), equalTo("key1.ns@fred"));
    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));

    key = Keys.keyBuilder()
        .rawKey("@fred:key1.ns@fred")
        .build();

    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("fred")));
    assertThat(key.toString(), equalTo("@fred:key1.ns@fred"));
    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));

    key = Keys.keyBuilder()
        .rawKey("@fred:key1.ns@fred")
        .metadata(Metadata.builder().isBinary(true).build())
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .isBinary(Boolean.TRUE)
        .build()));

  }

  @Test
  void testSharedKeyBuilderThrowsExpectedExceptionWhenSharedByIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.sharedKeyBuilder().build());
    assertThat(ex.getMessage(), containsString("sharedBy is not set"));
  }

  @Test
  void testSharedKeyBuilderThrowsExpectedExceptionWhenKeyNameIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.sharedKeyBuilder().sharedBy(AtSign.of("fred"))
                                                   .sharedWith(AtSign.of("colin")).build());
    assertThat(ex.getMessage(), containsString("name is not set"));
  }

  @Test
  void testSharedKeyBuilderThrowsExpectedExceptionWhenSharedKeyWithIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.sharedKeyBuilder().sharedBy(AtSign.of("fred")).name("key1")
                                                   .build());
    assertThat(ex.getMessage(), containsString("sharedWith is not set"));
  }

  @Test
  void testSharedKeyBuilderThrowsExpectedExceptionWhenRawKeyAndOtherFieldsSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.sharedKeyBuilder().name("key1").rawKey("colin@:key1@fred")
                                                   .build());
    assertThat(ex.getMessage(), containsString("both rawKey and other fields set"));
  }

  @Test
  void testSharedKeyBuilderWithNoNamespaceCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .sharedWith(AtSign.of("colin"))
        .name("key1")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("@colin:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));
  }

  @Test
  void testSharedKeyBuilderWithRawKeyNoNamespaceCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .rawKey("@colin:key1@fred")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("@colin:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));
  }

  @Test
  void testSharedKeyBuilderWithCachedRawKeyCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .rawKey("cached:@colin:key1@fred")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("cached:@colin:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.TRUE)
        .isEncrypted(Boolean.TRUE)
        .build()));
  }

  @Test
  void testSharedKeyBuilderWithNamespaceCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .sharedWith(AtSign.of("colin"))
        .name("key1")
        .namespace("ns")
        .build();

    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.toString(), equalTo("@colin:key1.ns@fred"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
  }

  @Test
  void testSharedKeyBuilderWithRawKeyWithNamespaceCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .rawKey("@colin:key1.ns@fred")
        .build();

    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.toString(), equalTo("@colin:key1.ns@fred"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
  }

  @Test
  void testSharedKeyBuilderWithAdditionalMetadataFieldsCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .sharedWith(AtSign.of("colin"))
        .name("key1")
        .ttl(1000L)
        .ttb(2000L)
        .ttr(3000L)
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .ttl(1000L)
        .ttb(2000L)
        .ttr(3000L)
        .build()));
  }

  @Test
  void testSharedKeyBuilderWithMetadataFieldsOverridesCreatesExpectedKey() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .sharedWith(AtSign.of("colin"))
        .name("key1")
        .isBinary(true)
        .build();

    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .isBinary(Boolean.TRUE)
        .build()));
  }

  @Test
  void testKeyBuilderCreatesExpectedSharedKey() {
    Keys.AtKey key = Keys.keyBuilder()
        .rawKey("@colin:key1@fred")
        .build();

    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("@colin:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));

    key = Keys.keyBuilder()
        .rawKey("cached:@colin:key1@fred")
        .build();

    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("cached:@colin:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.TRUE)
        .isEncrypted(Boolean.TRUE)
        .build()));

    key = Keys.keyBuilder()
        .rawKey("@colin:_key1@fred")
        .build();

    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("_key1"));
    assertThat(key.toString(), equalTo("@colin:_key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.TRUE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .build()));

    key = Keys.keyBuilder()
        .rawKey("@colin:key1@fred")
        .metadata(Metadata.builder().isBinary(true).build())
        .build();

    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.sharedWith(), equalTo(AtSign.of("colin")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("@colin:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.FALSE)
        .isCached(Boolean.FALSE)
        .isEncrypted(Boolean.TRUE)
        .isBinary(Boolean.TRUE)
        .build()));

  }

  @Test
  void testUpdateMissingMetadataWorksAsExpected() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .sharedWith(AtSign.of("colin"))
        .name("key1")
        .isBinary(true)
        .ttl(1000L)
        .build();

    Metadata metadata = Metadata.builder()
        .ttl(2000L)
        .ttb(3000L)
        .isBinary(false)
        .build();

    key.updateMissingMetadata(metadata);

    assertThat(key.metadata().ttl(), equalTo(1000L));
    assertThat(key.metadata().ttb(), equalTo(3000L));
    assertThat(key.metadata().isBinary(), equalTo(true));
  }

  @Test
  void testOverwriteMetadataWorksAsExpected() {
    Keys.SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .sharedWith(AtSign.of("colin"))
        .name("key1")
        .isBinary(true)
        .ttl(1000L)
        .build();

    Metadata metadata = Metadata.builder()
        .ttl(2000L)
        .ttb(3000L)
        .isBinary(false)
        .build();

    key.overwriteMetadata(metadata);

    assertThat(key.metadata().ttl(), equalTo(2000L));
    assertThat(key.metadata().ttb(), equalTo(3000L));
    assertThat(key.metadata().isBinary(), equalTo(false));
  }

  @Test
  void testPrivateHiddenKeyBuilderThrowsExpectedExceptionWhenSharedByIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.privateHiddenKeyBuilder().build());
    assertThat(ex.getMessage(), containsString("sharedBy is not set"));
  }

  @Test
  void testPrivateHiddenKeyBuilderThrowsExpectedExceptionWhenKeyNameIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> Keys.privateHiddenKeyBuilder().sharedBy(AtSign.of("fred"))
                                                   .build());
    assertThat(ex.getMessage(), containsString("name is not set"));
  }

  @Test
  void testPrivateHiddenKeyBuilderWithNoNamespaceCreatesExpectedKey() {
    Keys.PrivateHiddenKey key = Keys.privateHiddenKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .rawKey("private:key1@fred")
        .build();

    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("private:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.TRUE)
        .isCached(Boolean.FALSE)
        .build()));
  }

  @Test
  void testPrivateHiddenKeyBuilderWithNamespaceCreatesExpectedKey() {
    Keys.PrivateHiddenKey key = Keys.privateHiddenKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key1")
        .namespace("ns")
        .rawKey("private:key1.ns@fred")
        .build();

    assertThat(key.name(), equalTo("key1.ns"));
    assertThat(key.toString(), equalTo("private:key1.ns@fred"));
    assertThat(key.namespace(), equalTo("ns"));
    assertThat(key.nameWithoutNamespace(), equalTo("key1"));
  }

  @Test
  void testKeyBuilderCreatesExpectedPrivateHiddenKey() {
    Keys.AtKey key = Keys.keyBuilder()
        .rawKey("private:key1@fred")
        .build();

    assertThat(key, instanceOf(Keys.PrivateHiddenKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("private:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.TRUE)
        .isCached(Boolean.FALSE)
        .build()));

    key = Keys.keyBuilder()
        .rawKey("private:key1@fred")
        .metadata(Metadata.builder().isBinary(true).build())
        .build();

    assertThat(key, instanceOf(Keys.PrivateHiddenKey.class));
    assertThat(key.sharedBy(), equalTo(AtSign.of("fred")));
    assertThat(key.name(), equalTo("key1"));
    assertThat(key.toString(), equalTo("private:key1@fred"));
    assertThat(key.metadata(), equalTo(Metadata.builder()
        .isPublic(Boolean.FALSE)
        .isHidden(Boolean.TRUE)
        .isCached(Boolean.FALSE)
        .isBinary(Boolean.TRUE)
        .build()));
  }

  //
  // Migrated KeyStringUtilTest.java
  //

  @Test
  public void testKeyBuilderWithPublicKeyRawKey() throws Exception {
    Keys.AtKey key = Keys.keyBuilder().rawKey("public:phone@bob").build();

    assertThat(key.toString(), equalTo("public:phone@bob"));

    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  // Row 5 Public Hidden key
  @Test
  public void publicKey2() throws Exception {
    String KEY_NAME = "public:_phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.toString(), equalTo("public:_phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("_phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
  }

  // Row 6 Public Hidden key
  @Test
  public void publicKey3() throws Exception {
    String KEY_NAME = "public:__phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.toString(), equalTo("public:__phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("__phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
  }

  // Row 7A Public key and SharedWith populated
  public void publicKey4A() throws Exception {
    String KEY_NAME = "public:@bob:phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("public:@bob:phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith().toString(), equalTo("@bob"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  // Row 7B Public key and SharedWith populated
  // TODO: query this scenario
  @Test
  public void publicKey4B() throws Exception {
    String KEY_NAME = "public:@alice:phone@bob";
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Keys.keyBuilder().rawKey(KEY_NAME).build());
    assertThat(ex.getMessage(), containsString("does NOT match any raw key parser"));
  }

  // Row 8 self key (sharedWith not populated)
  @Test
  public void selfKey1() throws Exception {
    String KEY_NAME = "phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  // Row 9 Self key (sharedWith populated)
  @Test
  public void selfKey2() throws Exception {
    String KEY_NAME = "@bob:phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@bob:phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith().toString(), equalTo("@bob"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  // Row 10 Self hidden key (sharedWith populated)
  @Test
  public void selfKey3() throws Exception {
    String KEY_NAME = "@bob:_phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@bob:_phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("_phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith().toString(), equalTo("@bob"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
  }

  // row 11 Self Hidden Key without sharedWith
  @Test
  public void selfKey4() throws Exception {
    String KEY_NAME = "_phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("_phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("_phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
  }

  // Row 12 SharedKey
  @Test
  public void sharedKey1() throws Exception {
    String KEY_NAME = "@bob:phone@alice";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@bob:phone@alice"));
    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedWith().toString(), equalTo("@bob"));
    assertThat(key.sharedBy().toString(), equalTo("@alice"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  // Row 13 Shared and hidden
  @Test
  public void sharedKey2() throws Exception {
    String KEY_NAME = "@alice:_phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@alice:_phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("_phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith().toString(), equalTo("@alice"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
  }

  // Row 14A Private keys
  // TODO: query this scenario
  @Test
  public void privateKey1() throws Exception {
    String KEY_NAME = "private:phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("private:phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PrivateHiddenKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
    assertThat(key.metadata().isPublic(), is(false));
  }

  // Row 14B Private keys
  // TODO: query this scenario
  @Test
  public void privateKey2() throws Exception {
    String KEY_NAME = "privatekey:phone@bob";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("privatekey:phone@bob"));
    assertThat(key.nameWithoutNamespace(), equalTo("phone"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PrivateHiddenKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@bob"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(true));
    assertThat(key.metadata().isPublic(), is(false));
  }

  /**
   * 0: @farinataanxious:lemon@sportsunconscious
   * 1: @farinataanxious:shared_key@sportsunconscious
   * 2: @farinataanxious:test@sportsunconscious
   * 3: @sportsunconscious:shared_key@sportsunconscious
   * 4: @sportsunconscious:signing_privatekey@sportsunconscious
   * 5: public:publickey@farinataanxious
   * 6: public:publickey@sportsunconscious
   * 7: public:signing_publickey@sportsunconscious
   * 8: shared_key.farinataanxious@sportsunconscious
   * 9: shared_key.sportsunconscious@sportsunconscious
   */
  @Test
  public void suKey1() throws Exception {
    String KEY_NAME = "@farinataanxious:lemon@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@farinataanxious:lemon@sportsunconscious"));
    assertThat(key.nameWithoutNamespace(), equalTo("lemon"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith().toString(), equalTo("@farinataanxious"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey2() throws Exception {
    String KEY_NAME = "@farinataanxious:shared_key@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@farinataanxious:shared_key@sportsunconscious"));
    assertThat(key.namespace(), nullValue());
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key"));
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith().toString(), equalTo("@farinataanxious"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey3() throws Exception {
    String KEY_NAME = "@farinataanxious:test@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@farinataanxious:test@sportsunconscious"));
    assertThat(key.nameWithoutNamespace(), equalTo("test"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith().toString(), equalTo("@farinataanxious"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey4() throws Exception {
    String KEY_NAME = "@sportsunconscious:shared_key@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@sportsunconscious:shared_key@sportsunconscious"));
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith().toString(), equalTo("@sportsunconscious"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey5() throws Exception {
    String KEY_NAME = "@sportsunconscious:signing_privatekey@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@sportsunconscious:signing_privatekey@sportsunconscious"));
    assertThat(key.nameWithoutNamespace(), equalTo("signing_privatekey"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith().toString(), equalTo("@sportsunconscious"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey6() throws Exception {
    String KEY_NAME = "public:publickey@farinataanxious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("public:publickey@farinataanxious"));
    assertThat(key.nameWithoutNamespace(), equalTo("publickey"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@farinataanxious"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey7() throws Exception {
    String KEY_NAME = "public:publickey@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("public:publickey@sportsunconscious"));
    assertThat(key.nameWithoutNamespace(), equalTo("publickey"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.PublicKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey8() throws Exception {
    String KEY_NAME = "shared_key.farinataanxious@sportsunconscious";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("shared_key.farinataanxious@sportsunconscious"));
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key.farinataanxious"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@sportsunconscious"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void suKey9() throws Exception {
    String KEY_NAME =
        "atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague@smoothalligator";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertEquals("atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague@smoothalligator",
                 key.toString());
    assertThat(key.nameWithoutNamespace(),
               equalTo("atconnections.hacktheleague.smoothalligator.at_contact.mospherepro"));
    assertThat(key.namespace(), equalTo("hacktheleague"));

  }

  /**
   * smoothalligator
   * 0: @abbcservicesinc:shared_key@smoothalligator
   * 1: @denise:shared_key@smoothalligator
   * 2: @er_nobile_14:shared_key@smoothalligator
   * 3: @fascinatingsnow:shared_key@smoothalligator
   * 4: @hacktheleague:shared_key@smoothalligator
   * 5: @smoothalligator:signing_privatekey@smoothalligator
   * 6: @wildgreen:shared_key@smoothalligator
   * 7:
   * atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc@smoothalligator
   * 8: atconnections.denise.smoothalligator.at_contact.mospherepro.denise@smoothalligator
   * 9:
   * atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague@smoothalligator
   * 10: atconnections.wildgreen.smoothalligator.at_contact.mospherepro.wildgreen@smoothalligator
   * 11: @smoothalligator:shared_key@abbcservicesinc
   * 12: @smoothalligator:shared_key@denise
   * 13: @smoothalligator:shared_key@fascinatingsnow
   * 14: @smoothalligator:shared_key@wildgreen
   * 15: public:firstname.wavi.wavi@abbcservicesinc
   * 16: public:firstname.wavi.wavi@wildgreen
   * 17: public:image.wavi.wavi@abbcservicesinc
   * 18: public:image.wavi.wavi@denise
   * 19: public:image.wavi.wavi@wildgreen
   * 20: public:lastname.wavi.wavi@abbcservicesinc
   * 21: public:lastname.wavi.wavi@wildgreen
   * 22: public:publickey@abbcservicesinc
   * 23: public:publickey@denise
   * 24: public:publickey@er_nobile_14
   * 25: public:publickey@fascinatingsnow
   * 26: public:publickey@hacktheleague
   * 27: public:publickey@wildgreen
   * 28: public:email.wavi.wavi@smoothalligator
   * 29: public:field_order_of_self.wavi.wavi@smoothalligator
   * 30: public:firstname.wavi.wavi@smoothalligator
   * 31: public:following_by_self.at_follows.wavi.at_follows@smoothalligator
   * 32: public:lastname.wavi.wavi@smoothalligator
   * 33: public:privateaccount.wavi.wavi@smoothalligator
   * 34: public:publickey@smoothalligator
   * 35: public:signing_publickey@smoothalligator
   * 36: public:theme_color.wavi.wavi@smoothalligator
   * 37: publickey.fascinatingsnow.fascinatingsnow@smoothalligator
   * 38: senthistory_v2.mospherepro.mospherepro@smoothalligator
   * 39: shared_key.abbcservicesinc@smoothalligator
   * 40: shared_key.denise@smoothalligator
   * 41: shared_key.er_nobile_14@smoothalligator
   * 42: shared_key.fascinatingsnow@smoothalligator
   * 43: shared_key.hacktheleague@smoothalligator
   * 44: shared_key.wildgreen@smoothalligator
   */

  @Test
  public void saTest1() throws Exception {
    String KEY_NAME = "@abbcservicesinc:shared_key@smoothalligator";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@abbcservicesinc:shared_key@smoothalligator"));
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key"));
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@smoothalligator"));
    assertThat(key.sharedWith().toString(), equalTo("@abbcservicesinc"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void saTest2() throws Exception {
    String KEY_NAME = "@denise:shared_key@smoothalligator";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@denise:shared_key@smoothalligator"));
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key"));
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@smoothalligator"));
    assertThat(key.sharedWith().toString(), equalTo("@denise"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void saTest3() throws Exception {
    String KEY_NAME = "@er_nobile_14:shared_key@smoothalligator";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@er_nobile_14:shared_key@smoothalligator"));
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@smoothalligator"));
    assertThat(key.sharedWith().toString(), equalTo("@er_nobile_14"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
    assertThat(key.namespace(), nullValue());
  }

  @Test
  public void saTest4() throws Exception {
    String KEY_NAME = "@fascinatingsnow:shared_key@smoothalligator";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertThat(key.toString(), equalTo("@fascinatingsnow:shared_key@smoothalligator"));
    assertThat(key.nameWithoutNamespace(), equalTo("shared_key"));
    assertThat(key.namespace(), nullValue());
    assertThat(key, instanceOf(Keys.SharedKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@smoothalligator"));
    assertThat(key.sharedWith().toString(), equalTo("@fascinatingsnow"));
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void saTest7() throws Exception {
    String KEY_NAME =
        "atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc@smoothalligator";
    Keys.AtKey key = Keys.keyBuilder().rawKey(KEY_NAME).build();
    assertEquals("atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc@smoothalligator",
                 key.toString());
    assertThat(key.nameWithoutNamespace(),
               equalTo("atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro"));
    assertThat(key.namespace(), equalTo("abbcservicesinc"));
    assertThat(key, instanceOf(Keys.SelfKey.class));
    assertThat(key.sharedBy().toString(), equalTo("@smoothalligator"));
    assertThat(key.sharedWith(), nullValue());
    assertThat(key.metadata().isCached(), is(false));
    assertThat(key.metadata().isHidden(), is(false));
  }

  @Test
  public void testGetNamespaceReturnsNullForKeyStringsWithNoNamespace() throws Exception {
    assertNamespace("public:location@alice", null);
    assertNamespace("selfkey1@alice", null);
    assertNamespace("@bob:phone@alice", null);

    assertNamespace("public:_hiddenlocation@alice", null);
    assertNamespace("_hiddenselfkey1@alice", null);
    assertNamespace("@bob:__hiddenphone@alice", null);

    assertNamespace("cached:@bob:phone@alice", null);
  }

  @Test
  public void testGetNamespaceReturnsNullForReservedSharedKeyPrefix() throws Exception {
    assertNamespace("shared_key.bob@alice", null);
  }

  @Test
  public void testGetNamespaceReturnsNamespaceForKeyStringWithNamespace() throws Exception {
    assertNamespace("public:location.ns@alice", "ns");
    assertNamespace("public:a.location.ns@alice", "ns");
    assertNamespace("a.b.selfkey1.ns@alice", "ns");
    assertNamespace("@bob:a.phone.ns@alice", "ns");
    assertNamespace("@bob:a.b.c.phone.ns@alice", "ns");

    assertNamespace("public:_a.hiddenlocation.ns@alice", "ns");
    assertNamespace("_a.hiddenselfkey1.ns@alice", "ns");
    assertNamespace("@bob:__a.b.hiddenphone.ns@alice", "ns");
  }

  //
  // migrated from AtClientValidationTest
  //

  @Test
  public void testInvalidKeyNameCauseExceptionsToBeThron() {

    Exception ex = assertThrows(IllegalArgumentException.class,
                                () -> Keys.publicKeyBuilder().sharedBy(AtSign.of("fred")).name("").build());
    assertThat(ex.getMessage(), equalTo("key name is blank"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> Keys.publicKeyBuilder().sharedBy(AtSign.of("fred")).name("test@").build());
    assertThat(ex.getMessage(), equalTo("illegal characters in key name"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> Keys.publicKeyBuilder().sharedBy(AtSign.of("fred")).name("te st").build());
    assertThat(ex.getMessage(), equalTo("illegal characters in key name"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> Keys.publicKeyBuilder().sharedBy(AtSign.of("fred")).name("te:st").build());
    assertThat(ex.getMessage(), equalTo("illegal characters in key name"));
  }

  @Test
  public void testPublicKeyInvalidMetadataThrowsException() {
    Exception ex = assertThrows(IllegalArgumentException.class, () -> Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(-1L)
        .ttb(0L)
        .ttr(-1L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttl cannot be negative"));

    ex = assertThrows(IllegalArgumentException.class, () -> Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(0L)
        .ttb(-1L)
        .ttr(-1L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttb cannot be negative"));

    ex = assertThrows(IllegalArgumentException.class, () -> Keys.publicKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(0L)
        .ttb(0L)
        .ttr(-2L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttr cannot be < -1"));
  }

  @Test
  public void testSelfKeyInvalidMetadataThrowsException() {
    Exception ex = assertThrows(IllegalArgumentException.class, () -> Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(-1L)
        .ttb(0L)
        .ttr(-1L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttl cannot be negative"));

    ex = assertThrows(IllegalArgumentException.class, () -> Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(0L)
        .ttb(-1L)
        .ttr(-1L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttb cannot be negative"));

    ex = assertThrows(IllegalArgumentException.class, () -> Keys.selfKeyBuilder()
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(0L)
        .ttb(0L)
        .ttr(-2L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttr cannot be < -1"));
  }

  @Test
  public void testSharedKeyInvalidMetadataThrowsException() {
    Exception ex = assertThrows(IllegalArgumentException.class, () -> Keys.sharedKeyBuilder()
        .sharedWith(AtSign.of("colin"))
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(-1L)
        .ttb(0L)
        .ttr(-1L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttl cannot be negative"));

    ex = assertThrows(IllegalArgumentException.class, () -> Keys.sharedKeyBuilder()
        .sharedWith(AtSign.of("colin"))
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(0L)
        .ttb(-1L)
        .ttr(-1L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttb cannot be negative"));

    ex = assertThrows(IllegalArgumentException.class, () -> Keys.sharedKeyBuilder()
        .sharedWith(AtSign.of("colin"))
        .sharedBy(AtSign.of("fred"))
        .name("key")
        .ttl(0L)
        .ttb(0L)
        .ttr(-2L)
        .build());
    assertThat(ex.getMessage(), equalTo("ttr cannot be < -1"));
  }

  private static void assertNamespace(String fullKeyName, String expected) {
    Keys.AtKey key = Keys.keyBuilder().rawKey(fullKeyName).build();
    assertThat(key.namespace(), equalTo(expected));
  }

}
