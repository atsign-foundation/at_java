package org.atsign.client.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.atsign.client.api.AtClient;
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
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.config.ConfigReader;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

/**
 * A command-line interface for scanning keys in your secondary
 */
public class Scan {
    public static void main(String[] args) {

        // constants
        String atSignConst = "@sportsunconscious";

        // ======================================================
        String rootUrl = null;
        AtSign atSign = new AtSign(atSignConst);

        ConfigReader configReader = new ConfigReader();
        try {
            configReader.loadConfig();
            rootUrl = configReader.getProperty("rootServer", "domain") + ":" + configReader.getProperty("rootServer", "port");
            System.out.println(rootUrl);
        } catch (StreamReadException | DatabindException | FileNotFoundException e) {
            System.err.println("Failed to create load config and construct rootUrl : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
        

        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl));
        } catch (AtException | IOException e) {
            System.err.println("Failed to create AtClientImpl : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            System.out.println("Starting PKAM");
            AuthUtil authUtil = new AuthUtil();
            String secondaryUrl = new AtRootConnection(rootUrl).lookupAtSign(atSign);
            System.out.println("Retrieved Secondary URL of " + atSign.atSign + ": " + secondaryUrl);
            AtSecondaryConnection conn = new AtSecondaryConnection(new SimpleAtEventBus(), atSign, secondaryUrl, null, false, false);
            conn.connect();
            System.out.println("Initialized connection: " + conn.getUrl());
            Map<String, String> keys = KeysUtil.loadKeys(atSign);
            System.out.println("Successfully retrieved .atKeys.");
            authUtil.authenticateWithPkam(conn, atSign, keys);
            System.out.println("PKAM Auth Success");
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            CompletableFuture<List<AtKey>> future = atClient.getAtKeys("");
            List<AtKey> atKeys = future.get();
            for(AtKey atKey : atKeys) {
                if(atKey instanceof PublicKey) {
                    PublicKey publicKey = (PublicKey) atKey;
                    String value = atClient.get(publicKey).get(); // .get() NOT IMPLEMENTED
                    System.out.println(value);
                }
                if(atKey instanceof SharedKey) {
                    SharedKey sharedKey = (SharedKey) atKey;
                    String value = atClient.get(sharedKey).get();
                    System.out.println(value);
                }
                if(atKey instanceof SelfKey) {
                    SelfKey selfKey = (SelfKey) atKey;
                    String value = atClient.get(selfKey).get(); // .get() NOT IMPLEMENTED
                    System.out.println(value);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
            System.exit(1);
        }


      

    }
}
