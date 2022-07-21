package org.atsign.common.examples;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys.Metadata;
import org.atsign.common.Keys.SelfKey;

public class SelfKeyGetExample {
    
    public static void main(String[] args) {
        // 1. establish constants
        String ROOT_URL = "root.atsign.org:64";
        String ATSIGN_STR = "@33thesad";
        boolean VERBOSE = true;
        
        String KEY_NAME = "test";
        
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
        
        // 4. create selfkey
        SelfKey sk = new KeyBuilders.SelfKeyBuilder(atSign).key(KEY_NAME).build();
        
        // 5. get the key
        String response = null;
        try {
            response = atClient.get(sk).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to get key " + e);
            e.printStackTrace();
        }
        System.out.println(response);
        _printMetadata(sk.metadata);

    }

    
    private static void _printMetadata(Metadata metadata) {
        System.out.println("ttl: " + metadata.ttl);
        System.out.println("ttb: " + metadata.ttb);
        System.out.println("ttr: " + metadata.ttr);
        System.out.println("ccd: " + metadata.ccd);
        System.out.println("availableAt: " + (metadata.availableAt != null ? metadata.availableAt.toString() : "null"));
        System.out.println("expiresAt: " + (metadata.expiresAt != null ? metadata.expiresAt.toString() : "null"));
        System.out.println("refreshAt: " + (metadata.refreshAt != null ? metadata.refreshAt.toString() : "null"));
        System.out.println("createdAt: " + (metadata.createdAt != null ? metadata.createdAt.toString() : "null"));
        System.out.println("updatedAt: " + (metadata.updatedAt != null ? metadata.updatedAt.toString() : "null"));
        System.out.println("dataSignature: " + metadata.dataSignature);
        System.out.println("sharedKeyStatus: " + metadata.sharedKeyStatus);
        System.out.println("isPublic: " + metadata.isPublic);
        System.out.println("isEncrypted: " + metadata.isEncrypted);
        System.out.println("isHidden: " + metadata.isHidden);
        System.out.println("namespaceAware: " + metadata.namespaceAware);
        System.out.println("isBinary: " + metadata.isBinary);
        System.out.println("isCached: " + metadata.isCached);
        System.out.println("sharedKeyEnc: " + metadata.sharedKeyEnc);
        System.out.println("pubKeyCS: " + metadata.pubKeyCS);
    }


}
