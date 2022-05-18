package org.atsign.client.api.impl.connections;

import org.atsign.client.api.AtConnection;
import org.atsign.client.api.AtConnectionFactory;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.Secondary;
import org.atsign.common.AtSign;

/**
 * @see org.atsign.client.api.AtConnectionFactory
 */
public class DefaultAtConnectionFactory implements AtConnectionFactory {
    @Override
    public AtSecondaryConnection getSecondaryConnection(AtEvents.AtEventBus eventBus, AtSign atSign, Secondary.Address secondaryAddress, AtConnection.Authenticator authenticator) {
        return new AtSecondaryConnection(eventBus, atSign, secondaryAddress, authenticator, true, false);
    }

    @Override
    public AtSecondaryConnection getSecondaryConnection(AtEvents.AtEventBus eventBus, AtSign atSign, Secondary.Address secondaryAddress, AtConnection.Authenticator authenticator, boolean verbose) {
        return new AtSecondaryConnection(eventBus, atSign, secondaryAddress, authenticator, true, verbose);
    }

    @Override
    public AtSecondaryConnection getSecondaryConnection(AtEvents.AtEventBus eventBus, AtSign atSign, String secondaryUrl, AtConnection.Authenticator authenticator, boolean verbose) {
        return new AtSecondaryConnection(eventBus, atSign, secondaryUrl, authenticator, true, verbose);
    }

    @Override
    public AtRootConnection getRootConnection(AtEvents.AtEventBus eventBus, String rootUrl) {
        return new AtRootConnection(eventBus, rootUrl, true, false);
    }

    @Override
    public AtRootConnection getRootConnection(AtEvents.AtEventBus eventBus, String rootUrl, boolean verbose) {
        return new AtRootConnection(eventBus, rootUrl, true, verbose);
    }
}
