package org.atsign.client.api.impl.connections;

import org.atsign.client.api.AtEvents;
import org.atsign.client.api.Secondary;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;

import java.io.IOException;

/**
 * A connection which understands how to talk with the root server.
 * @see org.atsign.client.api.AtConnection
 */
public class AtRootConnection extends AtConnectionBase implements Secondary.AddressFinder {

    public AtRootConnection(String rootUrl) {
        this (null, rootUrl);
    }
    public AtRootConnection(AtEvents.AtEventBus eventBus, String rootUrl) {
        this(eventBus, rootUrl, true, false);
    }
    public AtRootConnection(AtEvents.AtEventBus eventBus, String rootUrl, boolean autoReconnect, boolean verbose) {
        super(eventBus, rootUrl, null, autoReconnect, verbose);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected String parseRawResponse(String rawResponse) throws IOException {
        // responses from root are either 'null' or <host:port>
        if (rawResponse.startsWith("@")) {
            rawResponse = rawResponse.substring(1);
        }
        return rawResponse;
    }

    /**
     * Looks up the address of the secondary for a given atsign
     * @param atSign the AtSign being looked up
     * @return A {@link Secondary.Address}
     * @throws IOException if connection to root server is unavailable, encounters an error, or response is malformed
     * @throws AtSecondaryNotFoundException if the root server returns the string 'null' as the lookup response,
     * which means that the atsign is not known to the root server
     */
    @Override
    public Secondary.Address findSecondary(AtSign atSign) throws IOException, AtSecondaryNotFoundException {
        if (!isConnected()) {
            try {
                connect();
            } catch (AtException e) {
                // connect will only throw an AtException if authentication fails. Root connections do not require authentication.
                throw new IOException(e);
            }
        }
        String response = executeCommand(atSign.withoutPrefix());

        if ("null".equals(response)) {
            throw new AtSecondaryNotFoundException("Root lookup returned null for " + atSign);
        } else {
            try {
                return Secondary.Address.fromString(response);
            } catch (IllegalArgumentException e) {
                throw new IOException("Received malformed response " + response + " from lookup of " + atSign + " on root server");
            }
        }
    }

    /**
     * Wrapper for {@link #findSecondary(AtSign)}
     */
    public String lookupAtSign(AtSign atSign) throws IOException, AtSecondaryNotFoundException {
        return this.findSecondary(atSign).toString();
    }
}
