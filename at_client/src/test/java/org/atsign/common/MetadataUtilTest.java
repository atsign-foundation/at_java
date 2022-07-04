package org.atsign.common;

import java.io.IOException;

import javax.swing.event.SwingPropertyChangeSupport;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.client.util.MetadataUtil;

public class MetadataUtilTest {
    public static void main(String[] args) {
        String rootUrl = "root.atsign.wtf:64";
        String atSignStr = "@sportsunconscious";
        String fullKeyName = "@farinataanxious:lemon@sportsunconscious";

        AtSign atSign = new AtSign(atSignStr);

        Secondary.AddressFinder sAddressFinder = ArgsUtil.createAddressFinder(rootUrl);
        Secondary.Address sAddress = null;
        try {
            sAddress = sAddressFinder.findSecondary(atSign);
        } catch (NoSuchSecondaryException | IOException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            return;
        }
            

        AtClient atClient = null; 
        try {
            atClient = AtClient.withRemoteSecondary(atSign, sAddress, false);
        } catch (AtException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            return;
        }

        Secondary.Response llookupMetaResponse = null;
        try {
            String command = "llookup:meta:" + fullKeyName;
            Secondary.Response rawResponse = atClient.executeCommand(command, true);
            llookupMetaResponse = rawResponse;
        } catch (AtException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            return;
        }

        System.out.println("Raw: " + llookupMetaResponse.data + "\n");

        MetadataUtil metadataUtil = new MetadataUtil(llookupMetaResponse);
        _printMetadataUtil(metadataUtil);
    }

    private static void _printMetadataUtil(MetadataUtil metadataUtil) {
        // print out all getters in metadataUtil
        System.out.println("===============");
        System.out.println("LlookupResponse: " + metadataUtil.getRawLlookupMetaString() + " (" + metadataUtil.getRawLlookupMetaString().getClass().toString() + ")");
        System.out.println();
        System.out.println("createdBy: " + metadataUtil.getCreatedBy());
        System.out.println("updatedBy: " + metadataUtil.getUpdatedBy() );
        System.out.println("availableAt: " + metadataUtil.getAvailableAt());
        System.out.println("expiresAt: " + metadataUtil.getExpiresAt());
        System.out.println("refreshAt: " + metadataUtil.getRefreshAt());
        System.out.println("status: " + metadataUtil.getStatus());
        System.out.println("version: " + metadataUtil.getVersion());
        System.out.println("ttl: " + metadataUtil.getTTL());
        System.out.println("ttb: " + metadataUtil.getTTB());
        System.out.println("ttr: " + metadataUtil.getTTR());
        System.out.println("ccd: " + metadataUtil.isCCD());
        System.out.println("isBinary: " + metadataUtil.isBinary());
        System.out.println("isEncrypted: " + metadataUtil.isEncrypted());
        System.out.println("dataSignature: " + metadataUtil.getDataSignature());
        System.out.println("sharedKeyEnc: " + metadataUtil.getSharedKeyEnc());
        System.out.println("pubKeyCS: " + metadataUtil.getPubKeyCS());
        System.out.println();
    }
}
