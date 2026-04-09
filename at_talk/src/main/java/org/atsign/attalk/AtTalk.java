package org.atsign.attalk;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.commands.SharedKeyCommands.getEncryptKey;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.AtClients;
import org.atsign.client.impl.cli.AtSignConverter;
import org.atsign.client.impl.commands.MonitorOptions;
import org.atsign.client.impl.commands.builders.NotifyUpdateSharedKeyCommandBuilder;
import org.atsign.client.impl.exceptions.AtException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * A Java port of at_talk
 */
@Slf4j
public class AtTalk implements Callable<Integer> {

  public static final String KEYNAME = "attalk";

  @Option(names = {"-k", "--key-file"}, description = "Your atSign's atKeys file if not in ~/.atsign/keys/")
  private String keysPath;

  @Option(required = true, names = {"-a", "--atsign"}, description = "Your atSign", converter = AtSignConverter.class)
  private AtSign atSign;

  @Option(required = true, names = {"-t", "--toatsign"}, description = "Talk to this atSign",
      paramLabel = "@atsign", converter = AtSignConverter.class)
  private AtSign toAtSign;

  @Option(names = {"-d", "--root-domain"}, description = "Root Domain (defaults to root.atsign.org)")
  private String rootDomain = "root.atsign.org";

  @Option(names = {"-n", "--namespace"}, description = "Namespace (defaults to ai6bh)")
  private String namespace = "ai6bh";

  @Option(names = {"-v", "--verbose"}, description = "logs sent and received at commands as INFO")
  private boolean verbose;
  private Keys.SharedKey key;
  private NotifyUpdateSharedKeyCommandBuilder builder;

  public static void main(String[] args) {
    System.exit(execute(args));
  }

  public static int execute(String[] args) {
    return new CommandLine(new AtTalk())
        .setUsageHelpWidth(80)
        .setAllowOptionsAsOptionParameters(true)
        .execute(args);
  }

  @Override
  public Integer call() throws Exception {

    boolean hasTerminal = detectHasTerminal();
    key = createSharedKey();
    builder = new NotifyUpdateSharedKeyCommandBuilder().key(key);

    System.out.print(ansi().cursorToColumn(0).bold().fg(Ansi.Color.BLUE).a("Connecting ... ").reset());

    try (AtClient client = createAtClient()) {

      System.out.println(ansi().fg(Ansi.Color.GREEN).a("Connected").reset());

      // register for decrypted update notifications
      client.addEventListener(this::onNotification, Set.of(decryptedUpdateNotification));

      // set toAtSign's public key
      builder.sharedWithPublicKey(getEncryptKey(client.getCommandExecutor(), toAtSign));

      StringBuilder buffer = new StringBuilder();
      Scanner scanner = new Scanner(System.in);
      writePrompt();
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (isExit(line)) {
          return 0;
        } else if (isSwitchToAtSign(line)) {
          setToAtSign(client, createAtSign(line));
        } else if (!hasTerminal) {
          buffer.append(line).append("\r\n");
        } else {
          client.getCommandExecutor().sendSync(builder.build(line));
        }
        if (hasTerminal) {
          writePrompt();
        }
      }
      scanner.close();

      if (buffer.length() > 0) {
        String s = ansi().fg(Ansi.Color.BLUE).a("Sending a file").reset().toString()
            + ansi().fg(Ansi.Color.WHITE).a(buffer).reset().toString();
        client.getCommandExecutor().sendSync(builder.build(s));
      }
    }
    return 0;
  }

  private static boolean isExit(String line) {
    return "/exit".equals(line);
  }

  private static boolean isSwitchToAtSign(String line) {
    return line.matches("@.+");
  }

  private void setToAtSign(AtClient client, AtSign atSign) throws AtException {
    toAtSign = atSign;
    key = createSharedKey();
    builder.key(key);
    builder.sharedWithPublicKey(getEncryptKey(client.getCommandExecutor(), toAtSign));
  }

  private Keys.SharedKey createSharedKey() {
    return Keys.sharedKeyBuilder()
        .name(KEYNAME)
        .namespace(namespace)
        .sharedBy(atSign)
        .sharedWith(toAtSign)
        .build();
  }

  private AtClient createAtClient() throws AtException {
    return AtClients.builder()
        .url(rootDomain)
        .keysPath(keysPath)
        .atSign(atSign)
        .withMonitoring(true)
        .monitorOptions(MonitorOptions.builder().selfNotification(true).build())
        .isVerbose(verbose)
        .build();
  }

  private void writePrompt() {
    System.out.print(ansi().bold().fg(Ansi.Color.RED).a(atSign + ": ").reset());
  }

  public void onNotification(AtEvents.AtEventType eventType, Map<String, Object> data) {
    if (isKeyMatch(data)) {
      System.out.print(ansi().cursorToColumn(0).eraseLine());
      System.out.print(ansi().bold().fg(Ansi.Color.GREEN).a(toAtSign + ": ").reset());
      System.out.print(ansi().fg(Ansi.Color.GREEN).a((String) data.get("decryptedValue")).reset());
      System.out.println();
      writePrompt();
    }
  }

  private boolean isKeyMatch(Map<String, Object> data) {
    String key = (String) data.get("key");
    return key != null && key.contains(KEYNAME + "." + namespace);
  }

  private static boolean detectHasTerminal() {
    try {
      AnsiConsole.systemInstall();
      return AnsiConsole.out().getType() != AnsiType.Unsupported;
    } finally {
      AnsiConsole.systemUninstall();
    }
  }
}
