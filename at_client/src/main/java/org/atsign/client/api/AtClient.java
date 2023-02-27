package org.atsign.client.api;

import org.atsign.client.api.impl.clients.AtClientImpl;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.DefaultAtConnectionFactory;
import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.client.api.impl.secondaries.RemoteSecondary;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtClientConfigException;
import org.atsign.common.exceptions.AtSecondaryConnectException;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;
import org.atsign.common.options.GetRequestOptions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.atsign.common.Keys.*;

/**
 * The primary interface of the AtSign client library.
 */
@SuppressWarnings("unused")
public interface AtClient extends Secondary, AtEvents.AtEventBus {

    /**
     * Standard AtClient factory - uses production @ root to look up the cloud secondary address for this atSign
     * @param atSign the atsign of this client
     * @return An {@link AtClient}
     * @throws AtException if something goes wrong with looking up or connecting to the remote secondary
     */
    static AtClient withRemoteSecondary(AtSign atSign) throws AtException {
        return withRemoteSecondary("root.atsign.org:64", atSign);
    }
    /**
     * Standard AtClient factory - uses production @ root to look up the cloud secondary address for this atSign
     * @param atSign the atsign of this client
     * @param verbose set to true for chatty logs
     * @return An {@link AtClient}
     * @throws AtException if something goes wrong with looking up or connecting to the remote secondary
     */
    static AtClient withRemoteSecondary(AtSign atSign, boolean verbose) throws AtException {
        return withRemoteSecondary("root.atsign.org:64", atSign, verbose);
    }

    /**
     * Factory to use when you wish to use a custom Secondary.AddressFinder
     * @param atSign the atSign of this client
     * @param secondaryAddressFinder will be used to find the Secondary.Address of the atSign
     * @return An {@link AtClient}
     * @throws AtException if any other exception occurs while connecting to the remote (cloud) secondary
     */
    static AtClient withRemoteSecondary(AtSign atSign, Secondary.AddressFinder secondaryAddressFinder) throws AtException {
        Secondary.Address remoteSecondaryAddress;
        try {
            remoteSecondaryAddress = secondaryAddressFinder.findSecondary(atSign);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to find secondary, with IOException", e);
        }
        return withRemoteSecondary(atSign, remoteSecondaryAddress, false);
    }

    /**
     * Factory - returns default AtClientImpl with a RemoteSecondary and a DefaultConnectionFactory
     * @param rootUrl the address of the root server to use - e.g. root.atsign.org:64 for production at-signs
     * @param atSign the atSign of the client - e.g. @alice
     * @return An {@link AtClient}
     * @throws AtException if anything goes wrong during construction
     */
    static AtClient withRemoteSecondary(String rootUrl, AtSign atSign) throws AtException {
        return withRemoteSecondary(rootUrl, atSign, false);
    }

    static AtClient withRemoteSecondary(String rootUrl, AtSign atSign, boolean verbose) throws AtException {
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

        return withRemoteSecondary(atSign, secondaryAddress, verbose);
    }

    /**
     * Factory to use when you wish to use a custom Secondary.AddressFinder
     * @param atSign the atSign of this client
     * @param verbose set to true for chatty logs
     * @return An {@link AtClient}
     * @throws IOException if thrown by the address finder
     * @throws AtException if any other exception occurs while connecting to the remote (cloud) secondary
     */
    static AtClient withRemoteSecondary(AtSign atSign, Secondary.AddressFinder secondaryAddressFinder, boolean verbose) throws IOException, AtException {
        Secondary.Address remoteSecondaryAddress = secondaryAddressFinder.findSecondary(atSign);
        return withRemoteSecondary(atSign, remoteSecondaryAddress, verbose);
    }

    /**
     * Factory to use when you already know the address of the remote (cloud) secondary
     * @param atSign the atSign of this client
     * @param remoteSecondaryAddress the address of the remote secondary server
     * @param verbose set to true for chatty logs
     * @return An {@link AtClient}
     * @throws AtException if any other exception occurs while connecting to the remote (cloud) secondary
     */
    static AtClient withRemoteSecondary(AtSign atSign, Secondary.Address remoteSecondaryAddress, boolean verbose) throws AtException {
        DefaultAtConnectionFactory connectionFactory = new DefaultAtConnectionFactory();
        AtEvents.AtEventBus eventBus = new SimpleAtEventBus();

        Map<String, String> keys;
        try {
            keys = KeysUtil.loadKeys(atSign);
        } catch (Exception e) {
            throw new AtClientConfigException("Failed to load keys", e);
        }

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
     * @param atSign the atSign of this client
     * @param remoteSecondaryAddress the address of the remote secondary server
     * @return An {@link AtClient}
     * @throws AtException if any other exception occurs while connecting to the remote (cloud) secondary
     */
    static AtClient withRemoteSecondary(Secondary.Address remoteSecondaryAddress, AtSign atSign) throws AtException {
        return withRemoteSecondary(atSign, remoteSecondaryAddress, false);
    }



    AtSign getAtSign();
    Secondary getSecondary();
    Map<String, String> getEncryptionKeys();

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
}
