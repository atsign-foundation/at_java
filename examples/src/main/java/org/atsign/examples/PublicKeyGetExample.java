package org.atsign.examples;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.impl.clients.AtClients;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.exceptions.AtClientConfigException;

import static org.atsign.client.util.KeysUtil.loadKeys;

public class PublicKeyGetExample {
  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish arguments
    String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
    String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory)
    boolean VERBOSE = true; // true for more print logs

    String KEY_NAME = "test"; // name of the key we will get

    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. build an AtClient
    AtClients.AtClientsBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(atSign)
        .keys(loadKeys(atSign))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create the key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. get the value associated with the key
      String response = atClient.get(pk).get();
      System.out.println(response);

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }
}
