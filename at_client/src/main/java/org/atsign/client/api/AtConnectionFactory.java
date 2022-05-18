package org.atsign.client.api;

import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.common.AtSign;

/**
 * For getting a hold of AtConnections to things.
 * We inject an AtConnectionFactory into AtClientImpl, primarily for testability
 */
public interface AtConnectionFactory {
    AtSecondaryConnection getSecondaryConnection(AtEvents.AtEventBus eventBus, AtSign atSign, Secondary.Address secondaryAddress, AtConnection.Authenticator authenticator);
    AtSecondaryConnection getSecondaryConnection(AtEvents.AtEventBus eventBus, AtSign atSign, String secondaryUrl, AtConnection.Authenticator authenticator, boolean verbose);
    AtSecondaryConnection getSecondaryConnection(AtEvents.AtEventBus eventBus, AtSign atSign, Secondary.Address secondaryAddress, AtConnection.Authenticator authenticator, boolean verbose);
    AtRootConnection getRootConnection(AtEvents.AtEventBus eventBus, String rootUrl);
    AtRootConnection getRootConnection(AtEvents.AtEventBus eventBus, String rootUrl, boolean verbose);
}
