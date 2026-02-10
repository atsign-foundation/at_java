package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.PublicKey;

public class PublicKeyPutExample {

  public static void main(String[] args) {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
    String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory)
    boolean VERBOSE = true; // true for more print logs

    String KEY_NAME = "test"; // name of the key we will create and put
    String VALUE = "I love pineapple on pizza 12345"; // value we will associate with the key

    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. atClient factory method
    try (AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign, loadKeys(atSign), VERBOSE)) {

      // 4. create a new public key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. put the key
      String response = atClient.put(pk, VALUE).get();
      System.out.println(response);

    } catch (AtException | IOException | InterruptedException | ExecutionException e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
