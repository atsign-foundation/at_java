package org.atsign.client.api.impl;

import org.atsign.common.AtSign;
import org.atsign.common.AtException;

import java.io.IOException;

/**
 * A connection which understands how to talk with the root server.
 * @see org.atsign.client.api.AtConnection
 */
public class AtRootConnection extends AtConnectionBase {

    public AtRootConnection(String rootUrl) {
        this(rootUrl, true, false);
    }
    public AtRootConnection(String rootUrl, boolean autoReconnect, boolean logging) {
        super(rootUrl, null, autoReconnect, logging);
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
