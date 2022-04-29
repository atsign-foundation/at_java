package org.atsign.client.api.impl;

import org.atsign.client.api.AtConnection;
import org.atsign.client.api.AtConnectionFactory;
import org.atsign.common.AtSign;

/**
 * @see org.atsign.client.api.AtConnectionFactory
 */
public class DefaultAtConnectionFactory implements AtConnectionFactory {
    @Override
    public AtSecondaryConnection getSecondaryConnection(AtSign atSign, String secondaryUrl, AtConnection.Authenticator authenticator) {
        return new AtSecondaryConnection(atSign, secondaryUrl, authenticator, true, false);
    }

    @Override
    public AtRootConnection getRootConnection(String rootUrl) {
        return new AtRootConnection(rootUrl, true, false);
    }
}
