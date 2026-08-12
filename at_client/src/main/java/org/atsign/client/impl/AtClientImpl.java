package org.atsign.client.impl;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;
import static org.atsign.client.impl.util.EncryptionUtils.aesDecryptFromBase64;
import static org.atsign.client.impl.util.EncryptionUtils.rsaDecryptFromBase64;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.atsign.client.api.*;
import org.atsign.client.api.AtEvents.AtEventBus;
import org.atsign.client.api.AtEvents.AtEventListener;
import org.atsign.client.api.AtEvents.AtEventType;
import org.atsign.client.api.Keys.AtKey;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.api.Keys.SelfKey;
import org.atsign.client.api.Keys.SharedKey;
import org.atsign.client.impl.commands.*;
import org.atsign.client.impl.exceptions.AtDecryptionException;
import org.atsign.client.impl.exceptions.AtException;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.atsign.client.impl.util.Base2e15Utils;

/**
 * Implementation of an {@link AtClient} which uses a {@link AtCommandExecutor} and
 * the "composite" AtPlatform Protocol commands in {@link org.atsign.client.impl.commands}
 * with an embedded {@link AtEventBus}.
 * Example usage:
 *
 * <pre>
 * AtClientImplBuilder builder = AtClientImpl.builder()
 *     .context(...)
 *     .executor(...)
 *     .eventBus(...);
 *
 * try (AtClient client = builder.build()) {
 *     client.startMonitor();
 *     client.put(...);
 *     client.get(...);
 * }
 * </pre>
 */
@Slf4j
public class AtClientImpl implements AtClient {

  private final AtCommandExecutorContext context;
  private final MonitorOptions monitorOptions;
  private final AtCommandExecutor executor;
  private final AtEventBus eventBus;
  private final AtomicBoolean isMonitoring = new AtomicBoolean();
  private final Notifications.EventBusBridge eventBusBridge;

  @Override
  public AtSign getAtSign() {
    return context.getAtSign();
  }

  @Override
  public AtCommandExecutor getCommandExecutor() {
    return executor;
  }

  @Builder
  public AtClientImpl(AtCommandExecutorContext context,
                      boolean withMonitoring,
                      MonitorOptions monitorOptions,
                      AtCommandExecutor executor,
                      AtEventBus eventBus) {
    this.context = checkNotNull(context, "context not set");
    checkNotNull(context.getKeys(), "keys not set");
    checkNotNull(context.getKeys().getEncryptPrivateKey(), "keys have not been fully enrolled");
    this.monitorOptions = monitorOptions != null ? monitorOptions : MonitorOptions.builder().build();
    this.executor = checkNotNull(executor, "executor not set");
    this.eventBus = checkNotNull(eventBus, "eventBus not set");
    this.eventBus.addEventListener(this::handleEvent, EnumSet.allOf(AtEventType.class));
    this.isMonitoring.set(withMonitoring);
    this.eventBusBridge = new Notifications.EventBusBridge(eventBus, context.getAtSign(), this.monitorOptions);
  }

  /**
   * A builder for instantiating {@link AtCommandExecutor} implementations that are included in
   * this library. Example usage:
   *
   * <pre>
   *
   * AtClientImpl.builder()
   *   .context(...) // the connection context: atSign, keys and client config
   *   .executor()   // the AtCommandExecutor this client will use
   *   .eventBus()   // the AtEventBus this client will publish to
   *   .build();
   * }
   * </pre>
   *
   * <b>NOTE</b> the context must be the same instance the {@link AtCommandExecutor} was built with
   * (see {@link AtClients#createAtClient}), so that the {@code from:} challenge and the client config
   * are shared by the connection's authentication and by every command the client issues.
   */
  public static class AtClientImplBuilder {
    // required for javadoc
  }

  @Override
  public void close() throws Exception {
    executor.close();
  }

  @Override
  public void startMonitor() {
    isMonitoring.compareAndSet(false, true);
    executor.onReady(Notifications.monitor(context, monitorOptions, eventBusBridge));
  }

  @Override
  public void stopMonitor() {
    isMonitoring.compareAndSet(true, false);
    executor.onReady(AuthenticationCommands.pkamAuthenticator(context));
  }

  @Override
  public boolean isMonitorRunning() {
    return isMonitoring.get();
  }

  @Override
  public synchronized void addEventListener(AtEventListener listener, Set<AtEventType> eventTypes) {
    eventBus.addEventListener(listener, eventTypes);
  }

  @Override
  public synchronized void removeEventListener(AtEventListener listener) {
    eventBus.removeEventListener(listener);
  }

  @Override
  public int publishEvent(AtEventType eventType, Map<String, Object> eventData) {
    return eventBus.publishEvent(eventType, eventData);
  }

  @Override
  public String get(SharedKey sharedKey) throws AtException {
    return SharedKeyCommands.get(executor, context, sharedKey);
  }

  @Override
  public byte[] getBinary(SharedKey sharedKey) throws AtException {
    return Base2e15Utils.decode(SharedKeyCommands.get(executor, context, sharedKey, true));
  }

  @Override
  public void put(SharedKey sharedKey, String value) throws AtException {
    SharedKeyCommands.put(executor, context, sharedKey, value);
  }

  @Override
  public void delete(SharedKey sharedKey) throws AtException {
    KeyCommands.deleteKey(executor, sharedKey);
  }

  @Override
  public String get(SelfKey selfKey) throws AtException {
    return SelfKeyCommands.get(executor, context, selfKey);
  }

  @Override
  public byte[] getBinary(SelfKey selfKey) throws AtException {
    return Base2e15Utils.decode(SelfKeyCommands.get(executor, context, selfKey, true));
  }

  @Override
  public void put(SelfKey selfKey, String value) throws AtException {
    SelfKeyCommands.put(executor, context, selfKey, value);
  }

  @Override
  public void delete(SelfKey selfKey) throws AtException {
    KeyCommands.deleteKey(executor, selfKey);
  }

  @Override
  public String get(PublicKey publicKey) throws AtException {
    return get(publicKey, null);
  }

  @Override
  public String get(PublicKey publicKey, GetRequestOptions options) throws AtException {
    return PublicKeyCommands.get(executor, context, publicKey, options);
  }

  @Override
  public byte[] getBinary(PublicKey publicKey) throws AtException {
    return getBinary(publicKey, null);
  }

  @Override
  public byte[] getBinary(PublicKey publicKey, GetRequestOptions options) throws AtException {
    return Base2e15Utils.decode(PublicKeyCommands.get(executor, context, publicKey, true, options));
  }

  @Override
  public void put(PublicKey publicKey, String value) throws AtException {
    PublicKeyCommands.put(executor, context, publicKey, value);
  }

  @Override
  public void delete(PublicKey publicKey) throws AtException {
    KeyCommands.deleteKey(executor, publicKey);
  }

  @Override
  public void put(SharedKey sharedKey, byte[] value) throws AtException {
    put(setIsBinary(sharedKey), Base2e15Utils.encode(value));
  }

  @Override
  public void put(SelfKey selfKey, byte[] value) throws AtException {
    put(setIsBinary(selfKey), Base2e15Utils.encode(value));
  }

  @Override
  public void put(PublicKey publicKey, byte[] value) throws AtException {
    put(setIsBinary(publicKey), Base2e15Utils.encode(value));
  }

  @Override
  public List<AtKey> getAtKeys(String regex) throws AtException {
    return getAtKeys(regex, true);
  }

  @Override
  public List<AtKey> getAtKeys(String regex, boolean fetchMetadata) throws AtException {
    return KeyCommands.getKeys(executor, regex, fetchMetadata);
  }

  private void handleEvent(AtEventType eventType, Map<String, Object> eventData) {
    try {
      switch (eventType) {
        case sharedKeyNotification:
          onSharedKeyNotification(eventData);
          break;
        case updateNotification:
          onUpdateNotification(eventData);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      log.error("unexpected exception handling {} : {}", eventType, eventData, e);
    }
  }

  private void onSharedKeyNotification(Map<String, Object> eventData) throws AtDecryptionException {
    // We've got notification that someone has shared an encryption key with us
    // If we also got a value, we can decrypt it and add it to our keys map
    // Note: a value isn't supplied when the ttr on the shared key was set to 0
    if (eventData.get("value") != null) {
      AtKeys keys = context.getKeys();
      String keyName = (String) eventData.get("key");
      String value = (String) eventData.get("value");
      String decrypted = rsaDecryptFromBase64(value, keys.getEncryptPrivateKey());
      keys.put(keyName, decrypted);
    }
  }

  private void onUpdateNotification(Map<String, Object> eventData) throws AtException {
    // Let's see if we can decrypt it on the fly
    if (eventData.get("value") != null) {
      AtKeys keys = context.getKeys();
      String encryptedValue = (String) eventData.get("value");
      Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
      String ivNonce = (String) metadata.get("ivNonce");
      String encryptKeySharedByOther;
      String sharedKeyEnc = (String) metadata.get("sharedKeyEnc");
      if (sharedKeyEnc != null) {
        encryptKeySharedByOther = rsaDecryptFromBase64(sharedKeyEnc, keys.getEncryptPrivateKey());
      } else {
        String key = (String) eventData.get("key");
        SharedKey sk = org.atsign.client.api.Keys.sharedKeyBuilder().rawKey(key).build();
        encryptKeySharedByOther = SharedKeyCommands.lookupEncryptKeySharedByOther(executor, context, sk);
      }
      String decryptedValue = aesDecryptFromBase64(encryptedValue, encryptKeySharedByOther, ivNonce);
      HashMap<String, Object> newEventData = new HashMap<>(eventData);
      newEventData.put("decryptedValue", decryptedValue);
      eventBus.publishEvent(decryptedUpdateNotification, newEventData);
    }
  }

  /**
   * A runnable command returning a String; may throw {@link AtException}s or
   * execution exceptions.
   */
  public interface AtCommandThatReturnsString {
    String run() throws AtException, ExecutionException, InterruptedException;
  }

  public static CompletableFuture<String> wrapAsync(AtCommandThatReturnsString command) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return command.run();
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * A runnable command returning a byte array; may throw {@link AtException}s or
   * execution exceptions.
   */
  public interface AtCommandThatReturnsByteArray {
    byte[] run() throws AtException, ExecutionException, InterruptedException;
  }

  public static CompletableFuture<byte[]> wrapAsync(AtCommandThatReturnsByteArray command) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return command.run();
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * A runnable command returning no value; may throw {@link AtException}s or
   * execution exceptions.
   */
  public interface AtCommandThatReturnsVoid {
    void run() throws AtException, ExecutionException, InterruptedException;
  }

  public static CompletableFuture<Void> wrapAsync(AtCommandThatReturnsVoid command) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        command.run();
        return null;
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    });
  }

  private static <T extends AtKey> T setIsBinary(T key) {
    if (!Metadata.isBinary(key.metadata())) {
      key.overwriteMetadata(key.metadata().toBuilder().isBinary(true).build());
    }
    return key;
  }

}
