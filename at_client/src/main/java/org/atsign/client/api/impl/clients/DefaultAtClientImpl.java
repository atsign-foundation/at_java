package org.atsign.client.api.impl.clients;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;
import static org.atsign.client.util.EncryptionUtil.aesDecryptFromBase64;
import static org.atsign.client.util.EncryptionUtil.rsaDecryptFromBase64;
import static org.atsign.client.util.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents.AtEventBus;
import org.atsign.client.api.AtEvents.AtEventListener;
import org.atsign.client.api.AtEvents.AtEventType;
import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.connection.protocol.*;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.exceptions.AtDecryptionException;
import org.atsign.common.options.GetRequestOptions;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of an {@link AtClient} which wraps a
 * {@link org.atsign.client.connection.api.AtClientConnection}
 * in order to implement the "map like" features of the {@link AtClient} interface
 */
@SuppressWarnings({"RedundantThrows", "unused"})
@Slf4j
public class DefaultAtClientImpl implements AtClient {

  private final AtSign atSign;
  private final AtKeys keys;
  private final AtClientConnection connection;
  private final AtEventBus eventBus;
  private final AtomicBoolean isMonitoring = new AtomicBoolean();
  private final Notifications.EventBusBridge eventBusBridge;

  @Override
  public AtSign getAtSign() {
    return atSign;
  }

  @Override
  public AtKeys getEncryptionKeys() {
    return keys;
  }

  @Override
  public AtClientConnection getCommandExecutor() {
    return connection;
  }

  @Builder
  public DefaultAtClientImpl(AtSign atSign, AtKeys keys, AtClientConnection connection, AtEventBus eventBus) {
    checkNotNull(keys.getEncryptPrivateKey(), "AtKeys have not been fully enrolled");
    this.atSign = atSign;
    this.keys = keys;
    this.connection = connection;
    this.eventBus = eventBus;
    this.eventBus.addEventListener(this::handleEvent, EnumSet.allOf(AtEventType.class));
    this.eventBusBridge = new Notifications.EventBusBridge(eventBus, atSign);
  }

  @Override
  public void close() throws IOException {
    try {
      connection.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void startMonitor() {
    isMonitoring.compareAndSet(false, true);
    connection.onReady(Notifications.monitor(atSign, keys, eventBusBridge::accept));
  }

  @Override
  public void stopMonitor() {
    isMonitoring.compareAndSet(true, false);
    connection.onReady(Authentication.pkamAuthenticator(atSign, keys));
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
  public CompletableFuture<String> get(SharedKey sharedKey) {
    return wrapAsync(() -> SharedKeys.get(connection, atSign, keys, sharedKey));
  }

  @Override
  public CompletableFuture<byte[]> getBinary(SharedKey sharedKey) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<Void> put(SharedKey sharedKey, String value) {
    return wrapAsync(() -> SharedKeys.put(connection, atSign, keys, sharedKey, value));
  }

  @Override
  public CompletableFuture<Void> delete(SharedKey sharedKey) {
    return wrapAsync(() -> Keys.deleteKey(connection, sharedKey));
  }

  @Override
  public CompletableFuture<String> get(SelfKey selfKey) {
    return wrapAsync(() -> SelfKeys.get(connection, keys, selfKey));
  }

  @Override
  public CompletableFuture<byte[]> getBinary(SelfKey selfKey) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<Void> put(SelfKey selfKey, String value) {
    return wrapAsync(() -> SelfKeys.put(connection, keys, selfKey, value));
  }

  @Override
  public CompletableFuture<Void> delete(SelfKey selfKey) {
    return wrapAsync(() -> Keys.deleteKey(connection, selfKey));
  }

  @Override
  public CompletableFuture<String> get(PublicKey publicKey) {
    return wrapAsync(() -> PublicKeys.get(connection, atSign, publicKey, null));
  }

  @Override
  public CompletableFuture<String> get(PublicKey publicKey, GetRequestOptions options) {
    return wrapAsync(() -> PublicKeys.get(connection, atSign, publicKey, options));
  }

  @Override
  public CompletableFuture<byte[]> getBinary(PublicKey publicKey) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<byte[]> getBinary(PublicKey publicKey, GetRequestOptions getRequestOptions) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<Void> put(PublicKey publicKey, String value) {
    return wrapAsync(() -> PublicKeys.put(connection, keys, publicKey, value));
  }

  @Override
  public CompletableFuture<Void> delete(PublicKey publicKey) {
    return wrapAsync(() -> Keys.deleteKey(connection, publicKey));
  }

  @Override
  public CompletableFuture<Void> put(SharedKey sharedKey, byte[] value) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<Void> put(SelfKey selfKey, byte[] value) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<Void> put(PublicKey publicKey, byte[] value) {
    throw new UnsupportedOperationException("to be implemented");
  }

  @Override
  public CompletableFuture<List<AtKey>> getAtKeys(String regex) {
    return getAtKeys(regex, true);
  }

  @Override
  public CompletableFuture<List<AtKey>> getAtKeys(String regex, boolean fetchMetadata) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return Keys.getKeys(connection, regex, fetchMetadata);
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    });
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
      String keyName = (String) eventData.get("key");
      String value = (String) eventData.get("value");
      String decrypted = rsaDecryptFromBase64(value, keys.getEncryptPrivateKey());
      keys.put(keyName, decrypted);
    }
  }

  private void onUpdateNotification(Map<String, Object> eventData) throws AtException {
    // Let's see if we can decrypt it on the fly
    if (eventData.get("value") != null) {
      String key = (String) eventData.get("key");
      String encryptedValue = (String) eventData.get("value");
      Map<String, Object> metadata = (Map<String, Object>) eventData.get("metadata");
      String ivNonce = (String) metadata.get("ivNonce");
      SharedKey sk = org.atsign.common.Keys.sharedKeyBuilder().rawKey(key).build();
      String encryptKeySharedByOther = SharedKeys.getEncryptKeySharedByOther(connection, keys, sk);
      String decryptedValue = aesDecryptFromBase64(encryptedValue, encryptKeySharedByOther, ivNonce);
      HashMap<String, Object> newEventData = new HashMap<>(eventData);
      newEventData.put("decryptedValue", decryptedValue);
      eventBus.publishEvent(decryptedUpdateNotification, newEventData);
    }
  }

  /**
   * A runnable command which returns a value but can throw {@link AtException}s or execution
   * exceptions
   */
  public interface AtCommandThatReturnsString {
    String run() throws AtException, ExecutionException, InterruptedException;
  }

  private static CompletableFuture<String> wrapAsync(AtCommandThatReturnsString command) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return command.run();
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * A runnable command which does NOT return a value but can throw {@link AtException}s or execution
   * exceptions
   */
  public interface AtCommandThatReturnsVoid {
    void run() throws AtException, ExecutionException, InterruptedException;
  }

  private static CompletableFuture<Void> wrapAsync(AtCommandThatReturnsVoid command) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        command.run();
        return null;
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    });
  }

}
