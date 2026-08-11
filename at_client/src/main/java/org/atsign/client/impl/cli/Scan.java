package org.atsign.client.impl.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.AtClients;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.client.impl.util.StringUtils;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys.AtKey;
import org.atsign.client.api.Metadata;

import lombok.extern.slf4j.Slf4j;


/**
 * A command-line interface for scanning keys in your secondary (must have keys to atSign in keys/)
 */
@Slf4j
public class Scan {
  public static void main(String[] args) throws Exception {

    if (args.length != 3) {
      System.out.println("Incorrect usage | Scan <rootUrl> <atSign> <scan regex>");
      System.exit(1);
    }

    String rootUrl = args[0];
    AtSign atSign = AtSign.of(args[1]);
    String regex = args[2];

    // all AtClients require AtKeys, this loads them based on the AtSign from the default location
    AtKeys keys = KeysUtils.loadKeys(atSign);

    try (AtClient atClient = AtClients.builder().url(rootUrl).atSign(atSign).keys(keys).build()) {

      List<AtKey> response = atClient.getAtKeys(regex);

      String input;
      Scanner scanner = new Scanner(System.in);
      do {
        System.out.println();
        System.out.println("Enter index you want to llookup (l to list, q to quit):");
        input = scanner.nextLine();
        if (StringUtils.isNumeric(input)) {
          int index = Integer.parseInt(input);
          if (index < response.size()) {
            printKeyInfo(response.get(index), System.out);
          } else {
            System.out.println("Index out of bounds");
          }
        } else if ("l".equalsIgnoreCase(input)) {
          printKeys(response, System.out);
        } else if (!"q".equalsIgnoreCase(input)) {
          System.out.println("Invalid input");
        }
      } while (!"q".equalsIgnoreCase(input));
      scanner.close();
    }
  }

  private static void printKeys(List<AtKey> keys, PrintStream out) {
    out.println("atKeys: {");
    for (int i = 0; i < keys.size(); i++) {
      AtKey key = keys.get(i);
      out.println("  " + i + ":  " + (key.metadata().isCached() ? "cached:" : "") + key);
    }
    out.println("}");
  }

  private static void printKeyInfo(AtKey key, PrintStream out) {
    out.println("======================");
    out.println("Full KeyName: " + key.toString());
    out.println("KeyName: " + key.nameWithoutNamespace());
    out.println("Namespace: " + key.namespace());
    out.println("SharedBy: " + key.sharedBy());
    out.println("SharedWith: " + (key.sharedWith() != null ? key.sharedWith() : "null"));
    out.println("KeyType: " + key.getClass().toString().split("\\$")[1]);
    out.println("Metadata -------------------");
    printKeyMetadata(key.metadata(), out);
    out.println("======================");
    out.println();
  }

  private static void printKeyMetadata(Metadata metadata, PrintStream out) {
    out.println("ttl: " + metadata.ttl());
    out.println("ttb: " + metadata.ttb());
    out.println("ttr: " + metadata.ttr());
    out.println("ccd: " + metadata.ccd());
    out.println("availableAt: " + (metadata.availableAt() != null ? metadata.availableAt().toString() : "null"));
    out.println("expiresAt: " + (metadata.expiresAt() != null ? metadata.expiresAt().toString() : "null"));
    out.println("refreshAt: " + (metadata.refreshAt() != null ? metadata.refreshAt().toString() : "null"));
    out.println("createdAt: " + (metadata.createdAt() != null ? metadata.createdAt().toString() : "null"));
    out.println("updatedAt: " + (metadata.updatedAt() != null ? metadata.updatedAt().toString() : "null"));
    out.println("dataSignature: " + metadata.dataSignature());
    out.println("sharedKeyStatus: " + metadata.sharedKeyStatus());
    out.println("isPublic: " + metadata.isPublic());
    out.println("isEncrypted: " + metadata.isEncrypted());
    out.println("isHidden: " + metadata.isHidden());
    out.println("namespaceAware: " + metadata.namespaceAware());
    out.println("isBinary: " + metadata.isBinary());
    out.println("isCached: " + metadata.isCached());
    out.println("sharedKeyEnc: " + metadata.sharedKeyEnc());
    out.println("pubKeyCS: " + metadata.pubKeyCS());
  }
}
