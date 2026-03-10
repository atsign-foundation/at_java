package org.atsign.examples;

import static org.atsign.client.impl.util.KeysUtils.loadKeys;


import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.SharedKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class SharedKeyGetOtherExample {
  /// Get the SharedKey sharedBy another person and sharedWith you
  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR_SHARED_BY = "@33thesad"; // their atSign (key is sharedBy this atSign)
    String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // your atSign (key is sharedWith you)
    boolean VERBOSE = true;
    String KEY_NAME = "test";

    // 2. create AtSign objects
    AtSign sharedBy = new AtSign(ATSIGN_STR_SHARED_BY);
    AtSign sharedWith = new AtSign(ATSIGN_STR_SHARED_WITH); // your atSign

    // 3. build an AtClient
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(sharedWith)
        .keys(loadKeys(sharedWith))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create SharedKey instance
      // key is sharedBy the other person and sharedWith you.
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
