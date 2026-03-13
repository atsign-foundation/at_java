package org.atsign.examples;

import static org.atsign.client.api.AtSign.createAtSign;


import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.SelfKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class SelfKeyDeleteExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory)

    String KEY_NAME = "test"; // name of the key we will create and put

    // 2. create AtSign object
    AtSign atSign = createAtSign(ATSIGN_STR);

    // 3. build an AtClient
    try (AtClient atClient = AtClients.builder().atSign(atSign).build()) {

      // 4. create self key
      SelfKey sk = Keys.selfKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. delete the key
      atClient.delete(sk).get();

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
