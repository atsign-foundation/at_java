package org.atsign.client.api;

import org.atsign.client.api.impl.clients.AtClientImpl;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.DefaultAtConnectionFactory;
import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.client.api.impl.secondaries.RemoteSecondary;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;
import org.atsign.client.util.KeysUtil;

import static org.atsign.common.Keys.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The primary interface of the AtSign client library.
 */
@SuppressWarnings("unused")
public interface AtClient extends Secondary, AtEvents.AtEventBus {
    /**
     * Factory - returns default AtClientImpl with a RemoteSecondary and a DefaultConnectionFactory
     * @param rootUrl the address of the root server to use - e.g. root.atsign.org:64 for prod, or
     *                root.atsign.wtf:64 for staging, or vip.ve.atsign.zone:64 for local host
     * @param atSign the atSign of the client - e.g. @alice
     * @return an AtClient
     * @throws AtException if anything goes wrong during construction
     */
    static AtClient withRemoteSecondary(String rootUrl, AtSign atSign) throws AtException {
        DefaultAtConnectionFactory connectionFactory = new DefaultAtConnectionFactory();
        AtEvents.AtEventBus eventBus = new SimpleAtEventBus();

        String secondaryUrl;
        try {
            // secondaryUrl = new AtRootConnection(rootUrl).lookupAtSign(atSign);
            AtRootConnection rootConnection = connectionFactory.getRootConnection(eventBus, rootUrl);
            rootConnection.connect();
            secondaryUrl = rootConnection.lookupAtSign(atSign);
        } catch (Exception e) {
            throw new AtException("Failed to lookup remote secondary: " + e.getMessage(), e);
        }

        Map<String, String> keys;
        try {
            keys = KeysUtil.loadKeys(atSign);
        } catch (Exception e) {
            throw new AtException("Failed to load keys : " + e.getMessage(), e);
        }

        RemoteSecondary secondary;
        try {
            secondary = new RemoteSecondary(eventBus, atSign, secondaryUrl, keys, connectionFactory);
        } catch (Exception e) {
            throw new AtException("Failed to create RemoteSecondary: " + e.getMessage(), e);
        }

        return new AtClientImpl(eventBus, atSign, keys, secondary);
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
    CompletableFuture<byte[]> getBinary(PublicKey publicKey);
    CompletableFuture<String> put(PublicKey publicKey, String value);
    CompletableFuture<String> delete(PublicKey publicKey);

    CompletableFuture<String> put(SharedKey sharedKey, byte[] value);
    CompletableFuture<String> put(SelfKey selfKey, byte[] value);
    CompletableFuture<String> put(PublicKey publicKey, byte[] value);

    CompletableFuture<List<AtKey>> getAtKeys(String regex);
}
