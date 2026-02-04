package org.atsign.examples;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.SelfKey;

import static org.atsign.client.util.KeysUtil.loadKeys;

public class SelfKeyPutExample {

  public static void main(String[] args) {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR = "@33thesad";
    boolean VERBOSE = true;

    String KEY_NAME = "test";
    String VALUE = "I hate pineapple on pizza!!!";
    int ttl = 30 * 60 * 1000;


    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. atClient factory method
    try (AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign, loadKeys(atSign), VERBOSE)) {

      // 4. create selfkey
      SelfKey sk = new KeyBuilders.SelfKeyBuilder(atSign).key(KEY_NAME).build();
      sk.metadata.ttl = ttl;

      // 5. put the key
      String response = atClient.put(sk, VALUE).get();
      System.out.println(response);

    } catch (AtException | IOException | InterruptedException | ExecutionException e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
