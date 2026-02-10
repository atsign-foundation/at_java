package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;

import lombok.extern.slf4j.Slf4j;

/**
 * A command-line interface half-example half-utility to get something that was shared by another
 * atSign
 */
@Slf4j
public class Get {

  public static void main(String[] args) throws Exception {

    if (args.length != 4) {
      System.err.println("Usage: Get <rootUrl> <your AtSign> <other AtSign> <name of shared key, including namespace>");
      System.exit(1);
    }

    String rootUrl = args[0];
    AtSign atSign = new AtSign(args[1]);
    AtSign otherAtSign = new AtSign(args[2]);
    String keyName = args[3];

    // all AtClients require AtKeys, this loads them based on the AtSign from the default location
    AtKeys keys = KeysUtil.loadKeys(atSign);

    try (AtClient atClient = AtClient.withRemoteSecondary(rootUrl, atSign, keys, true)) {

      Keys.SharedKey key = Keys.sharedKeyBuilder()
          .sharedBy(otherAtSign)
          .sharedWith(atSign)
          .name(keyName)
          .build();

      String response = atClient.get(key).get();

      log.info("get response : {}", response);
    }
  }
}
