package org.atsign.common.examples;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.SelfKey;

public class SelfKeyDeleteExample {
    
    public static void main(String[] args) {
        // 1. establish constants
        String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
        String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory) 
        boolean VERBOSE = true; // true for more print logs 
        
        String KEY_NAME = "test"; // name of the key we will create and put
        
        // 2. create AtSign object
        AtSign atSign = new AtSign(ATSIGN_STR);
        
        // 3. atClient factory method
        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign, VERBOSE);
        } catch (AtException e) {
            System.err.println("Failed to connect to remote server " + e);
            e.printStackTrace();
        }

        // 4. create self key
        SelfKey sk = new KeyBuilders.SelfKeyBuilder(atSign).key(KEY_NAME).build();

        // 5. delete the key
        String response = null;
        try {
            response = atClient.delete(sk).get();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            System.err.println("Failed to delete key " + e);
            e.printStackTrace();
        }
        System.out.println(response);

    }

}
