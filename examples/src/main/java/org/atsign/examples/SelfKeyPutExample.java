package org.atsign.examples;

import static org.atsign.client.util.KeysUtil.loadKeys;

import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.impl.clients.AtClients;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.exceptions.AtClientConfigException;

public class SelfKeyPutExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR = "@33thesad";
    boolean VERBOSE = true;

    String KEY_NAME = "test";
    String VALUE = "I hate pineapple on pizza!!!";
    long ttl = TimeUnit.SECONDS.toMillis(30);


    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. build an AtClient
    AtClients.AtClientsBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(atSign)
        .keys(loadKeys(atSign))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create selfkey
      SelfKey sk = Keys.selfKeyBuilder().sharedBy(atSign).name(KEY_NAME).ttl(ttl).build();

      // 5. put the key
      atClient.put(sk, VALUE).get();

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
