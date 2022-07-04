package org.atsign.common;

import java.io.IOException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.Metadata;

public class FromStringTests {

    public static void main(String[] args) {
        String ROOT_URL = "root.atsign.wtf:64";
        String AT_SIGN_STR = "@sportsunconscious";

        Secondary.AddressFinder sAddressFinder = ArgsUtil.createAddressFinder(ROOT_URL);

        Secondary.Address sAddress = null;
        try {
            sAddress = sAddressFinder.findSecondary(new AtSign(AT_SIGN_STR));
        } catch (IOException | NoSuchSecondaryException e) {
            System.err.println(e);
            e.printStackTrace();
        }

        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(new AtSign(AT_SIGN_STR), sAddress, false);
        } catch (AtException e) {
            System.err.println(e);
            e.printStackTrace();
        }

        try {
            String KEY_NAME_1 = "public:publickey@sportsunconscious";
            _runTest(atClient, KEY_NAME_1);
        } catch (AtException e) {
            System.err.println(e);
            e.printStackTrace();
        }


    }

    private static void _runTest(AtClient atClient, String keyName) throws AtException {
        String RAW_LLOOKUP_VERB = "llookup:meta:" + keyName;
        Secondary.Response rawResponse = atClient.executeCommand(RAW_LLOOKUP_VERB, false);
        Metadata llookedUpMetadata = Metadata.fromString(rawResponse.data);

        AtKey atKey = Keys.fromString(keyName);
        atKey.metadata = Metadata.squash(atKey.metadata, llookedUpMetadata);
        _printAtKeyInfo(atKey);        
    }
    
    private static void _printAtKeyInfo(AtKey atKey) {
        System.out.println("======================");
        System.out.println("Full KeyName: " + atKey.toString() + " isPublic: " + atKey.metadata.isPublic);
        System.out.println("KeyName: " + atKey.name);
        System.out.println("Namespace: " + atKey.getNamespace());
        System.out.println("SharedBy: " + atKey.sharedBy.atSign);
        System.out.println("SharedWith: " + (atKey.sharedWith != null ? atKey.sharedWith.atSign : "null"));
        System.out.println("KeyType: " + atKey.getClass().toString().split("\\$")[1]);
        System.out.println("Metadata ----");
        _printMetadata(atKey.metadata);
        System.out.println("======================");
        System.out.println();
    }

    private static void _printMetadata(Metadata metadata) {
        System.out.println("ttl: " + metadata.ttl);
        System.out.println("ttb: " + metadata.ttb);
        System.out.println("ttr: " + metadata.ttr);
        System.out.println("ccd: " + metadata.ccd);
        System.out.println("availableAt: " + metadata.availableAt);
        System.out.println("expiresAt: " + metadata.expiresAt);
        System.out.println("refreshAt: " + metadata.refreshAt);
        System.out.println("createdAt: " + metadata.createdAt);
        System.out.println("updatedAt: " + metadata.updatedAt);
        System.out.println("dataSignature: " + metadata.dataSignature);
        System.out.println("sharedKeyStatus: " + metadata.sharedKeyStatus);
        System.out.println("isPublic: " + metadata.isPublic);
        System.out.println("isEncrypted: " + metadata.isEncrypted);
        System.out.println("isHidden: " + metadata.isHidden);
        System.out.println("namespaceAware: " + metadata.namespaceAware);
        System.out.println("isBinary: " + metadata.isBinary);
        System.out.println("isCached: " + metadata.isCached);
        System.out.println("sharedKeyEnc" + metadata.sharedKeyEnc);
        System.out.println("pubKeyCS: " + metadata.pubKeyCS);
    }
    
}