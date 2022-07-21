package org.atsign.common.examples;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.SharedKey;

public class SharedKeyGetOtherExample {
    /// Get the SharedKey sharedBy another person and sharedWith you
    public static void main(String[] args) {
        // 1. establish constants
        String ROOT_URL = "root.atsign.org:64";
        String ATSIGN_STR_SHARED_BY = "@33thesad"; // their atSign (key is sharedBy this atSign)
        String ATSIGN_STR_SHARED_WITH = "@farinataanxious"; // your atSign (key is sharedWith you)
        boolean VERBOSE = true;
        String KEY_NAME = "test";

        // 2. create AtSign objects
        AtSign sharedBy = new AtSign(ATSIGN_STR_SHARED_BY);
        AtSign sharedWith = new AtSign(ATSIGN_STR_SHARED_WITH); // your atSign

        // 3. atClient factory method
        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(ROOT_URL, sharedWith, VERBOSE); // AtClient instance created with your atSign (sharedWith)
        } catch (AtException e) {
            System.err.println("Failed to create AtClient instance " + e);
            e.printStackTrace();
        }

        // 4. create SharedKey instance
        // key is sharedBy the other person and sharedWith you.
        SharedKey sk = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key(KEY_NAME).build();

        // 5. get the key
        String response = null;
        try {
            response = atClient.get(sk).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to get key " + e);
            e.printStackTrace();
        }
        System.out.println(response);
    }

}
