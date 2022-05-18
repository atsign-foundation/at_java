package org.atsign.client.api;

import org.atsign.common.AtException;

import java.io.IOException;
import java.net.Socket;

/**
 * A simple abstraction around connections to @ platform services - e.g. the root server and secondary servers
 */
@SuppressWarnings("unused")
public interface AtConnection {
    String getUrl();

    String getHost();

    int getPort();

    Socket getSocket();

    boolean isConnected();

    boolean isAutoReconnect();

    boolean isVerbose();

    void setVerbose(boolean verbose);

    void connect() throws IOException, AtException;
    void disconnect();

    String executeCommand(String command) throws IOException;

    interface Authenticator {
        void authenticate(AtConnection connection) throws AtException, IOException;
    }

}
