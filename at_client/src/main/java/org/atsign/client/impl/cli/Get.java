package org.atsign.client.impl.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.AtClients;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;

import lombok.extern.slf4j.Slf4j;

import static org.atsign.client.api.AtSign.createAtSign;

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
    AtSign atSign = createAtSign(args[1]);
    AtSign otherAtSign = createAtSign(args[2]);
    String keyName = args[3];

    // all AtClients require AtKeys, this loads them based on the AtSign from the default location
    AtKeys keys = KeysUtils.loadKeys(atSign);

    try (AtClient atClient = AtClients.builder().url(rootUrl).atSign(atSign).keys(keys).build()) {

      Keys.SharedKey key = Keys.sharedKeyBuilder()
          .sharedBy(otherAtSign)
          .sharedWith(atSign)
          .name(keyName)
          .build();

      String response = atClient.get(key).get();

      System.out.printf("get response : %s", response);
    }
  }
}
