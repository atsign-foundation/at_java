package org.atsign.client.api;

import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.NoSuchSecondaryException;

import java.io.IOException;

/**
 * Clients ultimately talk to a Secondary server - usually this is a microservice which implements
 * the @ protocol server spec, running somewhere in the cloud.
 *
 * In the initial implementation we just have AtClientImpl talking to a RemoteSecondary which in turn
 * talks, via TLS over a secure socket, to the cloud Secondary server.
 *
 * As we implement client-side offline storage, performance caching etc., we can expect e.g.
 * <br/>AtClient -> FastCacheSecondary -> OfflineStorageSecondary -> RemoteSecondary<br/>
 * where FastCacheSecondary might be an in-memory LRU cache, and OfflineStorageSecondary is a
 * persistent cache of some or all of the information in the RemoteSecondary. To make this
 * possible, each Secondary will need to be able to fully handle the @ protocol, thus the
 * interface is effectively the same as when interacting with a cloud secondary via openssl
 * from command line.
 */
@SuppressWarnings("unused")
public interface Secondary extends AtEvents.AtEventListener {
    /**
     * @param command in @ protocol format
     * @param throwExceptionOnErrorResponse sometimes we want to inspect an error response,
     *        sometimes we want to just throw an exception
     * @return response in @ protocol format
     * @throws AtException if there was an error response and throwExceptionOnErrorResponse
     *         is true, or as a wrapper for other exceptions - typically IOExceptions
     */
    Response executeCommand(String command, boolean throwExceptionOnErrorResponse) throws AtException;

    void startMonitor();
    void stopMonitor();
    boolean isMonitorRunning();

    class Response {
        public String data;
        public boolean isError;
        public String error;

        @Override
        public String toString() {
            if (isError) {
                return "error:" + error;
            } else {
                return "data:" + data;
            }
        }
    }

    class Address {
        public final String host;
        public final int port;

        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public static Address fromString(String hostAndPort) throws IllegalArgumentException {
            String[] split = hostAndPort.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Cannot construct Secondary.Address from malformed host:port string '" + hostAndPort + "'");
            }
            String host = split[0];
            int port;
            try {
                port = Integer.parseInt(split[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot construct Secondary.Address from malformed host:port string '" + hostAndPort + "'");
            }
            return new Address(host, port);
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }

    interface AddressFinder {
        Address findSecondary(AtSign atSign) throws IOException, NoSuchSecondaryException;
    }
}
