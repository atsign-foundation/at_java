package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.Secondary;
import static org.atsign.client.api.AtEvents.*;
import static org.atsign.client.api.AtEvents.AtEventType.*;

import org.atsign.client.util.ArgsUtil;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;
import org.atsign.common.Keys;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A command-line interface half-example half-utility which connects
 * to the remote secondary server, authenticates, and then simply sends whatever
 * commands the user types, and outputs whatever the server returns.
 */
public class REPL {
    public static void main(String[] args) {
        String rootUrl; // e.g. "root.atsign.org:64";
        AtSign atSign;  // e.g. "@alice";

        if (args.length != 3) {
            System.err.println("Usage: REPL <rootUrl> <atSign> <seeEncryptedNotifications == 'true|false'>");
            System.exit(1);
        }

        rootUrl = args[0];
        atSign = new AtSign(args[1]);
        boolean seeEncryptedNotifications = Boolean.parseBoolean(args[2]);

        AtClient atClient;
        try {
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl), false);

            System.out.println("org.atsign.client.core.Client connected OK");

            REPL repl = new REPL(atClient, seeEncryptedNotifications);
            repl.repl();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Calling System.exit");
            System.exit(1);
        }
    }

    private final AtClient client;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final boolean seeEncryptedNotifications;

    public REPL(AtClient client, boolean seeEncryptedNotifications) {
        this.client = client;
        this.seeEncryptedNotifications = seeEncryptedNotifications;
        HashSet<AtEventType> eventTypes = new HashSet<>(Collections.singletonList(decryptedUpdateNotification));
        if (seeEncryptedNotifications) {
            eventTypes.add(updateNotification);
        }
        client.addEventListener(new REPLEventListener(client), eventTypes);

        client.startMonitor();
    }

    public void repl() throws AtException {
        Scanner cliScanner = new Scanner(System.in);
        System.out.print('@');

        while (cliScanner.hasNextLine()) {
            String command = cliScanner.nextLine() + "\n";
            if (! command.trim().isEmpty()) {
                command = command.trim();
                Secondary.Response response;
                if (command.startsWith("_")) {
                    // simple repl for get / put /
                    command = command.substring(1);
                    String[] parts = command.split(" ");
                    String verb = parts[0];
                    String key = "";
                    if (! "scan".equals(verb)) {
                        key = parts[1];
                    }
                    String value;
                    try {
                        if ("get".equals(verb)) {
                            System.out.println("  => " + client.get(Keys.SharedKey.fromString(key)).get());
                        } else if ("put".equals(verb)) {
                            value = command.substring(verb.length() + key.length() + 2).trim();
                            System.out.println("  => " + client.put(Keys.SharedKey.fromString(key), value).get());
                        } else if ("delete".equals(verb)) {
                            System.out.println("  => " + client.delete(Keys.SharedKey.fromString(key)));
                        } else if ("scan".equals(verb)) {
                            System.out.println("  => " + client.getAtKeys("").get());
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.toString());
                    }
                } else {
                    try {
                        response = client.executeCommand(command, true);
                        System.out.println("  => " + response.toString());
                    } catch (AtException e) {
                        // We can swallow syntax errors. Anything else, let's rethrow.
                        if (!e.toString().contains("AT0003-Invalid syntax")) {
                            throw e;
                        } else {
                            System.err.println("*** " + e);
                        }
                    }
                }
            }
            System.out.print('@');
        }
    }

    public static class REPLEventListener implements AtEvents.AtEventListener {
        private final AtClient client;

        public REPLEventListener(AtClient client) {
            this.client = client;
        }

        @Override
        public void handleEvent(AtEventType eventType, Map<String, Object> eventData) {
            // System.out.println("\t" + OffsetDateTime.now() + " REPL received Event: " + eventType.toString() + " : " + eventData);

            Keys.SharedKey sharedKey = null;
            String value, decryptedValue;
            switch (eventType) {
                case decryptedUpdateNotification: {
                    sharedKey = Keys.SharedKey.fromString((String) eventData.get("key"));
                    value = (String) eventData.get("value");
                    decryptedValue = (String) eventData.get("decryptedValue");
                    System.out.println("\t" + OffsetDateTime.now()
                            + " REPL NOTIFIED with value [" + decryptedValue + "]"
                            + " for key [" + sharedKey + "]"
                            + " (encryptedValue was [" + value + "])");
                }
                break;
                case updateNotificationText: {
                    System.out.println(eventData);
                }
                break;
                case updateNotification: {
                    try {
                        sharedKey = Keys.SharedKey.fromString((String) eventData.get("key"));
                        value = (String) eventData.get("value");
                        decryptedValue = client.get(sharedKey).get();
//                        System.out.println("\t" + OffsetDateTime.now()
//                                + " REPL Retrieved value [" + decryptedValue + "]"
//                                + " for key [" + sharedKey + "]"
//                                + " (encryptedValue was [" + value + "])");
                        System.out.println("  => Notification ==> Key: [" + sharedKey + "]  ==> DecryptedValue [" + decryptedValue + "]");
                    } catch (Exception e) {
                        System.err.println("Failed to retrieve " + sharedKey + " : " + e);
                    }
                }
                break;
                default:
                    break;
            }
        }
    }
}
