package org.atsign.common.examples;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.PublicKey;

public class PublicKeyPutExample {

    public static void main(String[] args) {
        // 1. establish constants
        String ROOT_URL = "root.atsign.org:64"; // root url of the atsign server for fetching secondary address
        String ATSIGN_STR = "@33thesad"; // atSign that we will pkam auth (must have keys in keys directory) 
        boolean VERBOSE = true; // true for more print logs 

        String KEY_NAME = "test"; // name of the key we will create and put
        String VALUE = "I love pineapple on pizza 12345"; // value we will associate with the key

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

        // 4. create a new public key
        PublicKey pk = new KeyBuilders.PublicKeyBuilder(atSign).key(KEY_NAME).build();

        // 5. put the key
        String response = null;
        try {
            response = atClient.put(pk, VALUE).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to put key " + e);
            e.printStackTrace();
        }
        System.out.println(response);
    }

}