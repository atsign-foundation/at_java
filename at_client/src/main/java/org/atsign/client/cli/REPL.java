package org.atsign.client.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import static org.atsign.client.api.Secondary.EventType.*;

import org.atsign.common.AtSign;
import org.atsign.common.AtException;
import org.atsign.common.Keys;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * A command-line interface half-example half-utility which connects
 * to the remote secondary server, authenticates, and then simply sends whatever
 * commands the user types, and outputs whatever the server returns.
 */
public class REPL {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        String rootUrl; // e.g. "vip.ve.atsign.zone:64";
        AtSign atSign;  // e.g. "@alice";

        if (args.length != 2) {
            System.err.println("Usage: REPL <rootUrl> <atSign>");
            System.exit(1);
        }

        rootUrl = args[0];
        atSign = new AtSign(args[1]);

        try {
            AtClient atClient = AtClient.withRemoteSecondary(rootUrl, atSign);

            REPL repl = new REPL(atClient);
            repl.repl();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Calling System.exit");
            System.exit(1);
        }
    }

    private final AtClient client;

    public REPL(AtClient client) {
        this.client = client;
    }

    public void repl() throws AtException {

        System.out.println("org.atsign.client.core.Client connected OK");
        Scanner cliScanner = new Scanner(System.in);
        System.out.print('@');

        client.addEventListener(
                new REPLEventListener(client),
                new HashSet<>(Arrays.asList(updateNotification, deleteNotification))
                );
        while (cliScanner.hasNextLine()) {
            String command = cliScanner.nextLine() + "\n";
            if (! command.trim().isEmpty()) {
                Secondary.Response response;
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
            System.out.print('@');
        }
    }

    public static class REPLEventListener implements Secondary.EventListener {
        private final AtClient client;

        public REPLEventListener(AtClient client) {
            this.client = client;
        }

        @Override
        public void handleEvent(Secondary.EventType eventType, String eventData) {
            System.out.println("\t" + OffsetDateTime.now() + " EVNT: " + eventType.toString() + " : " + eventData);

            if (eventType == updateNotification) {
                @SuppressWarnings("rawtypes") Map map;
                try {
                    map = mapper.readValue(eventData, Map.class);
                } catch (IOException e) {
                    System.err.println("Failed to deserialize JSON : " + e);
                    return;
                }
                Keys.SharedKey sharedKey = null;
                try {
                    sharedKey = Keys.SharedKey.fromString((String) map.get("key"));
                    String value = client.get(sharedKey).get();
                    System.out.println("\t\t" + OffsetDateTime.now() + " Retrieved value [" + value + "] for key [" + sharedKey + "]");
                } catch (Exception e) {
                    System.err.println("Failed to retrieve " + sharedKey + " : " + e);
                }
            }
        }
    }
}
