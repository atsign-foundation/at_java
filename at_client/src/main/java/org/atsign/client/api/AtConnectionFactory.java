package org.atsign.client.api;

import org.atsign.client.api.impl.AtRootConnection;
import org.atsign.client.api.impl.AtSecondaryConnection;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;

import java.io.IOException;

/**
 * For getting a hold of AtConnections to things.
 * We inject an AtConnectionFactory into AtClientImpl, primarily for testability
 */
@SuppressWarnings("unused")
public interface AtConnectionFactory {
    AtSecondaryConnection getSecondaryConnection(AtSign atSign, String secondaryUrl, AtConnection.Authenticator authenticator) throws IOException, AtException;
    AtRootConnection getRootConnection(String rootUrl) throws IOException, AtException;
}
