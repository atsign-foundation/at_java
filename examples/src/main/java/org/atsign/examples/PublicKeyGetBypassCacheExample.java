package org.atsign.examples;


import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.atsign.client.api.AtClient.GetRequestOptions;

import static org.atsign.client.api.AtSign.createAtSign;

public class PublicKeyGetBypassCacheExample {
  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish arguments
    String ATSIGN_STR = "@alice"; // atSign that we will pkam auth (must have keys in keys directory)
    String KEY_NAME = "publickey"; // name of the key we will get

    // 2. create AtSign object
    AtSign atSign = createAtSign(ATSIGN_STR);

    // 3. build an AtClient
    try (AtClient atClient = AtClients.builder().atSign(atSign).build()) {

      // 4. create the key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(createAtSign("@bob")).name(KEY_NAME).build();

      // 5. get the value associated with the key
      String response = atClient.get(pk, GetRequestOptions.builder().bypassCache(true).build());
      System.out.println(response);

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }


  }
}
