package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.SharedKey;

public class SharedKeyGetOtherExample {
  /// Get the SharedKey sharedBy another person and sharedWith you
  public static void main(String[] args) {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR_SHARED_BY = "@33thesad"; // their atSign (key is sharedBy this atSign)
    String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // your atSign (key is sharedWith you)
    boolean VERBOSE = true;
    String KEY_NAME = "test";

    // 2. create AtSign objects
    AtSign sharedBy = new AtSign(ATSIGN_STR_SHARED_BY);
    AtSign sharedWith = new AtSign(ATSIGN_STR_SHARED_WITH); // your atSign

    // 3. atClient factory method
    try (AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, sharedWith, loadKeys(sharedWith), VERBOSE)) {

      // 4. create SharedKey instance
      // key is sharedBy the other person and sharedWith you.
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
