package org.atsign.client.api.impl.connections;

import org.atsign.client.api.AtEvents;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;

import java.io.IOException;

/**
 * A connection which understands how to talk with the root server.
 * @see org.atsign.client.api.AtConnection
 */
public class AtRootConnection extends AtConnectionBase {

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
     * @param atSign
     * @return A String in the format host:port
     * @throws IOException if connection to root server is unavailable or encounters an error
     * @throws AtException if the root server returns the string 'null' as the lookup response,
     * which means that the atsign is not known to the root server
     */
    public String lookupAtSign(AtSign atSign) throws IOException, AtException {
        if (!isConnected()) {
            connect();
        }
        String response = executeCommand(atSign.withoutPrefix());

        if ("null".equals(response)) {
            throw new AtException("Root lookup returned null for @" + atSign);
        } else {
            return response;
        }
    }
}
