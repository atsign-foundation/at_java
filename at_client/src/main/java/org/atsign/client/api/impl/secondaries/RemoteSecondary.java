package org.atsign.client.api.impl.secondaries;

import org.atsign.client.api.AtConnectionFactory;
import static org.atsign.client.api.AtEvents.*;
import org.atsign.client.api.Secondary;
import org.atsign.client.api.impl.connections.AtMonitorConnection;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;
import org.atsign.client.util.AuthUtil;

import java.io.IOException;
import java.util.*;

/**
 * @see org.atsign.client.api.Secondary
 */
public class RemoteSecondary implements Secondary {

    private final AtConnectionFactory connectionFactory;
    private final AtEventBus eventBus;

    @SuppressWarnings("unused")
    public AtConnectionFactory getConnectionFactory() {return connectionFactory;}

    private final AtSecondaryConnection connection;
    @SuppressWarnings("unused")
    public AtSecondaryConnection getConnection() {return connection;}

    private AtMonitorConnection monitorConnection;
    @SuppressWarnings("unused")
    public AtMonitorConnection getMonitorConnection() {return monitorConnection;}

    private final AtSign atSign;
    public AtSign getAtSign() {return atSign;}

    private final String secondaryUrl;
    @SuppressWarnings("unused")
    public String getSecondaryUrl() {return secondaryUrl;}

    private boolean verbose;
    @SuppressWarnings("unused")
    public boolean isVerbose() {return verbose;}
    @SuppressWarnings("unused")
    public void setVerbose(boolean b) {
        verbose = b;
        this.connection.setVerbose(b);
        this.monitorConnection.setVerbose(b);
    }

    public RemoteSecondary(AtEventBus eventBus, AtSign atSign, String secondaryUrl,
                           Map<String, String> keys, AtConnectionFactory connectionFactory) throws IOException, AtException {
        this(eventBus, atSign, secondaryUrl, keys, connectionFactory, false);
    }
    public RemoteSecondary(AtEventBus eventBus, AtSign atSign, String secondaryUrl,
                           Map<String, String> keys, AtConnectionFactory connectionFactory,
                           boolean verbose) throws IOException, AtException {
        this.eventBus = eventBus;
        this.atSign = atSign;
        this.secondaryUrl = secondaryUrl;
        this.connectionFactory = connectionFactory;
        this.verbose = verbose;

        this.connection = connectionFactory.getSecondaryConnection(
                this.eventBus,
                this.atSign,
                this.secondaryUrl,
                connection -> new AuthUtil().authenticateWithPkam(connection, atSign, keys),
                verbose);
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

    @Override public void startMonitor() {ensureMonitorRunning();}
    @Override public void stopMonitor() {ensureMonitorNotRunning();}
    @Override public boolean isMonitorRunning() {return monitorConnection.isRunning();}

    @Override
    public synchronized void handleEvent(AtEventType eventType, Map<String, Object> eventData) {
//        if (eventType == )
    }

    private void ensureMonitorRunning() {
        String what = "";
        try {
            if (monitorConnection == null) {
                what = "construct an AtMonitorConnection";
                monitorConnection = new AtMonitorConnection(eventBus, atSign, secondaryUrl, connection.getAuthenticator(), verbose);
            }
            if (! monitorConnection.isRunning()) {
                what = "call monitorConnection.startMonitor()";
                monitorConnection.startMonitor();
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
                monitorConnection.stopMonitor();
            }
        } catch (Exception e) {
            System.err.println("SEVERE: failed to " + what + " : " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
