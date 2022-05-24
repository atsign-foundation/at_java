package org.atsign.client.cli;

import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.common.AtSign;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.util.AuthUtil;
import org.atsign.client.util.KeysUtil;
import org.atsign.client.util.OnboardingUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Important utility which 'onboards' a new atSign.
 * Once onboarding is complete it creates the all-important keys file
 */
public class Onboard {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: Onboard <rootUrl> <atSign> <cramSecret>");
            System.exit(1);
        }

        String rootUrl = args[0]; // e.g. "root.atsign.org:64";
        AtSign atSign = new AtSign(args[1]);  // e.g. "@alice";
        String cramSecret = args[2];

        System.out.println("Looking up secondary server address for " + atSign);
        String secondaryUrl = new AtRootConnection(rootUrl).lookupAtSign(atSign);

        System.out.println("Got address: " + secondaryUrl);

        System.out.println("Connecting to " + secondaryUrl);
        AtSecondaryConnection conn = new AtSecondaryConnection(new SimpleAtEventBus(), atSign, secondaryUrl, null, false, true);
        conn.connect();

        AuthUtil auth = new AuthUtil();
        OnboardingUtil onboarding = new OnboardingUtil();

        System.out.println("Authenticating with CRAM");
        auth.authenticateWithCram(conn, atSign, cramSecret);
        System.out.println("Authenticating with CRAM succeeded");

        // We've authenticated with CRAM; let's generate and store the various keys we need
        Map<String, String> keys = new HashMap<>();
        System.out.println("Generating symmetric 'self' encryption key");
        onboarding.generateSelfEncryptionKey(keys);

        System.out.println("Generating PKAM keypair");
        onboarding.generatePkamKeypair(keys);

        System.out.println("Generating asymmetric encryption keypair");
        onboarding.generateEncryptionKeypair(keys);

        // Finally, let's store all the keys to a .keys file
        System.out.println("Saving keys to file");
        KeysUtil.saveKeys(atSign, keys);

        // we're authenticated, let's store the PKAM public key to the secondary
        System.out.println("Storing PKAM public key on cloud secondary");
        onboarding.storePkamPublicKey(conn, keys);

        // and now that the PKAM public key is on the server, let's auth via PKAM
        System.out.println("Authenticating with PKAM");
        auth.authenticateWithPkam(conn, atSign, keys);
        System.out.println("Authenticating with PKAM succeeded");

        System.out.println("Storing encryption public key");
        onboarding.storePublicEncryptionKey(conn, atSign, keys);

        // and as we've successfully authenticated with PKAM, let's delete the CRAM secret
        System.out.println("Deleting CRAM secret");
        onboarding.deleteCramKey(conn);

        System.out.println("Onboarding complete");
    }
}
