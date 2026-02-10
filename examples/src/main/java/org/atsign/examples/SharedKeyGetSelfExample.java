package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.SharedKey;

public class SharedKeyGetSelfExample {
  /// Get a SharedKey that you created and shared with another atSign
  public static void main(String[] args) {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR_SHARED_BY = "@33thesad"; // my atSign (sharedBy)
    String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // other atSign (sharedWith)
    boolean VERBOSE = true;
    String KEY_NAME = "test";

    // 2. create AtSign objects
    AtSign sharedBy = new AtSign(ATSIGN_STR_SHARED_BY);
    AtSign sharedWith = new AtSign(ATSIGN_STR_SHARED_WITH);

    // 3. atClient factory method
    try (AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, sharedBy, loadKeys(sharedBy), VERBOSE)) {

      // 4. create SharedKey instance
      SharedKey sk = Keys.sharedKeyBuilder().sharedBy(sharedBy).sharedWith(sharedWith).name(KEY_NAME).build();

      // 5. get the key
      String response = atClient.get(sk).get();
      System.out.println(response);

    } catch (AtException | IOException | InterruptedException | ExecutionException e) {
      System.err.println("Failed to create AtClient instance " + e);
      e.printStackTrace();
    }
  }

}
