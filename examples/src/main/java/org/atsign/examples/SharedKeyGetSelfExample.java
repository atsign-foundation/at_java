package org.atsign.examples;

import static org.atsign.client.api.AtSign.createAtSign;


import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.SharedKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class SharedKeyGetSelfExample {
  /// Get a SharedKey that you created and shared with another atSign
  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ATSIGN_STR_SHARED_BY = "@33thesad"; // my atSign (sharedBy)
    String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // other atSign (sharedWith)
    String KEY_NAME = "test";

    // 2. create AtSign objects
    AtSign sharedBy = createAtSign(ATSIGN_STR_SHARED_BY);
    AtSign sharedWith = createAtSign(ATSIGN_STR_SHARED_WITH);

    // 3. build an AtClient
    try (AtClient atClient = AtClients.builder().atSign(sharedBy).build()) {

      // 4. create SharedKey instance
      SharedKey sk = Keys.sharedKeyBuilder().sharedBy(sharedBy).sharedWith(sharedWith).name(KEY_NAME).build();

      // 5. get the key
      String response = atClient.get(sk).get();
      System.out.println(response);

    } catch (Exception e) {
      System.err.println("Failed to create AtClient instance " + e);
      e.printStackTrace();
    }
  }

}
