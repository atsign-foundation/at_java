package org.atsign.examples;



import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class PublicKeyPutExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory)
    String KEY_NAME = "test"; // name of the key we will create and put
    String VALUE = "I love pineapple on pizza 12345"; // value we will associate with the key

    // 2. create AtSign object
    AtSign atSign = AtSign.of(ATSIGN_STR);

    // 3. build an AtClient
    try (AtClient atClient = AtClients.builder().atSign(atSign).build()) {

      // 4. create a new public key
      PublicKey pk = Keys.publicKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. put the key
      atClient.put(pk, VALUE);
    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }

}
