package org.atsign.common;

import java.io.IOException;

import javax.swing.event.SwingPropertyChangeSupport;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.client.util.MetadataUtil;
import org.atsign.common.Keys.Metadata;

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

        Metadata metadata = Metadata.fromString(llookupMetaResponse.data);
        _printMetadata(metadata);
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

    private static void _printMetadata(Metadata metadata) {
        System.out.println("===============");
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
