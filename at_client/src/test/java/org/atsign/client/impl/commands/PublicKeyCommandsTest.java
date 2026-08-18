package org.atsign.client.impl.commands;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient.GetRequestOptions;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.exceptions.AtKeyNotFoundException;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.atsign.client.impl.util.EncryptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicKeyCommandsTest {

  private Keys.PublicKey key;
  private AtKeys keys;
  private AtSign atSign;

  // the key is shared by @gary, so @gary reads it with llookup and anyone else with plookup
  private AtCommandExecutorContext contextGary;
  private AtCommandExecutorContext contextColin;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .encryptKeyPair(EncryptionUtils.generateRSAKeyPair())
        .build();
    atSign = createAtSign("gary");
    key = Keys.publicKeyBuilder()
        .sharedBy(atSign)
        .name("test")
        .build();
    contextGary = new AtCommandExecutorContext(atSign, keys);
    contextColin = new AtCommandExecutorContext(createAtSign("colin"), null);
  }

  @Test
  void testGetSharedByMe() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:public:test@gary", "public:test@gary", "hello world")
        .build();

    String actual = PublicKeyCommands.get(executor, contextGary, key, null);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetCachedSharedByMe() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:public:test@gary", "cached:public:test@gary", "hello world")
        .build();

    PublicKeyCommands.get(executor, contextGary, key, null);

    assertThat(key.metadata().isCached(), is(true));
  }

  @Test
  void testGetSharedByMeNoSuchKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:all:public:test@gary", "error:AT0015:deliberate")
        .build();

    assertThrows(AtKeyNotFoundException.class, () -> PublicKeyCommands.get(executor, contextGary, key, null));
  }

  @Test
  void testGetSharedByMeExecutionException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:all:public:test@gary", new ExecutionException("deliberate", null))
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeyCommands.get(executor, contextGary, key, null));
  }

  @Test
  void testGetSharedByOther() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("plookup:all:test@gary", "public:test@gary", "hello world")
        .build();

    String actual = PublicKeyCommands.get(executor, contextColin, key, null);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetCachedSharedByOther() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("plookup:all:test@gary", "cached:public:test@gary", "hello world")
        .build();

    PublicKeyCommands.get(executor, contextColin, key, null);

    assertThat(key.metadata().isCached(), is(true));
  }

  @Test
  void testGetSharedByOtherBypassCache() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("plookup:bypassCache:true:all:test@gary", "public:test@gary", "hello world")
        .build();

    GetRequestOptions options = GetRequestOptions.builder().bypassCache(true).build();
    String actual = PublicKeyCommands.get(executor, contextColin, key, options);

    assertThat(actual, equalTo("hello world"));
  }

  @Test
  void testGetSharedByOtherNoSuchKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("plookup:all:test@gary", "error:AT0015:deliberate")
        .build();

    assertThrows(AtKeyNotFoundException.class, () -> PublicKeyCommands.get(executor, contextColin, key, null));
  }

  @Test
  void testGetSharedByOtherExecutionException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubExecutionException("plookup:all:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> PublicKeyCommands.get(executor, contextColin, key, null));
  }

  @Test
  void testPutSendsExpectedCommands() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world", "data:123")
        .build();

    PublicKeyCommands.put(executor, contextGary, key, "hello world");
  }

  @Test
  void testPutThrowExceptionIfCommandFails() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class,
                 () -> PublicKeyCommands.put(executor, contextGary, key, "hello world"));
  }

  @Test
  void testPutExecutionException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubExecutionException("update:dataSignature:.+:isEncrypted:false:public:test@gary hello world")
        .build();

    assertThrows(RuntimeException.class,
                 () -> PublicKeyCommands.put(executor, contextGary, key, "hello world"));
  }
}
