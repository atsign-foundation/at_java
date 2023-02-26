package org.atsign.client.api.impl.secondaries;

import org.atsign.client.api.AtConnectionFactory;
import org.atsign.client.api.Secondary;
import org.atsign.client.api.impl.connections.AtMonitorConnection;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.util.AuthUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtIllegalArgumentException;
import org.atsign.common.exceptions.AtInvalidAtKeyException;
import org.atsign.common.exceptions.AtInvalidSyntaxException;
import org.atsign.common.exceptions.AtUnknownResponseException;

import java.io.IOException;
import java.util.Map;

import static org.atsign.client.api.AtEvents.AtEventBus;
import static org.atsign.client.api.AtEvents.AtEventType;

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

    private final Secondary.Address secondaryAddress;
    @SuppressWarnings("unused")
    public Secondary.Address getSecondaryAddress() {return secondaryAddress;}
    @SuppressWarnings("unused")
    public String getSecondaryUrl() {return secondaryAddress.toString();}

    private boolean verbose;
    @SuppressWarnings("unused")
    public boolean isVerbose() {return verbose;}
    @SuppressWarnings("unused")
    public void setVerbose(boolean b) {
        verbose = b;
        this.connection.setVerbose(b);
        this.monitorConnection.setVerbose(b);
    }

    @SuppressWarnings("unused")
    public RemoteSecondary(AtEventBus eventBus, AtSign atSign, Secondary.Address secondaryAddress,
                           Map<String, String> keys, AtConnectionFactory connectionFactory) throws IOException, AtException {
        this(eventBus, atSign, secondaryAddress, keys, connectionFactory, false);
    }
    public RemoteSecondary(AtEventBus eventBus, AtSign atSign, Secondary.Address secondaryAddress,
                           Map<String, String> keys, AtConnectionFactory connectionFactory,
                           boolean verbose) throws IOException, AtException {
        this.eventBus = eventBus;
        this.atSign = atSign;
        this.secondaryAddress = secondaryAddress;
        this.connectionFactory = connectionFactory;
        this.verbose = verbose;

        this.connection = connectionFactory.getSecondaryConnection(
                this.eventBus,
                this.atSign,
                this.secondaryAddress,
                connection -> new AuthUtil().authenticateWithPkam(connection, atSign, keys),
                verbose);
        connection.connect();
    }

    @Override
    public Response executeCommand(String command, boolean throwExceptionOnErrorResponse) throws IOException, AtException {
        Response response = new Response();
        String rawResponse = connection.executeCommand(command);

        if (rawResponse.startsWith("data:")) {
            response.setRawDataResponse(rawResponse.substring("data:".length()));
        } else if (rawResponse.startsWith("error:")) {
            response.setRawErrorResponse(rawResponse.substring("error:".length()));
            AtException theServerException = response.getException();

            if (theServerException instanceof AtInvalidSyntaxException
                    || theServerException instanceof AtIllegalArgumentException
                    || theServerException instanceof AtInvalidAtKeyException) {
                // Secondaries used to close connections for these exceptions so let's disconnect and reconnect
                connection.disconnect();
                connection.connect();
            }

            if (throwExceptionOnErrorResponse) {
                throw theServerException;
            }
        } else {
            throw new AtUnknownResponseException("Unknown response " + rawResponse + " from command " + command);
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
                monitorConnection = new AtMonitorConnection(eventBus, atSign, secondaryAddress.toString(), connection.getAuthenticator(), verbose);
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
