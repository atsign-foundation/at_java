package org.atsign.client.cli;

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
import org.atsign.common.exceptions.AtIllegalArgumentException;
import org.atsign.common.exceptions.AtInvalidSyntaxException;
import org.atsign.common.exceptions.AtNotYetImplementedException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.*;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;
import static org.atsign.client.api.AtEvents.AtEventType.updateNotification;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * A command-line interface half-example half-utility which connects
 * to the remote secondary server, authenticates, and then simply sends whatever
 * commands the user types, and outputs whatever the server returns.
 */
public class REPL {
    public static void main(String[] args) {
        AnsiConsole.systemInstall();

        String rootUrl; // e.g. "root.atsign.org:64";
        AtSign atSign;  // e.g. "@alice";
        boolean verbose = false;

        if (args.length < 3) {
            System.err.println("Usage: REPL <rootUrl> <atSign> <seeEncryptedNotifications == 'true|false'> [<verbose == 'true|false'>]");
            System.exit(1);
        }

        rootUrl = args[0];
        atSign = new AtSign(args[1]);
        boolean seeEncryptedNotifications = Boolean.parseBoolean(args[2]);
        if (args.length >= 4) {
            verbose = Boolean.parseBoolean(args[3]);
        }

        AtClient atClient;
        try {
            System.out.print(ansi().cursorToColumn(0).bold().fg(Ansi.Color.BLUE).a("Connecting ... ").reset());
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl), verbose);

            System.out.println(ansi().fg(Ansi.Color.GREEN).a("connected. ").reset().a("Type '/help' to see help").reset());

            REPL repl = new REPL(atClient, seeEncryptedNotifications);
            repl.repl();
        } catch (IOException | AtException e) {
            System.out.println(ansi().fg(Ansi.Color.RED).a("connection failed: " + e).reset());
            e.printStackTrace();
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

        writePrompt();

        while (cliScanner.hasNextLine()) {
            String command = cliScanner.nextLine() + "\n";
            if (! command.trim().isEmpty()) {
                command = command.trim();
                Secondary.Response response;
                if (command.equals("help") || command.startsWith("_") || command.startsWith("/") || command.startsWith("\\")) {
                    // simple repl for get / put /
                    if (! command.equals("help")) {
                        command = command.substring(1);
                    }
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
                                System.out.println("  => \033[31m" + value + "\033[0m");
                            } else if(keyType.equals(KeyStringUtil.KeyType.PRIVATE_HIDDEN_KEY)) {
                                throw new AtNotYetImplementedException("PrivateHiddenKey is not implemented yet");
                            } else {
                                throw new AtInvalidSyntaxException("Could not evaluate the key type of: " + fullKeyName);
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
                                throw new AtNotYetImplementedException("PrivateHiddenKey is not implemented yet");
                            } else {
                                throw new AtIllegalArgumentException("Could not evaluate the key type of: " + fullKeyName);
                            }
                        } else if ("scan".equals(verb)) {
                            String regex = "";
                            if(parts.length > 1) regex = parts[1];
                            List<Keys.AtKey> value = client.getAtKeys(regex, false).get();
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
                                throw new AtNotYetImplementedException("PrivateHiddenKey is not implemented yet");
                            } else {
                                throw new AtIllegalArgumentException("Could not evaluate the key type of: " + fullKeyName);
                            }
                        } else {
                            System.err.println("ERROR: command not recognized: [" + verb + "]");
                        }
                    } catch (Exception e) {
                        //noinspection ThrowablePrintedToSystemOut
                        System.err.println(e);
                    }
                } else {
                    try {
                        response = client.executeCommand(command, true);
                        System.out.println("  => \033[31m" + response.toString() + "\033[0m");
                    } catch (AtException | IOException e) {
                        System.err.println("*** " + e);
                    }
                }
            }
            writePrompt();
        }
        cliScanner.close();
    }

    void writePrompt() {
        System.out.print(ansi().bold().fg(Ansi.Color.MAGENTA).a(client.getAtSign() + "@ ").reset());
    }

    public static class REPLEventListener implements AtEvents.AtEventListener {
        private final AtClient client;

        void writePrompt() {
            System.out.print(ansi().bold().fg(Ansi.Color.MAGENTA).a(client.getAtSign() + "@ ").reset());
        }
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
                    System.out.println("  => Notification ==> \033[31m Key: [" + sharedKey + "]  ==> EncryptedValue [" + value + "]  ==> DecryptedValue [" + decryptedValue + "]" + "\033[0m");
                    writePrompt();
                }
                break;
                case updateNotificationText: {
                    System.out.println(eventData);
                    writePrompt();
                }
                break;
                case updateNotification: {
                    try {
                        sharedKey = Keys.SharedKey.fromString((String) eventData.get("key"));
                        String encryptedValue = (String) eventData.get("value");
                        decryptedValue = client.get(sharedKey).get();
                        System.out.println("  => Notification ==> \033[31m Key: [" + sharedKey + "]  ==> EncryptedValue [" + encryptedValue + "]  ==> DecryptedValue [" + decryptedValue + "]" + "\033[0m");
                        writePrompt();
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
        System.out.println();
        System.out.println("AtClient REPL");
        System.out.println(ansi().render("  Notes:"));
        System.out.println(ansi().render("    1) By default, REPL treats input as atProtocol commands. Use / for additional commands listed below"));
        System.out.println(ansi().render("    2) In the usage examples below, it is assumed that the atSign being used is @|bold,green @alice|@"));
        System.out.println();
        System.out.println(ansi().render("  @|bold,magenta help|@ or @|bold,magenta /help|@ - print this help message"));
        System.out.println();
        System.out.println(ansi().render("  @|bold,magenta /scan|@ @|bold,green [regex]|@ - scan for all records, or all records whose keyNames match the regex (e.g. _scan test@alice.*)"));
        System.out.println();
        System.out.println(ansi().render("  @|bold,magenta /put|@ @|bold,green <atKeyName>|@ @|bold,blue <value>|@ - create or update a record with the given atKeyName and with the supplied value - for example:"));
        System.out.println(ansi().render("    @|bold,magenta /put|@ @|bold,green test@alice|@ @|bold,blue secret secrets|@ will create or update a 'self' record (a record private just to @alice)"));
        System.out.println(ansi().render("    @|bold,magenta /put|@ @|bold,green @bob:test@alice|@ @|bold,blue Hello, Bob!|@ will create or update a record encrypted for, and then shared with, @bob"));
        System.out.println();
        System.out.println(ansi().render("  @|bold,magenta /get|@ @|bold,green <atKeyName>|@ - retrieve a value from the record with this atKeyName - for example:"));
        System.out.println(ansi().render("    @|bold,magenta /get|@ @|bold,green <atKeyName>|@ - retrieve a value from the record with this atKeyName (e.g. _get test@alice)"));
        System.out.println();
        System.out.println(ansi().render("  @|bold,magenta /delete|@ @|bold,green <atKeyName>|@ - delete the record with this atKeyName (e.g. _delete test@alice)"));
        System.out.println();
        System.out.println(ansi().render("  @|bold,red NOTE:|@ @|bold,magenta put, get|@ and @|bold,magenta delete|@ will append the current atSign to the atKeyName if not supplied"));
        System.out.println();
    }
}
