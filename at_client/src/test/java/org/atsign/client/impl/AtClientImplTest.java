package org.atsign.client.impl;

import static org.atsign.client.api.AtEvents.AtEventType.statsNotification;
import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.Consumer;

import org.atsign.client.api.*;
import org.atsign.client.impl.commands.TestExecutorBuilder;
import org.atsign.client.impl.util.Base2e15Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class AtClientImplTest {

  private AtKeys keys;
  private AtEvents.AtEventBus bus;
  private AtCommandExecutor executor;
  private AtClientImpl client;
  private AtSign atSign;

  @BeforeEach
  void setUp() throws Exception {
    bus = Mockito.mock(AtEvents.AtEventBus.class);
    executor = TestExecutorBuilder.builder().build();
    atSign = AtSign.of("test");
    keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .encryptKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();
    client = AtClientImpl.builder()
        .atSign(atSign)
        .keys(keys)
        .executor(executor)
        .eventBus(bus)
        .build();
  }

  @AfterEach
  void tearDown() {}

  @Test
  void testSetAtSignReturnsConstructorArg() {
    AtClientImpl client = AtClientImpl.builder()
        .atSign(AtSign.of("test"))
        .keys(keys)
        .executor(executor)
        .eventBus(bus)
        .build();
    assertThat(client.getAtSign(), equalTo(AtSign.of("test")));
  }

  @Test
  void testGetCommandExecutorReturnsConstructorArg() {
    AtClientImpl client = AtClientImpl.builder()
        .atSign(AtSign.of("test"))
        .keys(keys)
        .executor(executor)
        .eventBus(bus)
        .build();
    assertThat(client.getCommandExecutor(), sameInstance(executor));
  }

  @Test
  void testCloseInvokesCloseOnExecutor() throws Exception {
    client.close();

    verify(executor).close();
  }

  @Test
  void testStartMonitor() throws Exception {

    // stub the executor so that onReady consumer successfully authenticates
    executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();

    AtClientImpl client = AtClientImpl.builder()
        .atSign(AtSign.of("alice"))
        .keys(keys)
        .executor(executor)
        .eventBus(bus)
        .build();

    assertThat(client.isMonitorRunning(), is(false));

    client.startMonitor();

    // verify onReady is called
    ArgumentCaptor<Consumer<AtCommandExecutor>> onReadyCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(executor).onReady(onReadyCaptor.capture());

    // explicitly invoke the onReady consumer
    onReadyCaptor.getValue().accept(executor);

    // verify monitor is called
    ArgumentCaptor<Consumer<String>> monitorCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(executor).sendSync(eq("monitor"), monitorCaptor.capture());

    // invoke the consumer and verify that eventBus is invoked
    monitorCaptor.getValue().accept("notification:{\"id\":\"-1\",\"from\":\"@gary\"}");

    // verify eventBus publish event was invoked
    Map<String, Object> expected = new HashMap<>();
    expected.put("id", "-1");
    expected.put("from", "@gary");
    verify(bus).publishEvent(eq(statsNotification), eq(expected));

    assertThat(client.isMonitorRunning(), is(true));
  }

  @Test
  void testStopMonitor() throws Exception {
    // stub the executor so that onReady consumer successfully authenticates
    executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();

    AtClientImpl client = AtClientImpl.builder()
        .atSign(AtSign.of("alice"))
        .keys(keys)
        .executor(executor)
        .eventBus(bus)
        .build();

    assertThat(client.isMonitorRunning(), is(false));

    client.startMonitor();
    client.stopMonitor();

    // verify onReady has been called twice
    ArgumentCaptor<Consumer<AtCommandExecutor>> onReadyCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(executor, times(2)).onReady(onReadyCaptor.capture());

    // explicitly invoke the onReady consumer
    onReadyCaptor.getAllValues().get(1).accept(executor);

    // verify monitor command is NOT sent
    verify(executor, never()).sendSync(eq("monitor"), ArgumentMatchers.any(Consumer.class));

    assertThat(client.isMonitorRunning(), is(false));
  }

  @Test
  void testAddEventListenerDelegatesToEventBus() {
    AtEvents.AtEventListener listener = mock(AtEvents.AtEventListener.class);
    Set<AtEvents.AtEventType> eventTypes = Set.of(AtEvents.AtEventType.values());

    client.addEventListener(listener, eventTypes);

    verify(bus).addEventListener(listener, eventTypes);
  }

  @Test
  void testRemoveEventListenerDelegatesToEventBus() {
    AtEvents.AtEventListener listener = mock(AtEvents.AtEventListener.class);

    client.removeEventListener(listener);

    verify(bus).removeEventListener(listener);
  }

  @Test
  void testPublishEventDelegatesToEventBus() {
    Map<String, Object> eventData = Collections.singletonMap("id", "-1");
    client.publishEvent(statsNotification, eventData);

    verify(bus).publishEvent(statsNotification, eventData);
  }

  @Test
  void testGetSelfKey() throws Exception {
    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64("hello me", keys.getSelfEncryptKey(), iv);
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:key1@test", "key1@test", encrypted, "ivNonce", iv)
        .stub("llookup:all:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("llookup:all:key3@test")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();

    Keys.SelfKey key1 = Keys.selfKeyBuilder().sharedBy(atSign).name("key1").build();
    assertThat(client.get(key1), equalTo("hello me"));

    Keys.SelfKey key2 = Keys.selfKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.get(key2));

    Keys.SelfKey key3 = Keys.selfKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.get(key3));
  }

  @Test
  void testGetSelfKeyBinary() throws Exception {
    byte[] bytes = new byte[15];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));

    String iv = generateRandomIvBase64(16);
    String encrypted = aesEncryptToBase64(Base2e15Utils.encode(bytes), keys.getSelfEncryptKey(), iv);
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:key1@test", "key1@test", encrypted, "ivNonce", iv, "isBinary", true)
        .stubLookupResponse("llookup:all:key4@test", "key4@test", encrypted, "ivNonce", iv)
        .stub("llookup:all:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("llookup:all:key3@test")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();

    Keys.SelfKey key1 = Keys.selfKeyBuilder().sharedBy(atSign).name("key1").build();
    assertThat(client.getBinary(key1), equalTo(bytes));

    Keys.SelfKey key2 = Keys.selfKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.getBinary(key2));

    Keys.SelfKey key3 = Keys.selfKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.getBinary(key3));

    Keys.SelfKey key4 = Keys.selfKeyBuilder().sharedBy(atSign).name("key4").build();
    Exception ex = assertThrows(Exception.class, () -> client.getBinary(key4));
    assertThat(ex.getMessage(), containsString("metadata.isBinary not set to true"));
  }

  @Test
  void testPutSelfKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:true:ivNonce:.+:key1@test .+", "data:123")
        .stub("update:dataSignature:.+:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("update:dataSignature:.+:key3@test .+")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    Keys.SelfKey key1 = Keys.selfKeyBuilder().sharedBy(atSign).name("key1").build();
    client.put(key1, "hello world");

    Keys.SelfKey key2 = Keys.selfKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.put(key2, "hello world"));

    Keys.SelfKey key3 = Keys.selfKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.put(key3, "hello world"));
  }

  @Test
  void testPutSelfKeyBytes() throws Exception {
    byte[] bytes = new byte[15];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isBinary:true:isEncrypted:true:ivNonce:.+:key1@test .+", "data:123")
        .stub("update:.+:key2@test .+", "error:AT0001:deliberate")
        .stubExecutionException("update:.+:key3@test .+")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    Keys.SelfKey key1 = Keys.selfKeyBuilder().sharedBy(atSign).name("key1").build();
    client.put(key1, bytes);

    Keys.SelfKey key2 = Keys.selfKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.put(key2, bytes));

    Keys.SelfKey key3 = Keys.selfKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.put(key3, bytes));
  }

  @Test
  void testDeleteSelfKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:key1@test", "data:123")
        .stub("delete:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("delete:key3@test")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    Keys.SelfKey key1 = Keys.selfKeyBuilder().sharedBy(atSign).name("key1").build();
    client.delete(key1);

    Keys.SelfKey key2 = Keys.selfKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.delete(key2));

    Keys.SelfKey key3 = Keys.selfKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.delete(key3));
  }

  @Test
  void testGetPublicKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:public:key1@test", "key1@test", "hello world")
        .stub("llookup:all:public:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("llookup:all:public:key3@test")
        .stubLookupResponse("plookup:all:key4@another", "key4@test", "greetings from another world")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();

    Keys.PublicKey key1 = Keys.publicKeyBuilder().sharedBy(atSign).name("key1").build();
    assertThat(client.get(key1), equalTo("hello world"));

    Keys.PublicKey key2 = Keys.publicKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.get(key2));

    Keys.PublicKey key3 = Keys.publicKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.get(key3));

    Keys.PublicKey key4 = Keys.publicKeyBuilder().sharedBy(AtSign.of("another")).name("key4").build();
    assertThat(client.get(key4), equalTo("greetings from another world"));
  }

  @Test
  void testGetPublicKeyBinary() throws Exception {
    byte[] bytes1 = new byte[15];
    Arrays.fill(bytes1, (byte) Integer.parseInt("10101010", 2));
    byte[] bytes2 = new byte[15];
    Arrays.fill(bytes2, (byte) Integer.parseInt("11110000", 2));

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stubLookupResponse("llookup:all:public:key1@test", "key1@test", Base2e15Utils.encode(bytes1), "isBinary", true)
        .stub("llookup:all:public:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("llookup:all:public:key3@test")
        .stubLookupResponse("plookup:all:key4@another", "key4@test", Base2e15Utils.encode(bytes2), "isBinary", true)
        .stubLookupResponse("llookup:all:public:key5@test", "key5@test", Base2e15Utils.encode(bytes1))
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();

    Keys.PublicKey key1 = Keys.publicKeyBuilder().sharedBy(atSign).name("key1").build();
    assertThat(client.getBinary(key1), equalTo(bytes1));

    Keys.PublicKey key2 = Keys.publicKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.getBinary(key2));

    Keys.PublicKey key3 = Keys.publicKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.getBinary(key3));

    Keys.PublicKey key4 = Keys.publicKeyBuilder().sharedBy(AtSign.of("another")).name("key4").build();
    assertThat(client.getBinary(key4), equalTo(bytes2));

    Keys.PublicKey key5 = Keys.publicKeyBuilder().sharedBy(atSign).name("key5").build();
    assertThrows(Exception.class, () -> client.getBinary(key5));
  }

  @Test
  void testPutPublicKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:key1@test hello world", "data:123")
        .stub("update:dataSignature:.+:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("update:dataSignature:.+:key3@test .+")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    Keys.PublicKey key1 = Keys.publicKeyBuilder().sharedBy(atSign).name("key1").build();
    client.put(key1, "hello world");

    Keys.PublicKey key2 = Keys.publicKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.put(key2, "hello world"));

    Keys.PublicKey key3 = Keys.publicKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.put(key3, "hello world"));
  }

  @Test
  void testPutPublicKeyBytes() throws Exception {
    byte[] bytes = new byte[15];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));

    String encoded = Base2e15Utils.encode(bytes);
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("update:dataSignature:.+:isEncrypted:false:public:key1@test " + encoded, "data:123")
        .stub("update:dataSignature:.+:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("update:dataSignature:.+:key3@test .+")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    Keys.PublicKey key1 = Keys.publicKeyBuilder().sharedBy(atSign).name("key1").build();
    client.put(key1, bytes);

    Keys.PublicKey key2 = Keys.publicKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.put(key2, bytes));

    Keys.PublicKey key3 = Keys.publicKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.put(key3, bytes));
  }

  @Test
  void testDeletePublicKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:public:key1@test", "data:123")
        .stub("delete:public:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("delete:public:key3@test")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    Keys.PublicKey key1 = Keys.publicKeyBuilder().sharedBy(atSign).name("key1").build();
    client.delete(key1);

    Keys.PublicKey key2 = Keys.publicKeyBuilder().sharedBy(atSign).name("key2").build();
    assertThrows(Exception.class, () -> client.delete(key2));

    Keys.PublicKey key3 = Keys.publicKeyBuilder().sharedBy(atSign).name("key3").build();
    assertThrows(Exception.class, () -> client.delete(key3));
  }


  @Test
  void testGetSharedKey() throws Exception {
    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted1 = aesEncryptToBase64("hello from me", encryptKey, iv);
    String encrypted2 = aesEncryptToBase64("greetings from another world", encryptKey, iv);
    String sharedKeyEnc = rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey());

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.another@test", "data:" + sharedKeyEnc)
        .stubLookupResponse("llookup:all:@another:key1@test", "key1@test", encrypted1,
                            "ivNonce", iv, "sharedKeyEnc", sharedKeyEnc)
        .stub("llookup:all:public:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("llookup:all:public:key3@test")
        .stubLookupResponse("lookup:all:key4@another", "key4@test", encrypted2,
                            "ivNonce", iv, "sharedKeyEnc", sharedKeyEnc)
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    AtSign atSign2 = AtSign.of("another");

    Keys.SharedKey key1 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key1").build();
    assertThat(client.get(key1), equalTo("hello from me"));

    Keys.SharedKey key2 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key2").build();
    assertThrows(Exception.class, () -> client.get(key2));

    Keys.SharedKey key3 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key3").build();
    assertThrows(Exception.class, () -> client.get(key3));

    Keys.SharedKey key4 = Keys.sharedKeyBuilder().sharedBy(atSign2).sharedWith(atSign).name("key4").build();
    assertThat(client.get(key4), equalTo("greetings from another world"));
  }

  @Test
  void testGetSharedKeyBinary() throws Exception {
    byte[] bytes1 = new byte[15];
    Arrays.fill(bytes1, (byte) Integer.parseInt("10101010", 2));
    byte[] bytes2 = new byte[15];
    Arrays.fill(bytes2, (byte) Integer.parseInt("11110000", 2));

    String encryptKey = generateAESKeyBase64();
    String iv = generateRandomIvBase64(16);
    String encrypted1 = aesEncryptToBase64(Base2e15Utils.encode(bytes1), encryptKey, iv);
    String encrypted2 = aesEncryptToBase64(Base2e15Utils.encode(bytes2), encryptKey, iv);

    String sharedKeyEnc = rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey());
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.another@test", "data:" + sharedKeyEnc)
        .stubLookupResponse("llookup:all:@another:key1@test", "key1@test", encrypted1,
                            "ivNonce", iv, "isBinary", true, "sharedKeyEnc", sharedKeyEnc)
        .stub("llookup:all:public:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("llookup:all:public:key3@test")
        .stubLookupResponse("lookup:all:key4@another", "key4@test", encrypted2,
                            "ivNonce", iv, "isBinary", true, "sharedKeyEnc", sharedKeyEnc)
        .stubLookupResponse("llookup:all:@another:key5@test", "key5@test", encrypted1, "ivNonce", iv)
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    AtSign atSign2 = AtSign.of("another");

    Keys.SharedKey key1 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key1").build();
    assertThat(client.getBinary(key1), equalTo(bytes1));

    Keys.SharedKey key2 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key2").build();
    assertThrows(Exception.class, () -> client.getBinary(key2));

    Keys.SharedKey key3 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key3").build();
    assertThrows(Exception.class, () -> client.getBinary(key3));

    Keys.SharedKey key4 = Keys.sharedKeyBuilder().sharedBy(atSign2).sharedWith(atSign).name("key4").build();
    assertThat(client.getBinary(key4), equalTo(bytes2));

    Keys.SharedKey key5 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key5").build();
    assertThrows(Exception.class, () -> client.getBinary(key5));
  }

  @Test
  void testPutSharedKey() throws Exception {
    String encryptKey = generateAESKeyBase64();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.another@test", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("update:isEncrypted:true:.+@another:key1@test .+", "data:123")
        .stub("update:.+:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("update:.+:key3@test .+")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    AtSign atSign2 = AtSign.of("another");

    Keys.SharedKey key1 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key1").build();
    client.put(key1, "hello world");

    Keys.SharedKey key2 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key2").build();
    assertThrows(Exception.class, () -> client.put(key2, "hello world"));

    Keys.SharedKey key3 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key3").build();
    assertThrows(Exception.class, () -> client.put(key3, "hello world"));
  }

  @Test
  void testPutSharedKeyBytes() throws Exception {
    byte[] bytes = new byte[15];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));

    String encryptKey = generateAESKeyBase64();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("llookup:shared_key.another@test", "data:" + rsaEncryptToBase64(encryptKey, keys.getEncryptPublicKey()))
        .stub("update:isBinary:true:isEncrypted:true:.+@another:key1@test .+", "data:123")
        .stub("update:.+:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("update:.+:key3@test .+")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    AtSign atSign2 = AtSign.of("another");

    Keys.SharedKey key1 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key1").build();
    client.put(key1, bytes);

    Keys.SharedKey key2 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key2").build();
    assertThrows(Exception.class, () -> client.put(key2, bytes));

    Keys.SharedKey key3 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key3").build();
    assertThrows(Exception.class, () -> client.put(key3, bytes));
  }

  @Test
  void testDeleteSharedKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:@another:key1@test", "data:123")
        .stub("delete:@another:key2@test", "error:AT0001:deliberate")
        .stubExecutionException("delete:@another:key3@test")
        .build();

    AtClientImpl client = AtClientImpl.builder().atSign(atSign).keys(keys).executor(executor).eventBus(bus).build();
    AtSign atSign2 = AtSign.of("another");

    Keys.SharedKey key1 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key1").build();
    client.delete(key1);

    Keys.SharedKey key2 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key2").build();
    assertThrows(Exception.class, () -> client.delete(key2));

    Keys.SharedKey key3 = Keys.sharedKeyBuilder().sharedBy(atSign).sharedWith(atSign2).name("key3").build();
    assertThrows(Exception.class, () -> client.delete(key3));
  }

}
