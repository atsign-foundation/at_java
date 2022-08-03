package org.atsign.common.examples;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.SharedKey;

public class SharedKeyDeleteExample {
    
    /// Delete a SharedKey that you shared with another atSign, the key must be on your own secondary server (belonging to the sharedBy atSign)
    public static void main(String[] args) {
        // 1. establish constants
        String ROOT_URL = "root.atsign.org:64";
        String ATSIGN_STR_SHARED_BY = "@33thesad"; // my atSign (sharedBy)
        String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // other atSign (sharedWith)
        boolean VERBOSE = true;
        String KEY_NAME = "test";

        // 2. create AtSign objects
        AtSign sharedBy = new AtSign(ATSIGN_STR_SHARED_BY);
        AtSign sharedWith = new AtSign(ATSIGN_STR_SHARED_WITH);

        // 3. atClient factory method
        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(ROOT_URL, sharedBy, VERBOSE);
        } catch (AtException e) {
            System.err.println("Failed to create AtClient instance " + e);
            e.printStackTrace();
        }

        // 4. create SharedKey instance
        SharedKey sk = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key(KEY_NAME).build();

        // 5. delete the key
        String response = null;
        try {
            response = atClient.delete(sk).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e);
            e.printStackTrace();
        }
        System.out.println(response);
    }

}
