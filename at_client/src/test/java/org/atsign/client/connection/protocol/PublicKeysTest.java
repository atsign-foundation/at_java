package org.atsign.client.connection.protocol;

import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.common.Keys;
import org.atsign.common.exceptions.AtKeyNotFoundException;
import org.atsign.common.exceptions.AtExceptions.AtServerRuntimeException;
import org.atsign.common.options.GetRequestOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicKeysTest {

  private Keys.PublicKey key;

  private AtKeys keys;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtil.generateRSAKeyPair())
        .build();
    key = Keys.publicKeyBuilder()
        .sharedBy(createAtSign("gary"))
        .name("test")
        .build();
  }

  @Test
  void testGetSharedByMe() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", createMockLookupResponse("public:test@gary", "hello world"))
        .build();

    String actual = PublicKeys.get(connection, createAtSign("gary"), key, null);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetCachedSharedByMe() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", createMockLookupResponse("cached:public:test@gary", "hello world"))
        .build();

    PublicKeys.get(connection, createAtSign("gary"), key, null);

    assertThat(key.metadata().isCached(), is(true));
  }

  @Test
  void testGetSharedByMeNoSuchKey() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", "error:AT0015:deliberate")
        .build();

    assertThrows(AtKeyNotFoundException.class, () -> PublicKeys.get(connection, createAtSign("gary"), key, null));
  }

  @Test
  void testGetSharedByMeExecutionException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", new ExecutionException("deliberate", null))
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeys.get(connection, createAtSign("gary"), key, null));
  }

  @Test
  void testGetSharedByOther() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("plookup:all:test@gary", createMockLookupResponse("public:test@gary", "hello world"))
        .build();

    String actual = PublicKeys.get(connection, createAtSign("colin"), key, null);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetCachedSharedByOther() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("plookup:all:test@gary", createMockLookupResponse("cached:public:test@gary", "hello world"))
        .build();

    PublicKeys.get(connection, createAtSign("colin"), key, null);

    assertThat(key.metadata().isCached(), is(true));
  }

  @Test
  void testGetSharedByOtherBypassCache() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("plookup:bypassCache:true:all:test@gary", createMockLookupResponse("public:test@gary", "hello world"))
        .build();

    GetRequestOptions options = GetRequestOptions.builder().bypassCache(true).build();
    String actual = PublicKeys.get(connection, createAtSign("colin"), key, options);

    assertThat(actual, equalTo("hello world"));
  }



  @Test
  void testGetSharedByOtherNoSuchKey() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("plookup:all:test@gary", "error:AT0015:deliberate")
        .build();

    assertThrows(AtKeyNotFoundException.class, () -> PublicKeys.get(connection, createAtSign("colin"), key, null));
  }

  @Test
  void testGetSharedByOtherExecutionException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stubExecutionException("plookup:all:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeys.get(connection, createAtSign("colin"), key, null));
  }

  @Test
  void testPutSendsExpectedCommands() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world", "data:123")
        .build();

    PublicKeys.put(connection, keys, key, "hello world");
  }

  @Test
  void testPutThrowExceptionIfCommandFails() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world", "error:AT0001:deliberate")
        .build();

    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtil.generateRSAKeyPair())
        .build();

    assertThrows(AtServerRuntimeException.class, () -> PublicKeys.put(connection, keys, key, "hello world"));
  }

  @Test
  void testPutExecutionException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stubExecutionException("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world")
        .build();

    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtil.generateRSAKeyPair())
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeys.put(connection, keys, key, "hello world"));
  }

  private static String createMockLookupResponse(String key, String value) {
    return String.format("data:{" +
        "\"key\": \"%s\"," +
        "\"data\": \"%s\"," +
        "\"metaData\": {\"ttl\": 86400000, \"isBinary\": false, \"isEncrypted\": false, \"isPublic\": true}" +
        "}", key, value);
  }
}
