package org.atsign.client.api;

import org.atsign.common.AtException;

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
}
