package org.atsign.examples;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.impl.AtClients;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class PublicKeyDeleteExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ATSIGN_STR = "@33thesad";

    String KEY_NAME = "test";

    // 2. create AtSign instance
    AtSign atSign = AtSign.of(ATSIGN_STR);

    // 3. build an AtClient
    try (AtClient atClient = AtClients.builder().atSign(atSign).build()) {

      // 4. create public key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. delete the key
      atClient.delete(pk);

    } catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
    }

  }

}
