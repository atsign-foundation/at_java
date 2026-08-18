package org.atsign.client.impl.commands;

import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelfKeyCommandsTest {

  private Keys.SelfKey key;

  private AtKeys keys;
  private AtSign atSign;
  private AtCommandExecutorContext context;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .selfEncryptKey(generateAESKeyBase64())
        .encryptKeyPair(generateRSAKeyPair())
        .build();
    atSign = createAtSign("gary");
    context = new AtCommandExecutorContext(atSign, keys);
    key = Keys.selfKeyBuilder()
        .sharedBy(atSign)
        .name("test")
        .build();
  }

  @Test
  void testGet() throws Exception {

    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello me", keys.getSelfEncryptKey(), iv);
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:test@gary", "test@gary", encrypted, "ivNonce", iv)
        .build();

    String actual = SelfKeyCommands.get(executor, context, key);

    assertThat(actual, equalTo("hello me"));
  }

  @Test
  void testGetException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:all:test@gary", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> SelfKeyCommands.get(executor, context, key));
  }

  @Test
  void testGetExecutionException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubExecutionException("llookup:all:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> SelfKeyCommands.get(executor, context, key));
  }

  @Test
  void testPut() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+", "data:123")
        .build();

    SelfKeyCommands.put(executor, context, key, "hello world");
    verify(executor).sendSync(argThat(s -> !s.contains("hello world")));
  }

  @Test
  void testPutAtException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class,
                 () -> SelfKeyCommands.put(executor, context, key, "hello world"));
  }

  @Test
  void testPutExecutionException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubExecutionException("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+")
        .build();

    assertThrows(RuntimeException.class,
                 () -> SelfKeyCommands.put(executor, context, key, "hello world"));
  }

}
