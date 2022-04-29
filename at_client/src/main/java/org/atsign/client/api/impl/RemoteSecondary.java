package org.atsign.client.api.impl;

import org.atsign.client.api.AtConnectionFactory;
import org.atsign.client.api.Secondary;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;
import org.atsign.client.util.AuthUtil;

import java.io.IOException;
import java.util.*;

/**
 * @see org.atsign.client.api.Secondary
 */
@SuppressWarnings("unused")
public class RemoteSecondary implements Secondary, Secondary.EventListener {

    private final AtConnectionFactory connectionFactory;
    public AtConnectionFactory getConnectionFactory() {return connectionFactory;}

    private final AtSecondaryConnection connection;
    public AtSecondaryConnection getConnection() {return connection;}

    private AtMonitorConnection monitorConnection;
    public AtMonitorConnection getMonitorConnection() {return monitorConnection;}

    private final AtSign atSign;
    public AtSign getAtSign() {return atSign;}

    @SuppressWarnings("FieldCanBeLocal")
    private final Map<String, String> keys;

    private final String secondaryUrl;
    public String getSecondaryUrl() {return secondaryUrl;}

    private final Map<EventListener, Set<EventType>> eventListeners = new HashMap<>();

    public RemoteSecondary(AtSign atSign, String secondaryUrl, Map<String, String> keys) throws AtException, IOException {
        this(atSign, secondaryUrl, keys, new DefaultAtConnectionFactory());
    }

    public RemoteSecondary(AtSign atSign, String secondaryUrl,
                           Map<String, String> keys, AtConnectionFactory connectionFactory) throws IOException, AtException {
        this.atSign = atSign;
        this.secondaryUrl = secondaryUrl;
        this.connectionFactory = connectionFactory;
        this.keys = keys;

        this.connection = connectionFactory.getSecondaryConnection(
                this.atSign,
                secondaryUrl,
                connection -> new AuthUtil().authenticateWithPkam(connection, atSign, keys));
        connection.connect();
    }

    @Override
    public Response executeCommand(String command, boolean throwExceptionOnErrorResponse) throws AtException {
        Response response = new Response();
        String rawResponse;
        try {
            rawResponse = connection.executeCommand(command);
        } catch (IOException e) {
            throw new AtException("Failed to execute command " + command + " : " + e.getMessage(), e);
        }

        if (rawResponse.startsWith("data:")) {
            response.data = rawResponse.substring("data:".length());
            response.isError = false;
            response.error = null;
        } else if (rawResponse.startsWith("error:")) {
            if (rawResponse.contains("AT0003-Invalid syntax")) {
                // Secondaries close connections rudely after invalid syntax
                connection.disconnect();
                try {
                    connection.connect();
                } catch (IOException e) {
                    throw new AtException("Failed to reconnect after syntax error");
                }
            }

            if (throwExceptionOnErrorResponse) {
                throw new AtException(rawResponse);
            } else {
                response.data = null;
                response.isError = true;
                response.error = rawResponse.substring("error:".length());
            }
        } else {
            throw new AtException("Invalid response " + rawResponse + " from command " + command);
        }
        return response;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public synchronized void handleEvent(EventType eventType, String eventData) {
        Set<Map.Entry<EventListener, Set<EventType>>> listenerEntries = eventListeners.entrySet();
        for (Map.Entry<EventListener, Set<EventType>> next : listenerEntries) {
            try {
                Set<EventType> acceptedEventTypes = next.getValue();
                if (acceptedEventTypes.contains(eventType)) {
                    EventListener listener = next.getKey();
                    listener.handleEvent(eventType, eventData);
                }
            } catch (Exception e) {
                System.err.println(this.getClass().getSimpleName() + " caught exception from one of its event listeners : " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    @Override
    public synchronized void addEventListener(EventListener listener, Set<EventType> eventTypes) {
        ensureMonitorRunning();
        eventListeners.put(listener, eventTypes);
    }

    @Override
    public synchronized void  removeEventListener(EventListener listener) {
        eventListeners.remove(listener);
        if (eventListeners.isEmpty()) {
            ensureMonitorNotRunning();
        }
    }

    private void ensureMonitorRunning() {
        String what = "";
        try {
            if (monitorConnection == null) {
                what = "construct an AtMonitorConnection";
                monitorConnection = new AtMonitorConnection(this, atSign, secondaryUrl, connection.authenticator, true);
            }
            if (! monitorConnection.isConnected()) {
                what = "call monitorConnection.connect()";
                monitorConnection.connect();
            }
            if (! monitorConnection.isRunning()) {
                what = "call monitorConnection.startMonitor()";
                monitorConnection.startMonitor("addEventListener->ensureMonitorRunning");
            }
        } catch (Exception e) {
            System.err.println("SEVERE: failed to " + what + " : " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void ensureMonitorNotRunning() {
        String what = "";
        try {
            if (monitorConnection == null) {
                return;
            }
            if (monitorConnection.isRunning()) {
                what = "call monitorConnection.stopMonitor()";
                monitorConnection.stopMonitor("removeEventListener->ensureMonitorNotRunning");
            }
            if (monitorConnection.isConnected()) {
                what = "call monitorConnection.disconnect()";
                monitorConnection.disconnect();
            }
        } catch (Exception e) {
            System.err.println("SEVERE: failed to " + what + " : " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
