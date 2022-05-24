package org.atsign.client.util;

import org.atsign.client.api.Secondary;
import org.atsign.client.api.impl.connections.AtRootConnection;

public class ArgsUtil {
    public static Secondary.AddressFinder createAddressFinder(String rootUrl) {
        Secondary.AddressFinder addressFinder;
        if (rootUrl.startsWith("proxy:")) {
            String[] proxyParts = rootUrl.split(":");
            if (proxyParts.length != 3) {
                System.err.println("When supplying a proxy url, it needs to be in format proxy:<host>:<port>");
                System.exit(1);
            }
            String proxyHost = proxyParts[1];
            int proxyPort = Integer.parseInt(proxyParts[2]);
            addressFinder = atSign -> new Secondary.Address(proxyHost, proxyPort);
        } else {
            addressFinder = new AtRootConnection(rootUrl);
        }
        return addressFinder;
    }
}
