package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.impl.clients.AtClients;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.exceptions.AtClientConfigException;

public class SharedKeyDeleteExample {

  /// Delete a SharedKey that you shared with another atSign, the key must be on your own secondary server (belonging to the sharedBy atSign)
  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR_SHARED_BY = "@33thesad"; // my atSign (sharedBy)
    String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // other atSign (sharedWith)
    boolean VERBOSE = true;
    String KEY_NAME = "test";

    // 2. create AtSign objects
    AtSign sharedBy = new AtSign(ATSIGN_STR_SHARED_BY);
    AtSign sharedWith = new AtSign(ATSIGN_STR_SHARED_WITH);

    // 3. build an AtClient
    AtClients.AtClientsBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(sharedBy)
        .keys(loadKeys(sharedBy))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create SharedKey instance
      SharedKey sk = Keys.sharedKeyBuilder().sharedBy(sharedBy).sharedWith(sharedWith).name(KEY_NAME).build();

      // 5. delete the key
      atClient.delete(sk).get();

    } catch (Exception e) {
      System.err.println("Failed to create AtClient instance " + e);
      e.printStackTrace();
    }
  }

}
