package org.atsign.examples;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.options.GetRequestOptions;

import static org.atsign.client.util.KeysUtil.loadKeys;

public class PublicKeyGetBypassCacheExample {
  public static void main(String[] args) {
    // 1. establish arguments
    String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
    String ATSIGN_STR = "@alice"; // atSign that we will pkam auth (must have keys in keys directory)
    boolean VERBOSE = true; // true for more print logs

    String KEY_NAME = "publickey"; // name of the key we will get

    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. atClient factory method
    try (AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign, loadKeys(atSign), VERBOSE)) {

      // 4. create the key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(new AtSign("@bob")).name(KEY_NAME).build();

      // 5. get the value associated with the key
      String response = atClient.get(pk, GetRequestOptions.builder().bypassCache(true).build()).get();
      System.out.println(response);

    } catch (AtException | IOException | InterruptedException | ExecutionException e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }


  }
}
