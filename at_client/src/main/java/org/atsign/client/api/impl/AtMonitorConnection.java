package org.atsign.client.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.client.api.Secondary;
import org.atsign.common.AtSign;

import java.io.IOException;
import java.util.Map;

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

    private final RemoteSecondary remoteSecondary;

    private boolean _shouldBeRunning = false;
    private void setShouldBeRunning(boolean b) {
        System.err.println("Setting shouldBeRunning " + b);
        _shouldBeRunning = b;
    }

    public boolean isShouldBeRunning() {
        return _shouldBeRunning;
    }

    public AtMonitorConnection(RemoteSecondary remoteSecondary, AtSign atSign, String secondaryUrl, Authenticator authenticator, boolean logging) {
        // Note that the Monitor doesn't make use of the auto-reconnect functionality, it does its own thing
        super(atSign, secondaryUrl, authenticator, false, logging);
        this.remoteSecondary = remoteSecondary;
        startHeartbeat();
    }

    private long lastHeartbeatSentTime = 0;
    private long lastHeartbeatAckTime = System.currentTimeMillis();
    private final int heartbeatIntervalMillis = 30000;
    private void startHeartbeat() {
        new Thread(() -> {
            while (true) {
                if (isShouldBeRunning()) {
                    if (!isRunning() || lastHeartbeatSentTime - lastHeartbeatAckTime >= heartbeatIntervalMillis) {
                        try {
                            // heartbeats have stopped being acked
                            stopMonitor("monitor heartbeat");
                            disconnect();
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
                            } else {
                                System.err.println("heartbeat thread - monitor has stopped OK");
                            }
                            startMonitor("monitor heartbeat");
                        } catch (Exception e) {
                            System.err.println("Monitor restart failed " + e);
                            e.printStackTrace(System.err);
                        }
                    } else {
                        if (System.currentTimeMillis() - lastHeartbeatSentTime > heartbeatIntervalMillis) {
                            try {
                                System.err.println("Sending heartbeat");
                                executeCommand("noop:0", false, false);
                                System.err.println("Sent heartbeat");
                                lastHeartbeatSentTime = System.currentTimeMillis();
                            } catch (Exception heartbeatSendException) {
                                System.err.println("heartbeat send resulted in exception : " + heartbeatSendException.getMessage());
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
    public synchronized boolean startMonitor(String requester) {
        System.err.println("Monitor START requested by " + requester);
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

    public synchronized void stopMonitor(String requester) {
        System.err.println("Monitor STOP requested by " + requester);
        setShouldBeRunning(false);
        disconnect();
    }

    /**
     * Please don't call this directly. Call startMonitor() instead, which starts the monitor in its own thread
     */
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
                Secondary.EventType eventType;
                String eventDataAsString;
                what = "parse monitor message";
                try {
                    if (response.startsWith("data:ok")) {
                        eventType = Secondary.EventType.heartbeatAck;
                        eventDataAsString = response.substring("data:".length());
                        lastHeartbeatAckTime = System.currentTimeMillis();
                    } else if (response.startsWith("data:")) {
                        eventType = Secondary.EventType.data;
                        eventDataAsString = response.substring("data:".length());
                    } else if (response.startsWith("error:")) {
                        eventType = Secondary.EventType.error;
                        eventDataAsString = response.substring("error:".length());
                    } else if (response.startsWith("notification:")) {
                        // if id is -1 then it's a stats update
                        // if id is > 0 then it's a data notification:
                        //   operation will be either 'update' or 'delete'
                        //   key will be the key that has changed
                        //   value will be the value, if available, or null, if not (e.g. when ttr == 0, value is not available)
                        eventDataAsString = response.substring("notification:".length());
                        @SuppressWarnings("rawtypes") Map map = mapper.readValue(eventDataAsString, Map.class);
                        String id = (String) map.get("id");
                        String operation = (String) map.get("operation");
                        String key = (String) map.get("key");
                        @SuppressWarnings("unused") String value = (String) map.get("value");
                        setLastReceivedTime(map.containsKey("epochMillis")
                                ? (long) map.get("epochMillis")
                                : System.currentTimeMillis());
                        if (id.equals("-1")) {
                            eventType = Secondary.EventType.statsNotification;
                        } else if ("update".equals(operation)) {
                            if (key.startsWith(getAtSign() + ":shared_key@")) {
                                eventType = Secondary.EventType.sharedKeyNotification;
                            } else {
                                eventType = Secondary.EventType.updateNotification;
                            }
                        } else if ("delete".equals(operation)) {
                            eventType = Secondary.EventType.deleteNotification;
                        } else {
                            eventType = Secondary.EventType.unknownNotification;
                        }
                    } else {
                        eventType = Secondary.EventType.unknown;
                        eventDataAsString = response;
                    }
                } catch (Exception e)  {
                    System.err.println("" + e);
                    eventType = Secondary.EventType.unknown;
                    eventDataAsString = response;
                }
                remoteSecondary.handleEvent(eventType, eventDataAsString);
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
