package org.atsign.client.impl.cli;

import static org.atsign.client.api.AtSign.createAtSign;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.AtClients;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;

import lombok.extern.slf4j.Slf4j;

/**
 * A command-line interface half-example half-utility to share something with another atSign
 */
@Slf4j
public class Share {
  public static void main(String[] args) throws Exception {

    if (args.length < 5) {
      System.err.println("Usage: Share <rootUrl> <your AtSign> <other AtSign> <keyName to share, including namespace> "
          + "<keyValue to share, a string> <ttr>");
      System.exit(1);
    }

    String rootUrl = args[0];
    AtSign atSign = createAtSign(args[1]);
    AtSign otherAtSign = createAtSign(args[2]);
    String keyName = args[3];
    String toShare = args[4];
    int ttr = args.length == 6 ? Integer.parseInt(args[5]) : 0;

    // all AtClients require AtKeys, this loads them based on the AtSign from the default location
    AtKeys keys = KeysUtils.loadKeys(atSign);

    try (AtClient atClient = AtClients.builder().url(rootUrl).atSign(atSign).keys(keys).build()) {
      Keys.SharedKey key = Keys.sharedKeyBuilder()
          .sharedBy(atSign)
          .sharedWith(otherAtSign)
          .name(keyName)
          .ccd(true)
          .ttr((long) ttr)
          .build();

      atClient.put(key, toShare);
    }
  }

}
