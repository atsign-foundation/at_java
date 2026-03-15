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
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelfKeyCommandsTest {

  private Keys.SelfKey key;

  private AtKeys keys;
  private AtSign atSign;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .selfEncryptKey(generateAESKeyBase64())
        .encryptKeyPair(generateRSAKeyPair())
        .build();
    atSign = createAtSign("gary");
    key = Keys.selfKeyBuilder()
        .sharedBy(atSign)
        .name("test")
        .build();
  }

  @Test
  void testGet() throws Exception {

    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello me", keys.getSelfEncryptKey(), iv);
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("llookup:all:test@gary", createMockLookupResponse("test@gary", encrypted, iv))
        .build();

    String actual = SelfKeyCommands.get(executor, atSign, keys, key);

    assertThat(actual, equalTo("hello me"));
  }

  @Test
  void testGetException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("llookup:all:test@gary", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> SelfKeyCommands.get(executor, atSign, keys, key));
  }

  @Test
  void testGetExecutionException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stubExecutionException("llookup:all:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> SelfKeyCommands.get(executor, atSign, keys, key));
  }

  @Test
  void testPut() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+", "data:123")
        .build();

    SelfKeyCommands.put(executor, atSign, keys, key, "hello world");
    verify(executor).sendSync(argThat(s -> !s.contains("hello world")));
  }

  @Test
  void testPutAtException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> SelfKeyCommands.put(executor, atSign, keys, key, "hello world"));
  }

  @Test
  void testPutExecutionException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stubExecutionException("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+")
        .build();

    assertThrows(RuntimeException.class, () -> SelfKeyCommands.put(executor, atSign, keys, key, "hello world"));
  }

  private static String createMockLookupResponse(String key, String encrypted, String iv) {

    return String.format("data:{" +
        "\"key\": \"%s\"," +
        "\"data\": \"%s\"," +
        "\"metaData\": {\"ttl\": 86400000, \"ivNonce\": \"%s\", \"isEncrypted\": true, \"isPublic\": false}" +
        "}", key, encrypted, iv);
  }

}
