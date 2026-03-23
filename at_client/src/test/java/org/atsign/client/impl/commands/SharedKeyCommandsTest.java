package org.atsign.client.impl.commands;

import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedKeyCommandsTest {

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
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stubLookupResponse("llookup:all:@colin:test@gary", "@colin:test@gary", encrypted, "ivNonce", iv)
        .build();

    String actual = SharedKeyCommands.get(executor, createAtSign("gary"), keys, key);

    assertThat(actual, equalTo("hello colin"));
  }

  @Test
  void testGetNotSharedByMeOrWithMeThrowsException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .build();

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> SharedKeyCommands.get(executor, createAtSign("alice"), keys, key));
    assertThat(ex.getMessage(), containsString("@alice is neither the sharedBy or sharedWith of @colin:test@gary"));
  }

  @Test
  void testGetSharedByMeCachedKey() throws Exception {
    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello colin", encryptKey, iv);
    keys.put("shared_key.colin", encryptKey);
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:@colin:test@gary", "@colin:test@gary", encrypted, "ivNonce", iv)
        .build();

    String actual = SharedKeyCommands.get(executor, createAtSign("gary"), keys, key);

    assertThat(actual, equalTo("hello colin"));
  }

  @Test
  void testGetSharedByMeServerException() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("llookup:all:@colin:test@gary", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class,
                 () -> SharedKeyCommands.get(executor, createAtSign("gary"), keys, key));
  }

  @Test
  void testGetSharedByMeExecutionException() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stubExecutionException("llookup:all:@colin:test@gary")
        .build();

    assertThrows(RuntimeException.class, () -> SharedKeyCommands.get(executor, createAtSign("gary"), keys, key));
  }

  @Test
  void getGetSharedByOther() throws Exception {
    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello colin", encryptKey, iv);
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("lookup:all:test@gary", "@colin:test@gary", encrypted, "ivNonce", iv)
        .stub("lookup:shared_key@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .build();

    String actual = SharedKeyCommands.get(executor, createAtSign("colin"), keys, key);

    assertThat(actual, equalTo("hello colin"));
  }

  @Test
  void testPutWhenSharedKeyAlreadyExists() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("update:isEncrypted:true:ivNonce:.+:@colin:test@gary .+", "data:123")
        .build();

    SharedKeyCommands.put(executor, createAtSign("gary"), keys, key, "hello colin");
    verify(executor).sendSync(argThat(s -> s.contains("update:") && !s.contains("hello colin")));
  }

  @Test
  void testPutWhenSharedKeDoesNotAlreadyExists() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.colin@gary", "error:AT0015:deliberate")
        .stub("plookup:publickey@colin", "data:" + keys.getEncryptPublicKey())
        .stub("update:shared_key.colin@gary .+", "data:1")
        .stub("update:ttr:86400000:@colin:shared_key@gary .+", "data:2")
        .stub("update:isEncrypted:true:ivNonce:.+:@colin:test@gary .+", "data:3")
        .build();

    SharedKeyCommands.put(executor, createAtSign("gary"), keys, key, "hello colin");
  }
}
