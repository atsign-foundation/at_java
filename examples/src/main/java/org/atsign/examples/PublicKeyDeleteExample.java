package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.PublicKey;

public class PublicKeyDeleteExample {

  public static void main(String[] args) {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR = "@33thesad";
    boolean VERBOSE = true;

    String KEY_NAME = "test";

    // 2. create AtSign instance
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. create AtClient instance using factory methods
    try (AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign, loadKeys(atSign), VERBOSE)) {

      // 4. create public key
      PublicKey pk = new KeyBuilders.PublicKeyBuilder(atSign).key(KEY_NAME).build();

      // 5. delete the key
      String response = atClient.delete(pk).get();
      System.out.println(response);

    } catch (AtException | IOException | InterruptedException | ExecutionException e) {
      System.err.println(e);
      e.printStackTrace();
    }

  }

}
