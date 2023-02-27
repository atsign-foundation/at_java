package org.atsign.client.cli;

import org.apache.commons.lang3.StringUtils;
import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.common.AtSign;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Metadata;
import org.atsign.common.AtException;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * A command-line interface for scanning keys in your secondary (must have keys to atSign in keys/)
 */
public class Scan {
    public static void main(String[] args) {

        if(args.length != 4) {
            System.out.println("Incorrect usage | Scan <rootUrl> <atSign> <verbose == true|false> <scan regex>");
            return;
        }

        // fetch command line args
        String rootUrl = args[0]; // root.atsign.wtf:64
        String atSignConst = args[1]; // @sportsunconscious
        String verboseStr = args[2]; // true|false (true for noisy print logs)
        String regex = args[3]; // scan regex (e.g. ".*")

        // ======================================================
        AtSign atSign = new AtSign(atSignConst);
        boolean verbose = Boolean.parseBoolean(verboseStr);
        String what = null; // error message string

        // find secondary address
        Secondary.Address sAddress = null;
        try {
            Secondary.AddressFinder sAddressFinder = ArgsUtil.createAddressFinder(rootUrl);
            what = "find secondary with atSign:" + atSign.atSign;
            sAddress = sAddressFinder.findSecondary(atSign);
            System.out.println("Found address of atSign \"" + atSign.atSign + "\": " + sAddress.host + ":" + sAddress.port);
        } catch (IOException | AtSecondaryNotFoundException e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }   

        // initialize AtClient and connect to remote secondary (with pkam auth therefore must have keys to secondary)
        AtClient atClient = null;
        try {
            what = "initialize AtClient";
            atClient = AtClient.withRemoteSecondary(atSign, sAddress, verbose);
        } catch (AtException e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // run scan
        List<AtKey> atKeys = null;
        try {
            what = "getAtKeys(" + regex + ")";
            atKeys = atClient.getAtKeys(regex).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // CLI
        String input;
        Scanner scanner = new Scanner(System.in);
        do {
            System.out.println();
            // _printAtKeys(atKeys);
            System.out.println("Enter index you want to llookup (l to list, q to quit):");
            input = scanner.nextLine();
            if(StringUtils.isNumeric(input)) {
                int index = Integer.parseInt(input);
                if(index < atKeys.size()) {
                    AtKey atKey = atKeys.get(index);
                    _printAtKeyInfo(atKey);
                } else {
                    System.out.println("Index out of bounds");
                }
            } else if(input.equalsIgnoreCase("l")) {
                _printAtKeys(atKeys);
            } else if(!input.equalsIgnoreCase("q")) { 
                System.out.println("Invalid input");
            }
        } while(!input.equalsIgnoreCase("q"));
        scanner.close();
    }

    private static void _printAtKeys(List<AtKey> atKeys) {
        System.out.println("atKeys: {");
        for(int i = 0; i < atKeys.size(); i++) {
            AtKey atKey = atKeys.get(i);
            System.out.println("  " + i + ":  " + (atKey.metadata.isCached ? "cached:" : "") + atKey);
        }
        System.out.println("}");
    }
    
    private static void _printAtKeyInfo(AtKey atKey) {
        System.out.println("======================");
        System.out.println("Full KeyName: " + atKey.toString());
        System.out.println("KeyName: " + atKey.name);
        System.out.println("Namespace: " + atKey.getNamespace());
        System.out.println("SharedBy: " + atKey.sharedBy.atSign);
        System.out.println("SharedWith: " + (atKey.sharedWith != null ? atKey.sharedWith.atSign : "null"));
        System.out.println("KeyType: " + atKey.getClass().toString().split("\\$")[1]);
        System.out.println("Metadata -------------------");
        _printMetadata(atKey.metadata);
        System.out.println("======================");
        System.out.println();
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
