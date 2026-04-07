package org.atsign.examples;

import static org.atsign.client.api.AtSign.createAtSign;

import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.SelfKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class SelfKeyPutExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ATSIGN_STR = "@33thesad";
    String KEY_NAME = "test";
    String VALUE = "I hate pineapple on pizza!!!";
    long ttl = TimeUnit.SECONDS.toMillis(30);

    // 2. create AtSign object
    AtSign atSign = createAtSign(ATSIGN_STR);

    // 3. build an AtClient
    try (AtClient atClient = AtClients.builder().atSign(atSign).build()) {

      // 4. create selfkey
      SelfKey sk = Keys.selfKeyBuilder().sharedBy(atSign).name(KEY_NAME).ttl(ttl).build();

      // 5. put the key
      atClient.put(sk, VALUE);

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
