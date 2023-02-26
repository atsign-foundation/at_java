package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.util.ArgsUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys;

import java.util.concurrent.ExecutionException;

/**
 * A command-line interface half-example half-utility to get something that was shared by another atSign
 */
public class Get {
    public static void main(String[] args) {
        String rootUrl; // e.g. "root.atsign.org:64";
        AtSign atSign;  // e.g. "@alice";
        AtSign otherAtSign;  // e.g. "@bob";
        String keyName;

        if (args.length != 4) {
            System.err.println("Usage: Get <rootUrl> <your AtSign> <other AtSign> <name of shared key, including namespace>");
            System.exit(1);
        }

        //noinspection DuplicatedCode
        rootUrl = args[0];
        atSign = new AtSign(args[1]);
        otherAtSign = new AtSign(args[2]);
        keyName = args[3];

        AtClient atClient = null;
        try {
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl));
        } catch (AtException e) {
            System.err.println("Failed to create AtClientImpl : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        try {
            Keys.SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(otherAtSign, atSign).key(keyName).build();
            String getResponse = atClient.get(sharedKey).get();
            System.out.println("get response : " + getResponse);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to get : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
