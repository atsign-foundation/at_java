package org.atsign.client.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.atsign.config.ConfigReader;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

/**
 * A command-line interface for scanning keys in your secondary
 */
public class Scan {
    public static void main(String[] args) {

        if(args.length != 3 && (args[2] != "false" || args[2] != "true")) {
            System.out.println("Incorrect usage | Scan <rootUrl> <atSign> <verbose == true|false>");
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
        

        // initialize AtClient and connect to remote secondary
        AtClient atClient = null;
        try {
            what = "initialize AtClient";
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl));
        } catch (AtException | IOException e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // pkam auth
        try {
            System.out.println("Starting PKAM");
            AuthUtil authUtil = new AuthUtil();
            // 1. get secondary server address 
            what = "look up at sign via new AtRootConnection(" + rootUrl + ").lookupAtSign(" + atSign.atSign + ");";
            String secondaryUrl = new AtRootConnection(rootUrl).lookupAtSign(atSign);
            System.out.println("Retrieved Secondary URL of " + atSign.atSign + ": " + secondaryUrl);
            
            // 2. initialize connection
            AtSecondaryConnection conn = new AtSecondaryConnection(new SimpleAtEventBus(), atSign, secondaryUrl, null, false, false);

            // 3. establish connection
            what = "establish connection to secondary in PKAM auth process.";
            conn.connect();
            System.out.println("Initialized connection: " + conn.getUrl());

            // 4. load .atKeys to get pkam private key
            what = "load keys from atSign: " + atSign.atSign + ".";
            Map<String, String> keys = KeysUtil.loadKeys(atSign);
            System.out.println("Successfully retrieved .atKeys.");

            // 5. use pkam private key to encrypt challenge
            what= "PKAM authenticate";
            authUtil.authenticateWithPkam(conn, atSign, keys);
            System.out.println("PKAM Auth Success");
        } catch (Exception e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // run scan command
        try {
            what = "execute scan command";
            Secondary.Response rawResponse = atClient.executeCommand("scan", true);
            System.out.println(rawResponse);
        } catch (Exception e) {
            System.err.println("Failed to " + what + " " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
