package org.atsign.client.impl.commands;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.util.EncryptionUtils;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.exceptions.AtKeyNotFoundException;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.atsign.client.api.AtClient.GetRequestOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicKeyCommandsTest {

  private Keys.PublicKey key;

  private AtKeys keys;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtils.generateRSAKeyPair())
        .build();
    key = Keys.publicKeyBuilder()
        .sharedBy(createAtSign("gary"))
        .name("test")
        .build();
  }

  @Test
  void testGetSharedByMe() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", createMockLookupResponse("public:test@gary", "hello world"))
        .build();

    String actual = PublicKeyCommands.get(executor, createAtSign("gary"), key, null);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetCachedSharedByMe() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", createMockLookupResponse("cached:public:test@gary", "hello world"))
        .build();

    PublicKeyCommands.get(executor, createAtSign("gary"), key, null);

    assertThat(key.metadata().isCached(), is(true));
  }

  @Test
  void testGetSharedByMeNoSuchKey() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", "error:AT0015:deliberate")
        .build();

    assertThrows(AtKeyNotFoundException.class, () -> PublicKeyCommands.get(executor, createAtSign("gary"), key, null));
  }

  @Test
  void testGetSharedByMeExecutionException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("llookup:all:public:test@gary", new ExecutionException("deliberate", null))
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeyCommands.get(executor, createAtSign("gary"), key, null));
  }

  @Test
  void testGetSharedByOther() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("plookup:all:test@gary", createMockLookupResponse("public:test@gary", "hello world"))
        .build();

    String actual = PublicKeyCommands.get(executor, createAtSign("colin"), key, null);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetCachedSharedByOther() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("plookup:all:test@gary", createMockLookupResponse("cached:public:test@gary", "hello world"))
        .build();

    PublicKeyCommands.get(executor, createAtSign("colin"), key, null);

    assertThat(key.metadata().isCached(), is(true));
  }

  @Test
  void testGetSharedByOtherBypassCache() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("plookup:bypassCache:true:all:test@gary", createMockLookupResponse("public:test@gary", "hello world"))
        .build();

    GetRequestOptions options = GetRequestOptions.builder().bypassCache(true).build();
    String actual = PublicKeyCommands.get(executor, createAtSign("colin"), key, options);

    assertThat(actual, equalTo("hello world"));
  }



  @Test
  void testGetSharedByOtherNoSuchKey() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("plookup:all:test@gary", "error:AT0015:deliberate")
        .build();

    assertThrows(AtKeyNotFoundException.class, () -> PublicKeyCommands.get(executor, createAtSign("colin"), key, null));
  }

  @Test
  void testGetSharedByOtherExecutionException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stubExecutionException("plookup:all:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeyCommands.get(executor, createAtSign("colin"), key, null));
  }

  @Test
  void testPutSendsExpectedCommands() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world", "data:123")
        .build();

    PublicKeyCommands.put(executor, keys, key, "hello world");
  }

  @Test
  void testPutThrowExceptionIfCommandFails() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world", "error:AT0001:deliberate")
        .build();

    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtils.generateRSAKeyPair())
        .build();

    assertThrows(AtServerRuntimeException.class, () -> PublicKeyCommands.put(executor, keys, key, "hello world"));
  }

  @Test
  void testPutExecutionException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stubExecutionException("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world")
        .build();

    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtils.generateRSAKeyPair())
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeyCommands.put(executor, keys, key, "hello world"));
  }

  private static String createMockLookupResponse(String key, String value) {
    return String.format("data:{" +
        "\"key\": \"%s\"," +
        "\"data\": \"%s\"," +
        "\"metaData\": {\"ttl\": 86400000, \"isBinary\": false, \"isEncrypted\": false, \"isPublic\": true}" +
        "}", key, value);
  }
}
