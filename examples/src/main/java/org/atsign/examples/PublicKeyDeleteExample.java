package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.impl.clients.AtClients;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.exceptions.AtClientConfigException;

public class PublicKeyDeleteExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR = "@33thesad";
    boolean VERBOSE = true;

    String KEY_NAME = "test";

    // 2. create AtSign instance
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. build an AtClient
    AtClients.AtClientsBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(atSign)
        .keys(loadKeys(atSign))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create public key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. delete the key
      atClient.delete(pk).get();

    } catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
    }

  }

}
