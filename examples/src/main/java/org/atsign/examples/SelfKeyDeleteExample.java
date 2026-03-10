package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.impl.clients.AtClients;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.exceptions.AtClientConfigException;

public class SelfKeyDeleteExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
    String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory)
    boolean VERBOSE = true; // true for more print logs

    String KEY_NAME = "test"; // name of the key we will create and put

    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. build an AtClient
    AtClients.AtClientsBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(atSign)
        .keys(loadKeys(atSign))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create self key
      SelfKey sk = Keys.selfKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. delete the key
      atClient.delete(sk).get();

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
