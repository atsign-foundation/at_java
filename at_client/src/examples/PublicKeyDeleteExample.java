package org.atsign.common.examples;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.PublicKey;

public class PublicKeyDeleteExample {
    
    public static void main(String[] args) {
        // 1. establish constants
        String ROOT_URL = "root.atsign.org:64";
        String ATSIGN_STR = "@33thesad";
        boolean VERBOSE = true;

        String KEY_NAME = "test";

        // 2. create AtSign instance
        AtSign atSign = new AtSign(ATSIGN_STR);

        
        // 3. create AtClient instance using factory methods
        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign, VERBOSE);
        } catch (AtException e) {
            System.err.println(e);
            e.printStackTrace();
        }

        // 4. create public key
        PublicKey pk = new KeyBuilders.PublicKeyBuilder(atSign).key(KEY_NAME).build();

        // 5. delete the key
        String response = null;
        try {
            response = atClient.delete(pk).get();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            System.err.println(e);
            e.printStackTrace();
        }
        System.out.println(response);
    }

}
