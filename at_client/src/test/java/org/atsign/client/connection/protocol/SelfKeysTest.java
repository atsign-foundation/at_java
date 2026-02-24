package org.atsign.client.connection.protocol;

import static org.atsign.client.util.EncryptionUtil.*;
import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.Keys;
import org.atsign.common.exceptions.AtExceptions.AtServerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelfKeysTest {

  private Keys.SelfKey key;

  private AtKeys keys;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .selfEncryptKey(generateAESKeyBase64())
        .encryptKeyPair(generateRSAKeyPair())
        .build();
    key = Keys.selfKeyBuilder()
        .sharedBy(createAtSign("gary"))
        .name("test")
        .build();
  }

  @Test
  void testGet() throws Exception {

    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello me", keys.getSelfEncryptKey(), iv);
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:test@gary", createMockLookupResponse("test@gary", encrypted, iv))
        .build();

    String actual = SelfKeys.get(connection, keys, key);

    assertThat(actual, equalTo("hello me"));
  }

  @Test
  void testGetException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:test@gary", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> SelfKeys.get(connection, keys, key));
  }

  @Test
  void testGetExecutionException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stubExecutionException("llookup:all:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> SelfKeys.get(connection, keys, key));
  }

  @Test
  void testPut() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+", "data:123")
        .build();

    SelfKeys.put(connection, keys, key, "hello world");
    verify(connection).sendSync(argThat(s -> !s.contains("hello world")));
  }

  @Test
  void testPutAtException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> SelfKeys.put(connection, keys, key, "hello world"));
  }

  @Test
  void testPutExecutionException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stubExecutionException("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:test@gary .+")
        .build();

    assertThrows(RuntimeException.class, () -> SelfKeys.put(connection, keys, key, "hello world"));
  }

  private static String createMockLookupResponse(String key, String encrypted, String iv) {

    return String.format("data:{" +
        "\"key\": \"%s\"," +
        "\"data\": \"%s\"," +
        "\"metaData\": {\"ttl\": 86400000, \"ivNonce\": \"%s\", \"isEncrypted\": true, \"isPublic\": false}" +
        "}", key, encrypted, iv);
  }

}
