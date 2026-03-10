package org.atsign.client.api;

import static org.atsign.common.Keys.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.AtSign;
import org.atsign.common.options.GetRequestOptions;

/**
 * The primary interface of the AtSign client library.
 */
@SuppressWarnings("unused")
public interface AtClient extends AtEvents.AtEventBus, AutoCloseable {

  AtSign getAtSign();

  AtKeys getEncryptionKeys();

  CompletableFuture<String> get(SharedKey sharedKey);

  CompletableFuture<byte[]> getBinary(SharedKey sharedKey);

  CompletableFuture<Void> put(SharedKey sharedKey, String value);

  CompletableFuture<Void> delete(SharedKey sharedKey);

  CompletableFuture<String> get(SelfKey selfKey);

  CompletableFuture<byte[]> getBinary(SelfKey selfKey);

  CompletableFuture<Void> put(SelfKey selfKey, String value);

  CompletableFuture<Void> delete(SelfKey selfKey);

  CompletableFuture<String> get(PublicKey publicKey);

  CompletableFuture<String> get(PublicKey publicKey, GetRequestOptions getRequestOptions);

  CompletableFuture<byte[]> getBinary(PublicKey publicKey);

  CompletableFuture<byte[]> getBinary(PublicKey publicKey, GetRequestOptions getRequestOptions);

  CompletableFuture<Void> put(PublicKey publicKey, String value);

  CompletableFuture<Void> delete(PublicKey publicKey);

  CompletableFuture<Void> put(SharedKey sharedKey, byte[] value);

  CompletableFuture<Void> put(SelfKey selfKey, byte[] value);

  CompletableFuture<Void> put(PublicKey publicKey, byte[] value);

  CompletableFuture<List<AtKey>> getAtKeys(String regex);

  CompletableFuture<List<AtKey>> getAtKeys(String regex, boolean fetchMetadata);

  void startMonitor();

  void stopMonitor();

  boolean isMonitorRunning();

  AtClientConnection getCommandExecutor();
}
