package org.atsign.client.connection.protocol;

import static org.atsign.client.util.EncryptionUtil.*;
import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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

class SharedKeysTest {

  private Keys.SharedKey key;

  private AtKeys keys;

  @BeforeEach
  public void setup() throws Exception {
    keys = AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();
    key = Keys.sharedKeyBuilder()
        .sharedBy(createAtSign("gary"))
        .sharedWith(createAtSign("colin"))
        .name("test")
        .build();
  }

  @Test
  void testGetSharedByMe() throws Exception {
    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello colin", encryptKey, iv);
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("llookup:all:@colin:test@gary", createMockLookupResponse("@colin:test@gary", encrypted, iv))
        .build();

    String actual = SharedKeys.get(connection, createAtSign("gary"), keys, key);

    assertThat(actual, equalTo("hello colin"));
  }

  @Test
  void testGetNotSharedByMeOrWithMeThrowsException() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .build();

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> SharedKeys.get(connection, createAtSign("alice"), keys, key));
    assertThat(ex.getMessage(), containsString("the client atsign is neither the sharedBy or sharedWith"));
  }

  @Test
  void testGetSharedByMeCachedKey() throws Exception {
    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello colin", encryptKey, iv);
    keys.put("shared_key.colin", encryptKey);
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:all:@colin:test@gary", createMockLookupResponse("@colin:test@gary", encrypted, iv))
        .build();

    String actual = SharedKeys.get(connection, createAtSign("gary"), keys, key);

    assertThat(actual, equalTo("hello colin"));
  }

  @Test
  void testGetSharedByMeServerException() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("llookup:all:@colin:test@gary", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> SharedKeys.get(connection, createAtSign("gary"), keys, key));
  }

  @Test
  void testGetSharedByMeExecutionException() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stubExecutionException("llookup:all:@colin:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> SharedKeys.get(connection, createAtSign("gary"), keys, key));
  }

  @Test
  void getGetSharedByOther() throws Exception {
    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello colin", encryptKey, iv);
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("lookup:all:test@gary", createMockLookupResponse("@colin:test@gary", encrypted, iv))
        .stub("lookup:shared_key@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .build();

    String actual = SharedKeys.get(connection, createAtSign("colin"), keys, key);

    assertThat(actual, equalTo("hello colin"));
  }

  @Test
  void testPutWhenSharedKeyAlreadyExists() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("update:isEncrypted:true:ivNonce:.+:@colin:test@gary .+", "data:123")
        .build();

    SharedKeys.put(connection, createAtSign("gary"), keys, key, "hello colin");
    verify(connection).sendSync(argThat(s -> s.contains("update:") && !s.contains("hello colin")));
  }

  @Test
  void testPutWhenSharedKeDoesNotAlreadyExists() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "error:AT0015:deliberate")
        .stub("plookup:publickey@colin", "data:" + keys.getEncryptPublicKey())
        .stub("update:shared_key.colin@gary .+", "data:1")
        .stub("update:ttr:86400000:@colin:shared_key@gary .+", "data:2")
        .stub("update:isEncrypted:true:ivNonce:.+:@colin:test@gary .+", "data:3")
        .build();

    SharedKeys.put(connection, createAtSign("gary"), keys, key, "hello colin");
  }

  private static String createMockLookupResponse(String key, String value) {
    return String.format("data:{" +
        "\"key\": \"%s\"," +
        "\"data\": \"%s\"," +
        "\"metaData\": {\"ttl\": 86400000, \"isEncrypted\": false, \"isPublic\": true}" +
        "}", key, value);
  }


  private static String createMockLookupResponse(String key, String encrypted, String iv) {
    return String.format("data:{" +
        "\"key\": \"%s\"," +
        "\"data\": \"%s\"," +
        "\"metaData\": {\"ttl\": 86400000, \"ivNonce\": \"%s\", \"isEncrypted\": true, \"isPublic\": false}" +
        "}", key, encrypted, iv);
  }

}
