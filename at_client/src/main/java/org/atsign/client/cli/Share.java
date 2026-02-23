package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;

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
    AtSign atSign = new AtSign(args[1]);
    AtSign otherAtSign = new AtSign(args[2]);
    String keyName = args[3];
    String toShare = args[4];
    int ttr = args.length == 6 ? Integer.parseInt(args[5]) : 0;

    // all AtClients require AtKeys, this loads them based on the AtSign from the default location
    AtKeys keys = KeysUtil.loadKeys(atSign);

    try (AtClient atClient = AtClient.withRemoteSecondary(rootUrl, atSign, keys, true)) {
      Keys.SharedKey key = Keys.sharedKeyBuilder()
          .sharedBy(atSign)
          .sharedWith(otherAtSign)
          .name(keyName)
          .ccd(true)
          .ttr((long) ttr)
          .build();

      String response = atClient.put(key, toShare).get();

      log.info("put response : {}", response);
    }
  }

}
