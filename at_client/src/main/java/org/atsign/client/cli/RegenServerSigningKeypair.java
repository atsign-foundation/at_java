package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import java.security.KeyPair;
import java.util.Base64;

public class RegenServerSigningKeypair {
    public static void main(String[] args) {
        AtSign atSign;  // e.g. "@alice";
        String rootUrl = "root.atsign.org:64";

        if (args.length < 1) {
            System.err.println("Usage: Share <your AtSign> [rootUrl - defaults to root.atsign.org:64]");
            System.exit(1);
        }

        atSign = new AtSign(args[0]);
        if (args.length >= 2) {
            rootUrl = args[1];
        }

        Secondary.AddressFinder addressFinder = ArgsUtil.createAddressFinder(rootUrl);

        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(atSign, addressFinder);
        } catch (AtException e) {
            System.err.println("Failed to create AtClientImpl for " + atSign + " : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            // Generate a keypair
            KeyPair keyPair = EncryptionUtil.generateRSAKeyPair();

            // Save the signing keypair to the server

            // update:public:signing_publickey@alice <public key, base64-encoded>
            System.out.printf("Updating public:signing_publickey%s\n", atSign);
            String publicKeyString = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            atClient.executeCommand(
                    String.format("update:public:signing_publickey%s %s",atSign, publicKeyString),
                    true);

            // update:@alice:signing_privatekey@alice <private key, base64-encoded>
            System.out.printf("Updating %s:signing_privatekey%s\n", atSign, atSign);
            String privateKeyString = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            atClient.executeCommand(
                    String.format("update:%s:signing_privatekey%s %s", atSign, atSign, privateKeyString),
                    true);

            System.out.println("\n" + "Success");
        } catch (Error | Exception e) {
            System.err.println("Failed to regenerate keypair : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
