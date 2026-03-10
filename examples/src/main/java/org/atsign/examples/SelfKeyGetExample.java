package org.atsign.examples;

import static org.atsign.client.impl.util.KeysUtils.loadKeys;


import org.atsign.client.api.AtClient;
import org.atsign.client.impl.AtClients;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.SelfKey;
import org.atsign.client.api.Metadata;
import org.atsign.client.impl.exceptions.AtClientConfigException;

public class SelfKeyGetExample {

  public static void main(String[] args) throws AtClientConfigException {
    // 1. establish constants
    String ROOT_URL = "root.atsign.org:64";
    String ATSIGN_STR = "@33thesad";
    boolean VERBOSE = true;

    String KEY_NAME = "test";

    // 2. create AtSign object
    AtSign atSign = new AtSign(ATSIGN_STR);

    // 3. build an AtClient
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url(ROOT_URL)
        .atSign(atSign)
        .keys(loadKeys(atSign))
        .isVerbose(VERBOSE);

    try (AtClient atClient = builder.build()) {

      // 4. create selfkey
      SelfKey sk = Keys.selfKeyBuilder().sharedBy(atSign).name(KEY_NAME).build();

      // 5. get the key
      String response = atClient.get(sk).get();
      System.out.println(response);
      _printMetadata(sk.metadata());

    } catch (Exception e) {
      System.err.println("Failed to connect to remote server " + e);
      e.printStackTrace();
    }
  }


  private static void _printMetadata(Metadata metadata) {
    System.out.println("ttl: " + metadata.ttl());
    System.out.println("ttb: " + metadata.ttb());
    System.out.println("ttr: " + metadata.ttr());
    System.out.println("ccd: " + metadata.ccd());
    System.out.println("availableAt: " + (metadata.availableAt() != null ? metadata.availableAt().toString() : "null"));
    System.out.println("expiresAt: " + (metadata.expiresAt() != null ? metadata.expiresAt().toString() : "null"));
    System.out.println("refreshAt: " + (metadata.refreshAt() != null ? metadata.refreshAt().toString() : "null"));
    System.out.println("createdAt: " + (metadata.createdAt() != null ? metadata.createdAt().toString() : "null"));
    System.out.println("updatedAt: " + (metadata.updatedAt() != null ? metadata.updatedAt().toString() : "null"));
    System.out.println("dataSignature: " + metadata.dataSignature());
    System.out.println("sharedKeyStatus: " + metadata.sharedKeyStatus());
    System.out.println("isPublic: " + metadata.isPublic());
    System.out.println("isEncrypted: " + metadata.isEncrypted());
    System.out.println("isHidden: " + metadata.isHidden());
    System.out.println("namespaceAware: " + metadata.namespaceAware());
    System.out.println("isBinary: " + metadata.isBinary());
    System.out.println("isCached: " + metadata.isCached());
    System.out.println("sharedKeyEnc: " + metadata.sharedKeyEnc());
    System.out.println("pubKeyCS: " + metadata.pubKeyCS());
  }
}
