package org.atsign.client.cli;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;
import static org.atsign.client.api.AtEvents.AtEventType.updateNotification;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.AtEvents.AtEventType;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.client.util.KeyStringUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;

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
        } catch (IOException | AtException e) {
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
        System.out.println("Type \'_help\' from help");
        Scanner cliScanner = new Scanner(System.in);
        System.out.print(client.getAtSign() + "@ ");

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
                    try {
                        if("help".equals(verb)) {
                            printHelpInstructions();
                        } else if ("get".equals(verb)) {
                            String fullKeyName = parts[1];
                            KeyStringUtil keyStringUtil = new KeyStringUtil(fullKeyName);
                            KeyStringUtil.KeyType keyType = keyStringUtil.getKeyType();
                            if(keyType.equals(KeyStringUtil.KeyType.PUBLIC_KEY)) {
                                PublicKey pk = (PublicKey) Keys.fromString(fullKeyName);
                                String value = client.get(pk).get();
                                System.out.println("  => \033[31m" + value + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.SELF_KEY)) {
                                SelfKey sk = (SelfKey) Keys.fromString(fullKeyName);
                                String value = client.get(sk).get();
                                System.out.println("  => \033[31m" + value + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.SHARED_KEY)) {
                                SharedKey sk = Keys.SharedKey.fromString(fullKeyName);
                                String value = client.get(sk).get();
                                System.out.println("  => \033[31m" + value + "\033[0m");                            } else if(keyType.equals(KeyStringUtil.KeyType.PRIVATE_HIDDEN_KEY)) {
                                throw new AtException("PrivateHiddenKey is not implemented yet");
                            } else {
                                throw new AtException("Could not evaluate the key type of: " + fullKeyName);
                            }
                        } else if ("put".equals(verb)) {
                            String fullKeyName = parts[1];
                            String value = command.substring(verb.length() + fullKeyName.length() + 2).trim();
                            KeyStringUtil keyStringUtil = new KeyStringUtil(fullKeyName);
                            KeyStringUtil.KeyType keyType = keyStringUtil.getKeyType();
                            if(keyType.equals(KeyStringUtil.KeyType.PUBLIC_KEY)) {
                                PublicKey pk = (PublicKey) Keys.fromString(fullKeyName);
                                String data = client.put(pk, value).get();
                                System.out.println("  => \033[31m" + data + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.SELF_KEY)) {
                                SelfKey sk = (SelfKey) Keys.fromString(fullKeyName);
                                String data = client.put(sk, value).get();
                                System.out.println("  => \033[31m" + data + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.SHARED_KEY)) {
                                SharedKey sk = Keys.SharedKey.fromString(fullKeyName);
                                String data = client.put(sk, value).get();
                                System.out.println("  => \033[31m" + data + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.PRIVATE_HIDDEN_KEY)) {
                                throw new AtException("PrivateHiddenKey is not implemented yet");
                            } else {
                                throw new AtException("Could not evaluate the key type of: " + fullKeyName);
                            }
                        } else if ("scan".equals(verb)) {
                            String regex = "";
                            if(parts.length > 1) regex = parts[1];
                            List<Keys.AtKey> value = client.getAtKeys(regex).get();
                            System.out.println("  => \033[31m" + value + "\033[0m");
                        } else if("delete".equals(verb)) {
                            String fullKeyName = parts[1];
                            KeyStringUtil keyStringUtil = new KeyStringUtil(fullKeyName);
                            KeyStringUtil.KeyType keyType = keyStringUtil.getKeyType();
                            if(keyType.equals(KeyStringUtil.KeyType.PUBLIC_KEY)) {
                                PublicKey pk = (PublicKey) Keys.fromString(fullKeyName);
                                String data = client.delete(pk).get();
                                System.out.println("  => \033[31m" + data + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.SELF_KEY)) {
                                SelfKey sk = (SelfKey) Keys.fromString(fullKeyName);
                                String data = client.delete(sk).get();
                                System.out.println("  => \033[31m" + data + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.SHARED_KEY)) {
                                SharedKey sk = Keys.SharedKey.fromString(fullKeyName);
                                String data = client.delete(sk).get();
                                System.out.println("  => \033[31m" + data + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.PRIVATE_HIDDEN_KEY)) {
                                throw new AtException("PrivateHiddenKey is not implemented yet");
                            } else {
                                throw new AtException("Could not evaluate the key type of: " + fullKeyName);
                            }
                        } else if("share".equals(verb)) {
                            // _share <atSign> <keyName> <...data>
                            // example: I am @bob, run _share @alice test hello world!! | will create a key "@alice:test@bob" with encrypted value "hello world!!"

                            String atSign = parts[1];
                            String keyName = parts[2];
                            String value = command.substring(verb.length() + atSign.length() + keyName.length() + 3).trim();

                            String fullKeyName = atSign + ":" + keyName + client.getAtSign();
                            SharedKey sk = Keys.SharedKey.fromString(fullKeyName);
                            String data = client.put(sk, value).get();
                            System.out.println("  => \033[31m" + data + "\033[0m");
                        } else {
                            System.err.println("ERROR: command not recognized: [" + verb + "]");
                        }
                    } catch (Exception e) {
                        System.err.println("Exception: " + e.toString());
                    }
                } else {
                    try {
                        response = client.executeCommand(command, true);
                        System.out.println("  => \033[31m" + response.toString() + "\033[0m");
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
            System.out.print(client.getAtSign() + "@ ");
        }
        cliScanner.close();
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
                    System.out.print(client.getAtSign() + "@ ");
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
                        System.out.println("  => Notification ==> \033[31m Key: [" + sharedKey + "]  ==> DecryptedValue [" + decryptedValue + "]" + "\033[0m");
                        System.out.print(client.getAtSign() + "@ ");
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

    public void printHelpInstructions() {
        System.out.println("\nAtClient REPL | <> = required, [] = optional");
        System.out.println("  REPL evaluates atProtocol by default. Use _ to use AtClient commands (see below)");
        System.out.println("  AtClient Commands:");
        System.out.println("    _help - print this help message");
        System.out.println("    _put <key> <value> - put a value to the key (e.g. _put test@bob hello world)");
        System.out.println("    _get <key> - get a value from the key (e.g. _get test@bob)");
        System.out.println("    _delete <key> - delete a key (e.g. _delete test@bob)");
        System.out.println("    _scan [regex] - scan for keys matching the regex (e.g. _scan test@bob.*)");
        System.out.println("    _share <atSign> <keyName> <...data> - share a key with another atSign (e.g. _share @alice test hello world!!)");
        System.out.println();
    }
}
