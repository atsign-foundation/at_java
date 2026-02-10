package org.atsign.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.atsign.client.util.EnrollmentId.createEnrollmentId;
import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtKeyNames;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.VerbBuilders.EnrollOperation;
import org.atsign.common.VerbBuilders.LookupOperation;
import org.atsign.common.VerbBuilders.NotifyOperation;
import org.atsign.common.VerbBuilders.UpdateCommandBuilder;
import org.junit.jupiter.api.Test;

public class VerbBuildersTest {

  @Test
  public void testFromBuilderGeneratesExpectedOutput() {

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.fromCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("atSign not set"));

    String command = VerbBuilders.fromCommandBuilder()
        .atSign(createAtSign("@bob"))
        .build();
    assertEquals("from:@bob", command);
  }

  @Test
  public void testCramBuilderGeneratesExpectedOutput() {

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.cramCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("digest not set"));

    String command = VerbBuilders.cramCommandBuilder()
        .digest("digest")
        .build();
    assertEquals("cram:digest", command);
  }

  @Test
  public void testPolBuilderGeneratesExpectedOutput() {
    String command = VerbBuilders.polCommandBuilder().build();
    assertEquals("pol", command);
  }

  @Test
  public void testPkamBuilderGeneratesExpectedOutput() {

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.pkamCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("digest not set"));

    String command = VerbBuilders.pkamCommandBuilder()
        .digest("digest")
        .build();
    assertEquals("pkam:digest", command);
  }

  @Test
  public void testPkamBuilderGeneratesExpectedOutputWhenEnrollmentIdIsSet() {

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.pkamCommandBuilder()
                                                   .digest("digest")
                                                   .enrollmentId(createEnrollmentId("12345-6789"))
                                                   .signingAlgo("RSA")
                                                   .build());
    assertThat(ex.getMessage(), containsString("hashingAlgo not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.pkamCommandBuilder()
        .digest("digest")
        .enrollmentId(createEnrollmentId("12345-6789"))
        .hashingAlgo("SHA")
        .build());
    assertThat(ex.getMessage(), containsString("signingAlgo not set"));

    String command = VerbBuilders.pkamCommandBuilder()
        .digest("digest")
        .enrollmentId(createEnrollmentId("12345-6789"))
        .signingAlgo("RSA")
        .hashingAlgo("SHA")
        .build();
    assertEquals("pkam:signingAlgo:RSA:hashingAlgo:SHA:enrollmentId:12345-6789:digest", command);
  }

  @Test
  public void testUpdateBuilderThrowsExceptionsWhenMandatoryFieldsAreNotSet() {

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.updateCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("keyName not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.updateCommandBuilder().keyName("test").build());
    assertThat(ex.getMessage(), containsString("sharedBy not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.updateCommandBuilder().keyName("test").sharedBy(createAtSign("fred")).build());
    assertThat(ex.getMessage(), containsString("value not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.updateCommandBuilder().isPublic(true).rawKey("@colin:key1@fred").build());
    assertThat(ex.getMessage(), containsString("both rawKeys and isHidden, isPublic isCached set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.updateCommandBuilder().keyName("test").rawKey("@colin:key1@fred").build());
    assertThat(ex.getMessage(), containsString("both rawKeys and key fields set"));
  }

  @Test
  public void testUpdateBuilderMetadataSettersOverrideKeyMetadata() {
    PublicKey key = Keys.publicKeyBuilder().sharedBy(createAtSign("fred")).name("test").build();
    UpdateCommandBuilder builder = VerbBuilders.updateCommandBuilder().key(key).value("x");

    builder = VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).value("x");
    assertThat(builder.isPublic(true).build(),
               equalTo("update:public:test@fred x"));
    assertThat(builder.isPublic(false).build(),
               equalTo("update:test@fred x"));

    builder = VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).value("x");
    assertThat(builder.isHidden(true).build(),
               equalTo("update:_test@fred x"));
    assertThat(builder.isHidden(false).build(),
               equalTo("update:test@fred x"));

    builder = VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).value("x");
    assertThat(builder.isCached(true).build(),
               equalTo("update:cached:test@fred x"));
    assertThat(builder.isCached(false).build(),
               equalTo("update:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.ttl(SECONDS.toMillis(1)).build(),
               equalTo("update:ttl:1000:public:test@fred x"));
    assertThat(builder.ttl(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.ttb(SECONDS.toMillis(2)).build(),
               equalTo("update:ttb:2000:public:test@fred x"));
    assertThat(builder.ttb(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.ttr(SECONDS.toMillis(3)).build(),
               equalTo("update:ttr:3000:public:test@fred x"));
    assertThat(builder.ttr(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.ccd(true).build(),
               equalTo("update:ccd:true:public:test@fred x"));
    assertThat(builder.ccd(false).build(),
               equalTo("update:ccd:false:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.isBinary(true).build(),
               equalTo("update:isBinary:true:public:test@fred x"));
    assertThat(builder.isBinary(false).build(),
               equalTo("update:isBinary:false:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.isEncrypted(true).build(),
               equalTo("update:isEncrypted:true:public:test@fred x"));
    assertThat(builder.isEncrypted(false).build(),
               equalTo("update:isEncrypted:false:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.dataSignature("XYZ").build(),
               equalTo("update:dataSignature:XYZ:public:test@fred x"));
    assertThat(builder.dataSignature(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.sharedKeyEnc("abcdef").build(),
               equalTo("update:sharedKeyEnc:abcdef:public:test@fred x"));
    assertThat(builder.sharedKeyEnc(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.pubKeyCS("xxxx").build(),
               equalTo("update:pubKeyCS:xxxx:public:test@fred x"));
    assertThat(builder.pubKeyCS(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.encoding("en").build(),
               equalTo("update:encoding:en:public:test@fred x"));
    assertThat(builder.encoding(null).build(),
               equalTo("update:public:test@fred x"));

    builder =
        VerbBuilders.updateCommandBuilder().keyName(key.name()).sharedBy(key.sharedBy()).isPublic(true).value("x");
    assertThat(builder.ivNonce("abc123op").build(),
               equalTo("update:ivNonce:abc123op:public:test@fred x"));
    assertThat(builder.ivNonce(null).build(),
               equalTo("update:public:test@fred x"));
  }

  @Test
  public void testUpdateBuilderThrowsExceptionIfMutuallyExclusiveFieldsHaveBeenSet() {
    PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(createAtSign("fred"))
        .name("test")
        .build();

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      VerbBuilders.updateCommandBuilder()
          .key(key)
          .sharedBy(key.sharedBy())
          .ccd(true)
          .value("x")
          .build();
    });
    assertThat(ex.getMessage(), containsString("both key and key fields set"));

  }

  @Test
  public void testUpdateBuilderGeneratesExpectedOutput() {
    String command;

    // self key
    command = VerbBuilders.updateCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@bob"))
        .value("my Value 123")
        .build();
    assertEquals("update:test@bob my Value 123", command);

    // self key but shared with self
    command = VerbBuilders.updateCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("bob"))
        .sharedWith(createAtSign("bob"))
        .value("My value 123")
        .build();
    assertEquals("update:@bob:test@bob My value 123", command);

    // public key
    command = VerbBuilders.updateCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("bob"))
        .isPublic(true)
        .value("my Value 123")
        .build();
    assertEquals("update:public:publickey@bob my Value 123", command);

    // cached public key
    command = VerbBuilders.updateCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("alice"))
        .isPublic(true)
        .isCached(true)
        .value("my Value 123")
        .build();
    assertEquals("update:cached:public:publickey@alice my Value 123", command);

    // shared key
    command = VerbBuilders.updateCommandBuilder()
        .keyName("sharedkey")
        .sharedBy(createAtSign("@bob"))
        .sharedWith(createAtSign("@alice"))
        .value("my Value 123")
        .build();
    assertEquals("update:@alice:sharedkey@bob my Value 123", command);

    // with shared key
    SharedKey sk1 = Keys.sharedKeyBuilder()
        .sharedBy(new AtSign("@bob"))
        .sharedWith(new AtSign("@alice"))
        .name("test")
        .ttl(TimeUnit.MINUTES.toMillis(10))
        .isBinary(true)
        .build();
    command = VerbBuilders.updateCommandBuilder()
        .key(sk1)
        .value("myBinaryValue123456")
        .build();
    assertEquals("update:ttl:600000:isBinary:true:isEncrypted:true:@alice:test@bob myBinaryValue123456", command);

    // with public key
    PublicKey pk1 = Keys.publicKeyBuilder()
        .sharedBy(new AtSign("@bob"))
        .name("test")
        .isCached(true)
        .build();
    command = VerbBuilders.updateCommandBuilder()
        .key(pk1)
        .value("myValue123")
        .build();
    assertEquals("update:isEncrypted:false:cached:public:test@bob myValue123", command);

    // with self key
    SelfKey sk2 = Keys.selfKeyBuilder()
        .sharedBy(new AtSign("@bob"))
        .name("test")
        .ttl(TimeUnit.MINUTES.toMillis(10))
        .build();
    command = VerbBuilders.updateCommandBuilder()
        .key(sk2)
        .value("myValue123")
        .build();
    assertEquals("update:ttl:600000:isEncrypted:true:test@bob myValue123", command);

    // with self key (shared with self)
    AtSign bob = createAtSign("@bob");
    SelfKey sk3 = Keys.selfKeyBuilder()
        .sharedBy(bob)
        .sharedWith(bob)
        .name("test")
        .ttl(TimeUnit.MINUTES.toMillis(10))
        .build();
    command = VerbBuilders.updateCommandBuilder()
        .key(sk3)
        .value("myValue123")
        .build();
    assertEquals("update:ttl:600000:isEncrypted:true:@bob:test@bob myValue123", command);

    // private hidden key
    // TODO with private hidden key when implemented
  }

  @Test
  public void testUpdateBuilderGeneratesExpectedOutputForPublicKeyWithNamespace() {
    PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.updateCommandBuilder()
        .key(key)
        .value("testvalue")
        .build();

    assertThat(command, equalTo("update:isEncrypted:false:public:test.testns@alice testvalue"));
  }

  @Test
  public void testUpdateBuilderGeneratesExpectedOutputForSelfKeyWithNamespace() {
    SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.updateCommandBuilder()
        .key(key)
        .value("testvalue")
        .build();

    assertThat(command, equalTo("update:isEncrypted:true:test.testns@alice testvalue"));
  }

  @Test
  public void testUpdateBuilderGeneratesExpectedOutputForSharedKeyWithNamespace() {
    SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .sharedWith(new AtSign("@bob"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.updateCommandBuilder()
        .key(key)
        .value("testvalue")
        .build();

    assertThat(command, equalTo("update:isEncrypted:true:@bob:test.testns@alice testvalue"));
  }

  @Test
  public void testUpdateBuilderGeneratesExpectedOutputForSharedEncryption() {
    AtSign sharedBy = createAtSign("sharedBy");
    AtSign sharedWith = createAtSign("sharedWith");

    String command = VerbBuilders.updateCommandBuilder()
        .keyName(AtKeyNames.toSharedByMeKeyName(sharedWith))
        .sharedBy(sharedBy)
        .value("XXXX")
        .build();

    assertThat(command, equalTo("update:shared_key.sharedWith@sharedBy XXXX"));

    command = VerbBuilders.updateCommandBuilder()
        .keyName(AtKeyNames.SHARED_KEY)
        .sharedBy(sharedBy)
        .sharedWith(sharedWith)
        .ttr(TimeUnit.HOURS.toMillis(24))
        .value("XXXX")
        .build();

    assertThat(command, equalTo("update:ttr:86400000:@sharedWith:shared_key@sharedBy XXXX"));
  }

  @Test
  public void testUpdateBuilderGeneratesExpectedOutputForPublicEncyrptionKey() {
    String command = VerbBuilders.updateCommandBuilder()
        .sharedBy(createAtSign("fred"))
        .keyName(AtKeyNames.PUBLIC_ENCRYPT)
        .isPublic(true)
        .value("XXXX")
        .build();
    assertThat(command, equalTo("update:public:publickey@fred XXXX"));
  }

  @Test
  public void testLlookupBuilderGeneratesExpectedOutput() {
    String command;

    // Type.NONE self key
    command = VerbBuilders.llookupCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .build();
    assertEquals("llookup:test@alice", command);

    // Type.METADATA self key
    command = VerbBuilders.llookupCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .operation(LookupOperation.meta)
        .build();
    assertEquals("llookup:meta:test@alice", command);

    // hidden self key, meta
    command = VerbBuilders.llookupCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .operation(LookupOperation.meta)
        .isHidden(true)
        .build();
    assertEquals("llookup:meta:_test@alice", command);

    // Type.ALL public cached key
    command = VerbBuilders.llookupCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("@alice"))
        .isCached(true)
        .isPublic(true)
        .operation(LookupOperation.all)
        .build();
    assertEquals("llookup:all:cached:public:publickey@alice", command);

    // no key name
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.llookupCommandBuilder()
                                                   .sharedBy(createAtSign("@alice")).build());
    assertThat(ex.getMessage(), containsString("keyName not set"));

    // no shared by
    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.llookupCommandBuilder().keyName("test").build());
    assertThat(ex.getMessage(), containsString("sharedBy not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.llookupCommandBuilder().keyName("test").rawKey("public:publickey@alice")
                          .build());
    assertThat(ex.getMessage(), containsString("both rawKey and key fields are set"));

    // with public key
    PublicKey pk = Keys.publicKeyBuilder().sharedBy(new AtSign("@bob")).name("publickey").build();
    command = VerbBuilders.llookupCommandBuilder()
        .key(pk)
        .operation(LookupOperation.meta)
        .build();
    assertEquals("llookup:meta:public:publickey@bob", command);
    command = VerbBuilders.llookupCommandBuilder()
        .rawKey("public:publickey@bob")
        .operation(LookupOperation.meta)
        .build();
    assertEquals("llookup:meta:public:publickey@bob", command);

    // with shared key
    SharedKey sk = Keys.sharedKeyBuilder()
        .sharedBy(new AtSign("@bob"))
        .sharedWith(new AtSign("@alice"))
        .name("sharedkey")
        .build();
    command = VerbBuilders.llookupCommandBuilder()
        .key(sk)
        .operation(LookupOperation.none)
        .build();
    assertEquals("llookup:@alice:sharedkey@bob", command);

    // with self key
    SelfKey selfKey1 = Keys.selfKeyBuilder().sharedBy(new AtSign("@bob")).name("test").build();
    command = VerbBuilders.llookupCommandBuilder()
        .key(selfKey1)
        .operation(LookupOperation.all)
        .build(); // "llookup:all:test@bob"
    assertEquals("llookup:all:test@bob", command);

    // with self key (shared with self)
    AtSign as = new AtSign("@bob");
    SelfKey selfKey2 = Keys.selfKeyBuilder().sharedBy(as).sharedWith(as).name("test").build();
    command = VerbBuilders.llookupCommandBuilder()
        .key(selfKey2)
        .operation(LookupOperation.all)
        .build();
    assertEquals("llookup:all:@bob:test@bob", command);

    // with cached public key
    PublicKey pk2 = Keys.publicKeyBuilder()
        .sharedBy(new AtSign("@bob"))
        .name("publickey")
        .isCached(true)
        .build();
    command = VerbBuilders.llookupCommandBuilder()
        .key(pk2)
        .operation(LookupOperation.all)
        .build();
    assertEquals("llookup:all:cached:public:publickey@bob", command);

    // with cached shared key
    SharedKey sk2 = Keys.sharedKeyBuilder()
        .sharedBy(new AtSign("@bob"))
        .sharedWith(new AtSign("@alice"))
        .name("sharedkey")
        .isCached(true)
        .build();
    command = VerbBuilders.llookupCommandBuilder()
        .key(sk2)
        .operation(LookupOperation.none)
        .build();
    assertEquals("llookup:cached:@alice:sharedkey@bob", command);

    // with private hidden key
    // TODO: not implemented yet

  }

  @Test
  public void testLlookupBuilderGeneratesExpectedOutputForPublicKeyWithNamespace() {
    PublicKey key = Keys.publicKeyBuilder().sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.llookupCommandBuilder()
        .key(key)
        .operation(LookupOperation.meta)
        .build();
    assertThat(command, equalTo("llookup:meta:public:test.testns@alice"));
  }

  @Test
  public void testLlookupBuilderGeneratesExpectedOutputForSelfKeyWithNamespace() {
    SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.llookupCommandBuilder()
        .key(key)
        .operation(LookupOperation.meta)
        .build();
    assertThat(command, equalTo("llookup:meta:test.testns@alice"));
  }

  @Test
  public void testLlookupBuilderGeneratesExpectedOutputForSharedKeyWithNamespace() {
    SharedKey key = Keys.sharedKeyBuilder().sharedBy(new AtSign("@alice"))
        .sharedWith(new AtSign("@bob"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.llookupCommandBuilder()
        .key(key)
        .operation(LookupOperation.meta)
        .build();
    assertThat(command, equalTo("llookup:meta:@bob:test.testns@alice"));
  }

  @Test
  public void lookupVerbBuilderTest() {
    String command;

    // Type.NONE
    command = VerbBuilders.lookupCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .build();
    assertEquals("lookup:test@alice", command);
    command = VerbBuilders.lookupCommandBuilder()
        .rawKey("test@alice")
        .build();
    assertEquals("lookup:test@alice", command);

    // Type.METADATA
    command = VerbBuilders.lookupCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .operation(LookupOperation.meta)
        .build();
    assertEquals("lookup:meta:test@alice", command);

    // Type.ALL
    command = VerbBuilders.lookupCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .operation(LookupOperation.all)
        .build(); // "lookup:test@alice"
    assertEquals("lookup:all:test@alice", command);

    // no key name
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class,
                     () -> VerbBuilders.lookupCommandBuilder().sharedBy(createAtSign("@alice")).build());
    assertThat(ex.getMessage(), containsString("keyName not set"));

    // no sharedBy
    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.lookupCommandBuilder().keyName("test").build());
    assertThat(ex.getMessage(), containsString("sharedBy not set"));

    // with shared key
    SharedKey sk = Keys.sharedKeyBuilder().sharedBy(new AtSign("@sharedby"))
        .sharedWith(new AtSign("@sharedwith"))
        .name("test")
        .build();
    command = VerbBuilders.lookupCommandBuilder()
        .key(sk)
        .operation(LookupOperation.meta)
        .build();
    assertEquals("lookup:meta:test@sharedby", command);
  }

  @Test
  public void testLookupVerbBuilderForSharedKeyWithNamespace() {
    SharedKey key = Keys.sharedKeyBuilder().sharedBy(new AtSign("@alice"))
        .sharedWith(new AtSign("@bob"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.lookupCommandBuilder()
        .key(key)
        .operation(LookupOperation.meta)
        .build();
    assertThat(command, equalTo("lookup:meta:test.testns@alice"));
  }

  @Test
  public void plookupVerbBuilderTest() {
    String command;

    // Type.NONE
    command = VerbBuilders.plookupCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("@alice"))
        .build(); // "plookup:publickey@alice"
    assertEquals("plookup:publickey@alice", command);
    command = VerbBuilders.plookupCommandBuilder()
        .rawKey("publickey@alice")
        .build(); // "plookup:publickey@alice"
    assertEquals("plookup:publickey@alice", command);

    // Type.METADATA
    command = VerbBuilders.plookupCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("@alice"))
        .operation(LookupOperation.meta)
        .build();
    assertEquals("plookup:meta:publickey@alice", command);

    // Type.ALL
    command = VerbBuilders.plookupCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("@alice"))
        .operation(LookupOperation.all)
        .build();
    assertEquals("plookup:all:publickey@alice", command);

    // no key
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class,
                     () -> VerbBuilders.plookupCommandBuilder().sharedBy(createAtSign("@alice"))
                         .operation(LookupOperation.all)
                         .build());
    assertThat(ex.getMessage(), containsString("keyName not set"));
    // no shared by
    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.plookupCommandBuilder().keyName("publickey").operation(LookupOperation.all)
                          .build());
    assertThat(ex.getMessage(), containsString("sharedBy not set"));

    // with
    PublicKey pk = Keys.publicKeyBuilder().sharedBy(new AtSign("@bob")).name("publickey").build();
    command = VerbBuilders.plookupCommandBuilder()
        .key(pk)
        .operation(LookupOperation.all)
        .build();
    assertEquals("plookup:all:publickey@bob", command);

    // bypasscache true
    command = VerbBuilders.plookupCommandBuilder()
        .keyName("publickey")
        .sharedBy(createAtSign("@alice"))
        .bypassCache(true)
        .operation(LookupOperation.all)
        .build();
    assertEquals("plookup:bypassCache:true:all:publickey@alice", command);
  }

  @Test
  public void testPlookupVerbBuilderForPublicKeyWithNamespace() {
    PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.plookupCommandBuilder()
        .key(key)
        .operation(LookupOperation.meta)
        .build();
    assertThat(command, equalTo("plookup:meta:test.testns@alice"));
  }


  @Test
  public void deleteVerbBuilderTest() {
    String command;

    // delete a public key
    command = VerbBuilders.deleteCommandBuilder()
        .isPublic(true)
        .keyName("publickey")
        .sharedBy(createAtSign("@alice"))
        .build();
    assertEquals("delete:public:publickey@alice", command);
    command = VerbBuilders.deleteCommandBuilder()
        .rawKey("public:publickey@alice")
        .build();
    assertEquals("delete:public:publickey@alice", command);

    // delete a cached public key
    command = VerbBuilders.deleteCommandBuilder()
        .isCached(true)
        .isPublic(true)
        .keyName("publickey")
        .sharedBy(createAtSign("@bob"))
        .build();
    assertEquals("delete:cached:public:publickey@bob", command);

    // delete a self key
    command = VerbBuilders.deleteCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .build();
    assertEquals("delete:test@alice", command);

    // delete a hidden self key
    command = VerbBuilders.deleteCommandBuilder()
        .isHidden(true)
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .build();
    assertEquals("delete:_test@alice", command);

    // delete a shared key
    command = VerbBuilders.deleteCommandBuilder()
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .sharedWith(createAtSign("@bob"))
        .build();
    assertEquals("delete:@bob:test@alice", command);

    // delete a cached shared key
    command = VerbBuilders.deleteCommandBuilder()
        .isCached(true)
        .keyName("test")
        .sharedBy(createAtSign("@alice"))
        .sharedWith(createAtSign("@bob"))
        .build();
    assertEquals("delete:cached:@bob:test@alice", command);

    // missing key name
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.deleteCommandBuilder()
                                                   .sharedBy(createAtSign("@alice"))
                                                   .sharedWith(createAtSign("@bob"))
                                                   .build());
    assertThat(ex.getMessage(), containsString("keyName not set"));

    // missing shared by
    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.deleteCommandBuilder().keyName("test").build());
    assertThat(ex.getMessage(), containsString("sharedBy not set"));

    // with self key
    SelfKey selfKey = Keys.selfKeyBuilder().sharedBy(new AtSign("@alice")).name("test").build();
    command = VerbBuilders.deleteCommandBuilder()
        .key(selfKey)
        .build();
    assertEquals("delete:test@alice", command);

    // with public key
    PublicKey pk = Keys.publicKeyBuilder().sharedBy(new AtSign("@bob")).name("publickey").build();
    command = VerbBuilders.deleteCommandBuilder()
        .key(pk)
        .build();

    // with shared key
    SharedKey sk = Keys.sharedKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .sharedWith(new AtSign("@bob"))
        .name("test")
        .build();
    command = VerbBuilders.deleteCommandBuilder()
        .key(sk)
        .build();
    assertEquals("delete:@bob:test@alice", command);
  }

  @Test
  public void testDeleteVerbBuilderForPublicKeyWithNamespace() {
    PublicKey key = Keys.publicKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.deleteCommandBuilder()
        .key(key)
        .build();
    assertThat(command, equalTo("delete:public:test.testns@alice"));
  }

  @Test
  public void testDeleteVerbBuilderForSelfKeyWithNamespace() {
    SelfKey key = Keys.selfKeyBuilder()
        .sharedBy(new AtSign("@alice"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.deleteCommandBuilder()
        .key(key)
        .build();
    assertThat(command, equalTo("delete:test.testns@alice"));
  }

  @Test
  public void testDeleteVerbBuilderForSharedKeyWithNamespace() {
    SharedKey key = Keys.sharedKeyBuilder()
        .sharedBy(new AtSign("@alice")).sharedWith(new AtSign("@bob"))
        .name("test")
        .namespace("testns")
        .build();
    String command = VerbBuilders.deleteCommandBuilder()
        .key(key)
        .build();
    assertThat(command, equalTo("delete:@bob:test.testns@alice"));
  }

  @Test
  public void scanVerbBuilderTest() {

    // Test not setting any parameters
    String command = VerbBuilders.scanCommandBuilder().build();
    assertEquals("scan", command);

    // Test setting just regex
    command = VerbBuilders.scanCommandBuilder().regex("*.public")
        .build();
    assertEquals("scan *.public", command);

    // Test setting just fromAtSign
    command = VerbBuilders.scanCommandBuilder()
        .fromAtSign(createAtSign("@other"))
        .build();
    assertEquals("scan:@other", command);

    // Test seting just showHidden
    command = VerbBuilders.scanCommandBuilder()
        .showHidden(true)
        .build();
    assertEquals("scan:showHidden:true", command);

    // Test setting regex & fromAtSign
    command = VerbBuilders.scanCommandBuilder()
        .regex("*.public")
        .fromAtSign(createAtSign("@other"))
        .build();
    assertEquals("scan:@other *.public", command);

    // Test setting regex & showHidden
    command = VerbBuilders.scanCommandBuilder()
        .regex("*.public")
        .showHidden(true)
        .build();
    assertEquals("scan:showHidden:true *.public", command);

    // Test setting fromAtSign & showHidden
    command = VerbBuilders.scanCommandBuilder()
        .fromAtSign(createAtSign("@other"))
        .showHidden(true)
        .build();
    assertEquals("scan:showHidden:true:@other", command);

    // Test setting regex & fromAtSign & showHidden
    command = VerbBuilders.scanCommandBuilder()
        .regex("*.public")
        .fromAtSign(createAtSign("@other"))
        .showHidden(true)
        .build();
    assertEquals("scan:showHidden:true:@other *.public", command);
  }

  @Test
  public void testNotifyTextBuilderGeneratesTheExpectedOutput() {
    // Test not setting any parameters
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> VerbBuilders.notifyTextCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("recipient not set"));

    // Test not setting the text
    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.notifyTextCommandBuilder().recipient(createAtSign("@somebody")).build());
    assertThat(ex.getMessage(), containsString("text not set"));

    String command = VerbBuilders.notifyTextCommandBuilder()
        .recipient(createAtSign("@test"))
        .text("Hi")
        .build();
    assertEquals("notify:messageType:text:@test:Hi", command);
  }

  @Test
  public void notifyKeyChangeBuilderTest() {
    // Test not setting any parameters
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.notifyKeyChangeCommandBuilder()
                                                   .operation(NotifyOperation.update)
                                                   .sender(createAtSign("sender"))
                                                   .recipient(createAtSign("recipient"))
                                                   .build());
    assertThat(ex.getMessage(), containsString("key not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.notifyKeyChangeCommandBuilder().key("key").build());
    assertThat(ex.getMessage(), containsString("operation not set"));

    // Test setting the value when ttr has been set
    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.notifyKeyChangeCommandBuilder()
        .operation(NotifyOperation.update)
        .sender(createAtSign("sender"))
        .recipient(createAtSign("recipient"))
        .key("phone")
        .ttr(10000L)
        .build());
    assertThat(ex.getMessage(), containsString("value not set (mandatory when ttr is set)"));

    // Test setting invalid ttr
    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.notifyKeyChangeCommandBuilder()
        .operation(NotifyOperation.update)
        .sender(createAtSign("sender"))
        .recipient(createAtSign("recipient"))
        .key("phone")
        .ttr(-100L)
        .build());
    assertThat(ex.getMessage(), containsString("ttr < -1"));

    // test command
    String command = VerbBuilders.notifyKeyChangeCommandBuilder()
        .operation(NotifyOperation.update)
        .sender(createAtSign("sender"))
        .recipient(createAtSign("recipient"))
        .key("phone")
        .build();
    assertEquals("notify:update:messageType:key:@recipient:phone@sender", command);

    // test command with a fully formed key
    command = VerbBuilders.notifyKeyChangeCommandBuilder()
        .operation(NotifyOperation.update)
        .key("@recipient:phone@sender")
        .build();
    assertEquals("notify:update:messageType:key:@recipient:phone@sender", command);

    // test command when ttr and value are present
    command = VerbBuilders.notifyKeyChangeCommandBuilder()
        .operation(NotifyOperation.update)
        .key("@recipient:phone@sender")
        .ttr(1000L)
        .value("cache_me")
        .build();
    assertEquals("notify:update:messageType:key:ttr:1000:@recipient:phone@sender:cache_me", command);
  }

  @Test
  public void notificationStatusVerbBuilderTest() {

    // Test not setting any parameters
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> VerbBuilders.notifyStatusCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("notificationId not set"));

    String command = VerbBuilders.notifyStatusCommandBuilder()
        .notificationId("n1234").build();
    assertEquals("notify:status:n1234", command);
  }

  @Test
  void testEnrollThrowExceptionIfOperationIsNotSet() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("operation not set"));
  }

  @Test
  void testEnrollListReturnsExpectedCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.list)
        .build();

    assertThat(result, is("enroll:list"));
  }

  @Test
  void testEnrollListWithStatusProducesCommandWithStatusFilter() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.list)
        .status("pending")
        .build();

    assertThat(result, is("enroll:list{\"enrollmentStatusFilter\":[\"pending\"]}"));
  }

  @Test
  void testEnrollApproveThrowsIfFieldsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.approve)
        .encryptPrivateKey("privKey")
        .encryptPrivateKeyIv("privKeyIv")
        .selfEncryptKey("selfKey")
        .selfEncryptKeyIv("selfKeyIv")
        .build());
    assertThat(ex.getMessage(), containsString("enrollmentId not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.approve)
        .enrollmentId(createEnrollmentId("abc123"))
        .encryptPrivateKeyIv("privKeyIv")
        .selfEncryptKey("selfKey")
        .selfEncryptKeyIv("selfKeyIv")
        .build());
    assertThat(ex.getMessage(), containsString("encryptPrivateKey not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.approve)
        .enrollmentId(createEnrollmentId("abc123"))
        .encryptPrivateKey("privKey")
        .selfEncryptKey("selfKey")
        .selfEncryptKeyIv("selfKeyIv")
        .build());

    assertThat(ex.getMessage(), containsString("encryptPrivateKeyIv not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.approve)
        .enrollmentId(createEnrollmentId("abc123"))
        .encryptPrivateKey("privKey")
        .encryptPrivateKeyIv("privKeyIv")
        .selfEncryptKeyIv("selfKeyIv")
        .build());

    assertThat(ex.getMessage(), containsString("selfEncryptKey not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.approve)
        .enrollmentId(createEnrollmentId("abc123"))
        .encryptPrivateKey("privKey")
        .encryptPrivateKeyIv("privKeyIv")
        .selfEncryptKey("selfKey")
        .build());

    assertThat(ex.getMessage(), containsString("selfEncryptKeyIv not set"));

  }

  @Test
  void testEnrollApproveWithAllRequiredParamsReturnsExpectedCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.approve)
        .enrollmentId(createEnrollmentId("abc123"))
        .encryptPrivateKey("privKey")
        .encryptPrivateKeyIv("privKeyIv")
        .selfEncryptKey("selfKey")
        .selfEncryptKeyIv("selfKeyIv")
        .build();

    assertThat(result, is("enroll:approve{" +
        "\"enrollmentId\":\"abc123\"," +
        "\"encryptedDefaultEncryptionPrivateKey\":\"privKey\"," +
        "\"encPrivateKeyIV\":\"privKeyIv\"," +
        "\"encryptedDefaultSelfEncryptionKey\":\"selfKey\"," +
        "\"selfEncKeyIV\":\"selfKeyIv\"" +
        "}"));
  }

  @Test
  void testEnrollFetchThrowsExpectedExceptionWhenFieldsAreNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.fetch)
        .build());

    assertThat(ex.getMessage(), containsString("enrollmentId not set"));
  }

  @Test
  void testEnrollFetchWithEnrollmentIdReturnsExpectedCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.fetch)
        .enrollmentId(createEnrollmentId("abc123"))
        .build();

    assertThat(result, is("enroll:fetch{\"enrollmentId\":\"abc123\"}"));
  }

  @Test
  void testEnrollDenyThrowExceptionIfEnrollmentIdIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.deny)
        .build());

    assertThat(ex.getMessage(), containsString("enrollmentId not set"));
  }

  @Test
  void testEnrollDenyWithEnrollmentIdProducesCorrectCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.deny)
        .enrollmentId(createEnrollmentId("abc123"))
        .build();

    assertThat(result, is("enroll:deny{\"enrollmentId\":\"abc123\"}"));
  }

  @Test
  void testEnrollRevokeThrowExceptionIfEnrollmentIdIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.revoke)
        .build());

    assertThat(ex.getMessage(), containsString("enrollmentId not set"));
  }

  @Test
  void testEnrollRevokeWithEnrollmentIdProducesCorrectCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.revoke)
        .enrollmentId(createEnrollmentId("abc123"))
        .build();

    assertThat(result, is("enroll:revoke{\"enrollmentId\":\"abc123\"}"));
  }

  @Test
  void testEnrollUnrevokeThrowExceptionIfEnrollmentIdIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.unrevoke)
        .build());

    assertThat(ex.getMessage(), containsString("enrollmentId not set"));
  }

  @Test
  void testEnrollUnrevokeWithEnrollmentIdProducesCorrectCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.unrevoke)
        .enrollmentId(createEnrollmentId("abc123"))
        .build();

    assertThat(result, is("enroll:unrevoke{\"enrollmentId\":\"abc123\"}"));
  }

  @Test
  void testEnrollDeleteThrowExceptionIfEnrollmentIdIsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.delete)
        .build());

    assertThat(ex.getMessage(), containsString("enrollmentId not set"));
  }

  @Test
  void testEnrollDeleteWithEnrollmentIdProducesCorrectCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.delete)
        .enrollmentId(createEnrollmentId("abc123"))
        .build();

    assertThat(result, is("enroll:delete{\"enrollmentId\":\"abc123\"}"));
  }

  @Test
  void testEnrollRequestThrowExceptionIfFieldsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .deviceName("myDevice")
        .apkamPublicKey("pubKey")
        .build());
    assertThat(ex.getMessage(), containsString("appName not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .appName("")
        .deviceName("myDevice")
        .apkamPublicKey("pubKey")
        .build());
    assertThat(ex.getMessage(), containsString("appName not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .appName("myApp")
        .apkamPublicKey("pubKey")
        .build());
    assertThat(ex.getMessage(), containsString("deviceName not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .appName("myApp")
        .deviceName("")
        .apkamPublicKey("pubKey")
        .build());
    assertThat(ex.getMessage(), containsString("deviceName not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .appName("myApp")
        .deviceName("myDevice")
        .build());
    assertThat(ex.getMessage(), containsString("apkamPublicKey not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .otp("")
        .appName("myApp")
        .deviceName("myDevice")
        .apkamPublicKey("pubKey")
        .build());
    assertThat(ex.getMessage(), containsString("otp not set"));

    ex = assertThrows(IllegalArgumentException.class, () -> VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .otp("AZ19")
        .appName("myApp")
        .deviceName("myDevice")
        .apkamPublicKey("pubKey")
        .build());
    assertThat(ex.getMessage(), containsString("namespaces not set"));
  }

  @Test
  void testInitialEnrollRequestReturnsExpectedCommand() {
    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .appName("myApp")
        .deviceName("myDevice")
        .apkamPublicKey("pubKey")
        .build();

    assertThat(result, is("enroll:request{" +
        "\"appName\":\"myApp\"," +
        "\"deviceName\":\"myDevice\"," +
        "\"apkamPublicKey\":\"pubKey\"" +
        "}"));
  }

  @Test
  void testSubsequentEnrollRequestReturnsExpectedCommand() {
    Map<String, String> namespaces = new LinkedHashMap<>();
    namespaces.put("ns1", "rw");

    String result = VerbBuilders.enrollCommandBuilder()
        .operation(EnrollOperation.request)
        .appName("myApp")
        .deviceName("myDevice")
        .apkamPublicKey("pubKey")
        .apkamSymmetricKey("symKey")
        .otp("123456")
        .namespaces(namespaces)
        .ttl(86400000L)
        .build();

    assertThat(result, is("enroll:request{" +
        "\"appName\":\"myApp\"," +
        "\"deviceName\":\"myDevice\"," +
        "\"apkamPublicKey\":\"pubKey\"," +
        "\"encryptedAPKAMSymmetricKey\":\"symKey\"," +
        "\"otp\":\"123456\"," +
        "\"namespaces\":{\"ns1\":\"rw\"}," +
        "\"apkamKeysExpiryInMillis\":86400000" +
        "}"));
  }

  @Test
  public void testOtpBuilderGeneratesExpectedOutput() {
    String command = VerbBuilders.otpCommandBuilder().build();
    assertEquals("otp:get", command);
  }

  @Test
  void testKeysBuilderThrowsExceptionIfFieldsNotSet() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> VerbBuilders.keysCommandBuilder().build());
    assertThat(ex.getMessage(), containsString("operation not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.keysCommandBuilder().operation(VerbBuilders.KeysOperation.get).build());
    assertThat(ex.getMessage(), containsString("keyName not set"));

    ex = assertThrows(IllegalArgumentException.class,
                      () -> VerbBuilders.keysCommandBuilder()
                          .operation(VerbBuilders.KeysOperation.delete)
                          .keyName("private:_secret@fred")
                          .build());
    assertThat(ex.getMessage(), containsString("delete not supported"));
  }


  @Test
  void testKeysBuilderReturnsExpectedCommand() {
    String command = VerbBuilders.keysCommandBuilder()
        .operation(VerbBuilders.KeysOperation.get)
        .keyName("private:_secret@fred")
        .build();
    assertThat(command, equalTo("keys:get:keyName:private:_secret@fred"));
  }

}
