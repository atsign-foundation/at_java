package org.atsign.examples;


import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.atsign.client.api.AtClient.GetRequestOptions;

import static org.atsign.client.impl.util.KeysUtils.loadKeys;

public class PublicKeyGetBypassCacheExample {
  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish arguments
    String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
    String ATSIGN_STR = "@alice"; // atSign that we will pkam auth (must have keys in keys directory)
    boolean VERBOSE = true; // true for more print logs

    String KEY_NAME = "publickey"; // name of the key we will get

    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. build an AtClient
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(atSign)
        .keys(loadKeys(atSign))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create the key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(new AtSign("@bob")).name(KEY_NAME).build();

      // 5. get the value associated with the key
      String response = atClient.get(pk, GetRequestOptions.builder().bypassCache(true).build()).get();
      System.out.println(response);

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }


  }
}
