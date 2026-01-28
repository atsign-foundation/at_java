package org.atsign.client.api;

import static org.atsign.common.Keys.AtKey;
import static org.atsign.common.Keys.PublicKey;
import static org.atsign.common.Keys.SelfKey;
import static org.atsign.common.Keys.SharedKey;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.atsign.client.api.impl.clients.AtClientImpl;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.DefaultAtConnectionFactory;
import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.client.api.impl.secondaries.RemoteSecondary;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtSecondaryConnectException;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;
import org.atsign.common.options.GetRequestOptions;

/**
 * The primary interface of the AtSign client library.
 */
@SuppressWarnings("unused")
public interface AtClient extends Secondary, AtEvents.AtEventBus, Closeable {

  /**
   * Standard AtClient factory - uses production @ root to look up the cloud secondary address for
   * this atSign
   *
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @return An {@link AtClient}
   * @throws AtException if something goes wrong with looking up or connecting to the remote secondary
   */
  static AtClient withRemoteSecondary(AtSign atSign, AtKeys keys) throws AtException {
    return withRemoteSecondary("root.atsign.org:64", atSign, keys);
  }

  /**
   * Standard AtClient factory - uses production @ root to look up the cloud secondary address for
   * this atSign
   *
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @param verbose set to true for chatty logs
   * @return An {@link AtClient}
   * @throws AtException if something goes wrong with looking up or connecting to the remote secondary
   */
  static AtClient withRemoteSecondary(AtSign atSign, AtKeys keys, boolean verbose) throws AtException {
    return withRemoteSecondary("root.atsign.org:64", atSign, keys, verbose);
  }

  /**
   * Factory to use when you wish to use a custom Secondary.AddressFinder
   *
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @param secondaryAddressFinder will be used to find the Secondary.Address of the atSign
   * @return An {@link AtClient}
   * @throws AtException if any other exception occurs while connecting to the remote (cloud)
   *         secondary
   */
  static AtClient withRemoteSecondary(AtSign atSign, AtKeys keys, Secondary.AddressFinder secondaryAddressFinder)
      throws AtException {
    Secondary.Address remoteSecondaryAddress;
    try {
      remoteSecondaryAddress = secondaryAddressFinder.findSecondary(atSign);
    } catch (IOException e) {
      throw new AtSecondaryConnectException("Failed to find secondary, with IOException", e);
    }
    return withRemoteSecondary(atSign, keys, remoteSecondaryAddress, false);
  }

  /**
   * Factory - returns default AtClientImpl with a RemoteSecondary and a DefaultConnectionFactory
   *
   * @param rootUrl the address of the root server to use - e.g. root.atsign.org:64 for production
   *        at-signs
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @return An {@link AtClient}
   * @throws AtException if anything goes wrong during construction
   */
  static AtClient withRemoteSecondary(String rootUrl, AtSign atSign, AtKeys keys) throws AtException {
    return withRemoteSecondary(rootUrl, atSign, keys, false);
  }

  /**
   * Factory - returns default AtClientImpl with a RemoteSecondary and a DefaultConnectionFactory
   *
   * @param rootUrl the address of the root server to use - e.g. root.atsign.org:64 for production
   *        at-signs
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @param verbose set to true for chatty logs
   * @return An {@link AtClient}
   * @throws AtException if anything goes wrong during construction
   */
  static AtClient withRemoteSecondary(String rootUrl, AtSign atSign, AtKeys keys, boolean verbose) throws AtException {
    DefaultAtConnectionFactory connectionFactory = new DefaultAtConnectionFactory();

    Secondary.Address secondaryAddress;
    try {
      AtRootConnection rootConnection = connectionFactory.getRootConnection(new SimpleAtEventBus(), rootUrl, verbose);
      rootConnection.connect();
      secondaryAddress = rootConnection.findSecondary(atSign);
    } catch (AtSecondaryNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new AtSecondaryNotFoundException("Failed to lookup remote secondary", e);
    }

    return withRemoteSecondary(atSign, keys, secondaryAddress, verbose);
  }

  /**
   * Factory to use when you wish to use a custom Secondary.AddressFinder
   *
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @param verbose set to true for chatty logs
   * @return An {@link AtClient}
   * @throws IOException if thrown by the address finder
   * @throws AtException if any other exception occurs while connecting to the remote (cloud)
   *         secondary
   */
  static AtClient withRemoteSecondary(AtSign atSign, AtKeys keys, Secondary.AddressFinder secondaryAddressFinder,
                                      boolean verbose)
      throws IOException, AtException {
    Secondary.Address remoteSecondaryAddress = secondaryAddressFinder.findSecondary(atSign);
    return withRemoteSecondary(atSign, keys, remoteSecondaryAddress, verbose);
  }

  /**
   * Factory to use when you already know the address of the remote (cloud) secondary
   *
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @param remoteSecondaryAddress the address of the remote secondary server
   * @param verbose set to true for chatty logs
   * @return An {@link AtClient}
   * @throws AtException if any other exception occurs while connecting to the remote (cloud)
   *         secondary
   */
  static AtClient withRemoteSecondary(AtSign atSign, AtKeys keys, Secondary.Address remoteSecondaryAddress,
                                      boolean verbose)
      throws AtException {
    DefaultAtConnectionFactory connectionFactory = new DefaultAtConnectionFactory();
    AtEvents.AtEventBus eventBus = new SimpleAtEventBus();

    RemoteSecondary secondary;
    try {
      secondary = new RemoteSecondary(eventBus, atSign, remoteSecondaryAddress, keys, connectionFactory, verbose);
    } catch (IOException e) {
      throw new AtSecondaryConnectException("Failed to create RemoteSecondary", e);
    }

    return new AtClientImpl(eventBus, atSign, keys, secondary);
  }

  /**
   * Factory to use when you already know the address of the remote (cloud) secondary
   *
   * @param remoteSecondaryAddress the address of the remote secondary server
   * @param atSign the {@link AtSign} of this client - e.g. @alice
   * @param keys the {@link AtKeys} for this client
   * @return An {@link AtClient}
   * @throws AtException if any other exception occurs while connecting to the remote (cloud)
   *         secondary
   */
  static AtClient withRemoteSecondary(Secondary.Address remoteSecondaryAddress, AtSign atSign, AtKeys keys)
      throws AtException {
    return withRemoteSecondary(atSign, keys, remoteSecondaryAddress, false);
  }



  AtSign getAtSign();

  Secondary getSecondary();

  AtKeys getEncryptionKeys();

  CompletableFuture<String> get(SharedKey sharedKey);

  CompletableFuture<byte[]> getBinary(SharedKey sharedKey);

  CompletableFuture<String> put(SharedKey sharedKey, String value);

  CompletableFuture<String> delete(SharedKey sharedKey);

  CompletableFuture<String> get(SelfKey selfKey);

  CompletableFuture<byte[]> getBinary(SelfKey selfKey);

  CompletableFuture<String> put(SelfKey selfKey, String value);

  CompletableFuture<String> delete(SelfKey selfKey);

  CompletableFuture<String> get(PublicKey publicKey);

  CompletableFuture<String> get(PublicKey publicKey, GetRequestOptions getRequestOptions);

  CompletableFuture<byte[]> getBinary(PublicKey publicKey);

  CompletableFuture<byte[]> getBinary(PublicKey publicKey, GetRequestOptions getRequestOptions);

  CompletableFuture<String> put(PublicKey publicKey, String value);

  CompletableFuture<String> delete(PublicKey publicKey);

  CompletableFuture<String> put(SharedKey sharedKey, byte[] value);

  CompletableFuture<String> put(SelfKey selfKey, byte[] value);

  CompletableFuture<String> put(PublicKey publicKey, byte[] value);

  CompletableFuture<List<AtKey>> getAtKeys(String regex);

  CompletableFuture<List<AtKey>> getAtKeys(String regex, boolean fetchMetadata);
}
