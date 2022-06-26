package org.atsign.client.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.client.util.ArgsUtil;
import org.atsign.client.util.AuthUtil;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.NoSuchSecondaryException;
import org.atsign.common.Keys.AtKey;
import org.atsign.config.ConfigReader;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

/**
 * A command-line interface for scanning keys in your secondary (must have keys to atSign in keys/)
 */
public class Scan {
    public static void main(String[] args) {

        if(args.length != 3) {
            System.out.println("Incorrect usage | Scan <rootUrl> <atSign> <verbose == true|false>");
            return;
        }

        // fetch command line args
        String rootUrl = args[0]; // root.atsign.wtf:64
        String atSignConst = args[1]; // @sportsunconscious
        String verboseStr = args[2]; // true|false (true for noisy print logs)

        // ======================================================
        AtSign atSign = new AtSign(atSignConst);
        boolean verbose = Boolean.parseBoolean(verboseStr);
        String what = null; // error message string 

        // find secondary address
        Secondary.Address sAddress = null;
        try {
            Secondary.AddressFinder sAddressFinder = ArgsUtil.createAddressFinder(rootUrl);
            what = "could not find secondary with atSign:" + atSign.atSign;
            sAddress = sAddressFinder.findSecondary(atSign);
            System.out.println("Found address of atSign \"" + atSign.atSign + "\": " + sAddress.host + ":" + sAddress.port);
        } catch (IOException | NoSuchSecondaryException e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }   

        // initialize AtClient and connect to remote secondary (with pkam auth)
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
        try {
            what = "execute scan command";
            // Secondary.Response rawResponse = atClient.executeCommand("scan", true);
            // System.out.println("\n" + rawResponse);
            List<AtKey> atKeys = atClient.getAtKeys("").get();
            System.out.println("atKeys: [");
            for(AtKey atKey : atKeys) {
                System.out.println("\t" + atKey.toString());
            }
            System.out.println("]");

            for(AtKey atKey : atKeys) {
                System.out.println("======================");
                System.out.println("Full KeyName: " + atKey.toString());
                System.out.println("KeyName: " + atKey.name);
                System.out.println("Namespace: " + atKey.getNamespace());
                System.out.println("SharedBy: " + atKey.sharedBy.atSign);
                System.out.println("SharedWith: " + (atKey.sharedWith != null ? atKey.sharedWith.atSign : "null"));
                System.out.println("KeyType: " + atKey.getClass().toString());
                System.out.println("isCached: " + atKey.metadata.isCached);
                System.out.println("======================");
                System.out.println("");
            }
        } catch (Exception e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
