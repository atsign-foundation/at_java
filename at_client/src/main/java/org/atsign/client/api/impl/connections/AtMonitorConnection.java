package org.atsign.client.api.impl.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.common.AtSign;

import java.util.HashMap;

import static org.atsign.client.api.AtEvents.*;
import static org.atsign.client.api.AtEvents.AtEventType.*;

/**
 *
 */
public class AtMonitorConnection extends AtSecondaryConnection implements Runnable {
    private static final ObjectMapper mapper = new ObjectMapper();

    private long _lastReceivedTime = 0;
    public long getLastReceivedTime() {return _lastReceivedTime;}
    public void setLastReceivedTime(long lastReceivedTime) {this._lastReceivedTime = lastReceivedTime;}

    private boolean running = false;
    public boolean isRunning() { return running; }

    private boolean _shouldBeRunning = false;
    private void setShouldBeRunning(boolean b) {
        if (_shouldBeRunning != b) {
            System.err.println("Monitor setting shouldBeRunning to [" + b + "]");
        }
        _shouldBeRunning = b;
    }

    public boolean isShouldBeRunning() {
        return _shouldBeRunning;
    }

    public AtMonitorConnection(
            AtEventBus eventBus,
            AtSign atSign,
            String secondaryUrl,
            Authenticator authenticator,
            boolean verbose) {
        // Note that the Monitor doesn't make use of the auto-reconnect functionality, it does its own thing
        super(eventBus, atSign, secondaryUrl, authenticator, false, verbose);
        startHeartbeat();
    }

    private long lastHeartbeatSentTime = System.currentTimeMillis();
    private long lastHeartbeatAckTime = System.currentTimeMillis();
    private final int heartbeatIntervalMillis = 30000;
    private void startHeartbeat() {
        new Thread(() -> {
            while (true) {
                if (isShouldBeRunning()) {
                    if (!isRunning() || lastHeartbeatSentTime - lastHeartbeatAckTime >= heartbeatIntervalMillis) {
                        try {
                            // heartbeats have stopped being acked
                            System.err.println("Monitor heartbeats not being received");
                            stopMonitor();
                            long waitStartTime = System.currentTimeMillis();
                            while (isRunning() && System.currentTimeMillis() - waitStartTime < 5000) {
                                // wait for monitor to stop
                                try {
                                    //noinspection BusyWait
                                    Thread.sleep(1000);
                                } catch (Exception ignore) {}
                            }
                            if (isRunning()) {
                                System.err.println("Monitor thread has not stopped, but going to start another one anyway");
                            }
                            startMonitor();
                        } catch (Exception e) {
                            System.err.println("Monitor restart failed " + e);
                            e.printStackTrace(System.err);
                        }
                    } else {
                        if (System.currentTimeMillis() - lastHeartbeatSentTime > heartbeatIntervalMillis) {
                            try {
                                executeCommand("noop:0", false, false);
                                lastHeartbeatSentTime = System.currentTimeMillis();
                            } catch (Exception ignore) {
                                // Can't do anything, the heartbeat loop will take care of restarting the monitor connection
                            }
                        }
                    }
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(heartbeatIntervalMillis / 5);
                } catch (Exception ignore) {}
            }
        }).start();
    }

    /**
     * @return true if the monitor start request has succeeded, or if the monitor is already running.
     */
    @SuppressWarnings("UnusedReturnValue")
    public synchronized boolean startMonitor() {
        lastHeartbeatSentTime = lastHeartbeatAckTime = System.currentTimeMillis();

        setShouldBeRunning(true);
        if (! running) {
            running = true;
            if (!isConnected()) {
                try {
                    connect();
                } catch (Exception e) {
                    System.err.println("startMonitor failed to connect to secondary : " + e.getMessage());
                    running = false;
                    return false;
                }
            }
            new Thread(this).start();
        }
        return true;
    }

    public synchronized void stopMonitor() {
        setShouldBeRunning(false);
        lastHeartbeatSentTime = lastHeartbeatAckTime = System.currentTimeMillis();
        disconnect();
    }

    /**
     * Please don't call this directly. Call startMonitor() instead, which starts the monitor in its own thread
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        System.err.println("***");
        System.err.println("*** Monitor thread starting");
        System.err.println("***");
        String what = "";
        // 			call executeCommand("monitor:<time>")
        //			while scanner.nextLine()
        //				filter out the things that aren't interesting
        //				and call the callback
        //			and when there is an error
        //				call connect() again
        //				then call executeCommand("monitor:<time>")
        //				and go back into the while loop
        try {
            String monitorCommand = "monitor:" + getLastReceivedTime();
            what = "send monitor command " + monitorCommand;
            executeCommand(monitorCommand, true, false);

            while (isShouldBeRunning() && socketScanner.hasNextLine()) {
                what = "read from connection";
                String response = parseRawResponse(socketScanner.nextLine());
                if (verbose) System.out.println("\tRCVD (MONITOR): " + response);
                AtEventType eventType;
                HashMap<String, Object> eventData = new HashMap<>();
                what = "parse monitor message";
                try {
                    if (response.startsWith("data:ok")) {
                        eventType = monitorHeartbeatAck;
                        eventData.put("key", "__heartbeat__");
                        eventData.put("value", response.substring("data:".length()));
                        lastHeartbeatAckTime = System.currentTimeMillis();

                    } else if (response.startsWith("data:")) {
                        eventType = monitorException;
                        eventData.put("key", "__monitorException__");
                        eventData.put("value", response);
                        eventData.put("exception", "Unexpected 'data:' message from server");

                    } else if (response.startsWith("error:")) {
                        eventType = monitorException;
                        eventData.put("key", "__monitorException__");
                        eventData.put("value", response);
                        eventData.put("exception", "Unexpected 'error:' message from server");

                    } else if (response.startsWith("notification:")) {
                        // if id is -1 then it's a stats update
                        // if id is > 0 then it's a data notification:
                        //   operation will be either 'update' or 'delete'
                        //   key will be the key that has changed
                        //   value will be the value, if available, or null, if not (e.g. when ttr == 0, value is not available)
                        eventData = mapper.readValue(response.substring("notification:".length()), HashMap.class);
                        String id = (String) eventData.get("id");
                        String operation = (String) eventData.get("operation");
                        String key = (String) eventData.get("key");
                        setLastReceivedTime(eventData.containsKey("epochMillis")
                                ? (long) eventData.get("epochMillis")
                                : System.currentTimeMillis());

                        if (id.equals("-1")) {
                            eventType = statsNotification;

                        } else if ("update".equals(operation)) {
                            if (key.startsWith(getAtSign() + ":shared_key@")) {
                                eventType = sharedKeyNotification;
                            } else {
                                eventType = updateNotification;
                            }

                        } else if ("delete".equals(operation)) {
                            eventType = deleteNotification;

                        } else {
                            eventType = monitorException;
                            eventData.put("key", "__monitorException__");
                            eventData.put("value", response);
                            eventData.put("exception", "Unknown notification operation '" + operation);
                        }
                    } else {
                        eventType = monitorException;
                        eventData.put("key", "__monitorException__");
                        eventData.put("value", response);
                        eventData.put("exception", "Malformed response from server");

                    }
                } catch (Exception e)  {
                    System.err.println("" + e);
                    eventType = monitorException;
                    eventData.put("key", "__monitorException__");
                    eventData.put("value", response);
                    eventData.put("exception", e.toString());
                }
                eventBus.publishEvent(eventType, eventData);
            }
            System.err.println("Monitor ending normally - shouldBeRunning is " + isShouldBeRunning());
        } catch (Exception e) {
            if (! isShouldBeRunning()) {
                System.err.println("shouldBeRunning is false, and monitor has stopped OK. Exception was : " + e.getMessage());
            } else {
                String message = "Monitor failed to " + what + " : " + e.getMessage();
                System.err.println(message);
                e.printStackTrace(System.err);

                System.err.println("Monitor ending. Monitor heartbeat thread should restart the monitor shortly");
                disconnect();
            }
        } finally {
            running = false;
            disconnect();
        }
    }
}
